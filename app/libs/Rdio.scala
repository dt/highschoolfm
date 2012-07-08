package libs

import models.RdioTrack
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

  def id(artist: String, title: String): Promise[Option[RdioTrack]] = { //CachedWS(artist + title) {
    call("search",  Map(
      "types" -> Seq("Track"),
      "query" -> Seq(artist + " " + title),
      "extras" -> Seq("playCount")
    )).map { resp =>
      val results = (resp.json \ "result" \ "results").asOpt[List[JsValue]].getOrElse(Nil)
      val bothMatch = results.filter { track =>
        (track \ "artist").asOpt[String].exists(_.toLowerCase.trim == artist.toLowerCase.trim) &&
        (track \ "name"  ).asOpt[String].exists(_.toLowerCase.trim == title.toLowerCase.trim)
      }
      lazy val bothStart = results.filter { track =>
        (track \ "artist").asOpt[String].exists(_.toLowerCase.trim startsWith artist.toLowerCase.trim) &&
        (track \ "name"  ).asOpt[String].exists(_.toLowerCase.trim startsWith title.toLowerCase.trim)
      }

      val matches = if (bothMatch.isEmpty) bothStart else bothMatch

      matches.sortBy{ track =>
        Logger.info((track \ "artist").as[String] + (track \ "name").as[String] + (track \ "playCount").asOpt[Int])
        (track \ "playCount").asOpt[Int].getOrElse(0)
      }(implicitly[Ordering[Int]].reverse).flatMap { track =>
        (track \ "key").asOpt[String].map(RdioTrack(_, (track \ "canStream").asOpt[Boolean].getOrElse(false)))
      }.sortBy(_.streamable)(implicitly[Ordering[Boolean]].reverse).headOption
    }
  }
}