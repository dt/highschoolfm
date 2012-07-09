package libs

import models.{RdioInfo, Track}
import play.api.libs.concurrent.Promise
import play.api.libs.json._
import play.api.libs.oauth._
import play.api.libs.ws.WS
import play.api.Logger
import play.api.mvc.RequestHeader

object Rdio {
  val Keys = ConsumerKey("sehsdk98rjaeqqy6rjhqar6t", "VGYtUF8cSz")
  val Tokenless = RequestToken("", "")
  val Signer = OAuthCalculator(Keys, Tokenless)

  def call(method: String, params: Map[String, Seq[String]] = Map.empty) = {
    WS.url("http://api.rdio.com/1/")
      .sign(Signer)
      .post(Map("method" -> Seq(method)) ++ params)
  }

  def playbackToken(domain: String) =
    call("getPlaybackToken", Map("domain" -> Seq(domain)))
    .map(i => (i.json \ "result").asOpt[String])

  case class TrackResult(artist: String, title: String, playCount: Int, rdio: RdioInfo)

  def id(rawArtist: String, rawTitle: String): Promise[Option[RdioInfo]] = { //CachedWS(artist + title) {
    call("search",  Map(
      "types" -> Seq("Track"),
      "query" -> Seq(rawArtist + " " + rawTitle),
      "extras" -> Seq("playCount")
    )).map { resp =>
      val artist = rawArtist.toLowerCase.trim
      val title = rawTitle.toLowerCase.trim

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

      val bad = List("featuring", "feat ", "feat.", "with ", "&", "and ", "the ", ",", "  ", "  ")
      def clean(str: String) = bad.fold(str){case (acc, bad) => acc.replaceAll(bad, "")}
      val threshold = 0.2

      lazy val goodEnough = results.filter { track =>
        val (cleanArtist, cleanTitle) = (clean(artist), clean(title))
        val (trackArtist, trackTitle) = (clean(track.artist), clean(track.title))
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

  def scanRankedTracks(tracks: Iterator[Track.WithRank]) { scanTracks(tracks.map(_.track)) }

  def scanTracks(tracks: Iterator[Track]) {
    tracks.grouped(10).foreach { group =>
      group.foreach { track =>
        id(track.artist, track.title).map(_.foreach(id => Track.db.save(track.copy(rdio = Some(id)))))
      }
      Thread.sleep(1010)
    }
  }
}