package libs

import models._
import play.api.libs.json._
import play.api.libs.oauth._
import play.api.libs.ws.WS
import play.api.Logger
import play.api.Play
import play.api.mvc.RequestHeader

import scala.concurrent.{ExecutionContext, Future}

object Rdio {
  import scala.concurrent.ExecutionContext.Implicits.global
  import play.api.Play
  import play.api.Play.current

  def cfg(k: String) = Play.application.configuration.getString(k).getOrElse(throw new NoSuchElementException(k))

  val clientToken: Future[String] = WS.url("https://www.rdio.com/oauth2/token")
    .post(Map(
      "grant_type"    -> Seq("client_credentials"),
      "client_id"     -> Seq(cfg("rdio.key")),
      "client_secret" -> Seq(cfg("rdio.secret"))
      )
    ).map { token =>
      Logger.info("Rdio Client Access Token: " + token.body)
      (token.json \ "access_token").as[String]
    }

  def call(method: String, params: Map[String, Seq[String]] = Map.empty)(implicit ctx: ExecutionContext) = {
    clientToken.flatMap(token =>
      WS.url("https://www.rdio.com/api/1/")
        .post(Map("method" -> Seq(method), "access_token" -> Seq(token)) ++ params)
    )
  }

  def playbackToken(domain: String)(implicit ctx: ExecutionContext) =
    call("getPlaybackToken", Map("domain" -> Seq(domain)))
      .map(i => (i.json \ "result").asOpt[String])

  case class TrackResult(artist: String, title: String, playCount: Int, rdio: RdioInfo)

  def clean(in: String) = {
    in
      .trim
      .toLowerCase
      .split("featuring")(0)
      .split(" feat. ")(0)
      .split(" feat ")(0)
      .trim
  }

  def id(rawArtist: String, rawTitle: String)(implicit ctx: ExecutionContext): Future[Option[RdioInfo]] = { //CachedWS(artist + title) {

    val artist = clean(rawArtist)
    val title = clean(rawTitle)

    call("search",  Map(
      "types" -> Seq("Track"),
      "query" -> Seq(artist + " " + title),
      "extras" -> Seq("playCount")
    )).map { resp =>

      def isOkayAlbum(name: String) = {
        val badStr = List("original broadway cast", "tribute", "covers")
        !badStr.exists(name.contains)
      }

      val results = for {
        track <- (resp.json \ "result" \ "results").asOpt[List[JsValue]].getOrElse(Nil)
        key <- (track \ "key").asOpt[String]
        artist <- (track \ "artist").asOpt[String]
        album <- (track \ "album").asOpt[String]
          if isOkayAlbum(album)
        title <- (track \ "name").asOpt[String]
          playCount = (track \ "playCount").asOpt[Int].getOrElse(0)
          stream =(track \ "canStream").asOpt[Boolean].getOrElse(false)
      } yield {TrackResult(artist.toLowerCase.trim, title.toLowerCase.trim, playCount, RdioInfo(key, stream))}

      val bothMatch = results.filter { track =>
        track.artist == artist && track.title == title
      }

      lazy val bothStart = results.filter { track =>
          ((track.artist startsWith artist) || (artist startsWith track.artist)) &&
          ((track.title startsWith title) || (title startsWith  track.title))
      }

      val replacements = List("with ", "&", "and ", "the ", ",", "  ", "  ")
      def simplify(str: String) = replacements.fold(str){case (acc, bad) => acc.replaceAll(bad, "")}
      val threshold = 0.2

      lazy val goodEnough = results.filter { track =>
        val (cleanArtist, cleanTitle) = (simplify(artist), simplify(title))
        val (trackArtist, trackTitle) = (simplify(track.artist), simplify(track.title))
        val avgArtist = (cleanArtist.length + trackArtist.length) / 2
        val avgTitle = (cleanTitle.length + trackTitle.length) / 2
        Strings.distance(trackArtist, cleanArtist) < (avgArtist * threshold) &&
        Strings.distance(trackTitle, cleanTitle) < (avgTitle * threshold)
      }

      def byPlayCount[X <: Seq[TrackResult]](xs: X) =
        xs.sortBy(_.playCount)(implicitly[Ordering[Int]].reverse)

      val matches = if (!bothMatch.isEmpty)
        byPlayCount(bothMatch)
      else {
        if (!bothStart.isEmpty)
          byPlayCount(bothStart)
        else
          byPlayCount(goodEnough).sortBy{ track =>
            Strings.distance(track.title, title) + Strings.distance(track.artist, artist)
          }
      }

      matches
        .map(_.rdio)
        .sortBy(_.streamable)(implicitly[Ordering[Boolean]].reverse)
        .headOption
    }
  }

  def scanRankedTracks(tracks: Iterator[ChartItem])(implicit ctx: ExecutionContext) {
    scanTracks(tracks.map(_.track))
  }

  def scanTracks(tracks: Iterator[Track])(implicit ctx: ExecutionContext) {
    tracks.grouped(9).foreach { group =>
      group.foreach { track =>
        Rdio.id(track.artist, track.title)
          .map(_.foreach(id => track.rdio = Some(id)))
      }
      Thread.sleep(1010)
    }
  }
}