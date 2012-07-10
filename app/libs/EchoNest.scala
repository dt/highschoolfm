package libs

import play.api.mvc.RequestHeader
import play.api.libs.ws.WS
import play.api.libs.concurrent.Promise
import play.api.libs.oauth._
import play.api.libs.json._

object EchoNest {
  val Key = "WUTDYFSFNUOAICBFJ"

  def lookup(artist: String, title: String) = { //CachedWS(artist + title) {
    WS.url("http://developer.echonest.com/api/v4/song/search").withQueryString( "api_key" -> Key,
      "format" -> "json",
      "bucket" -> "id:rdio-us-streaming",
      "artist" -> artist,
      "title" -> title
    ).get().map { resp =>
      resp.json \ "response" \ "songs"
    }
  }
}