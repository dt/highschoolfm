package libs

import models.Top100

import play.api.Logger
import play.api.libs.ws.WS
import play.api.libs.concurrent.Promise
import play.api.Play.current

object Chart {
  object urls {
    def top100(year: Int) = "http://en.wikipedia.org/w/api.php?"+
                            "action=query&prop=revisions&rvprop=content&format=xml&" +
                            "&titles=Billboard_Year-End_Hot_100_singles_of_" + year
  }

  def Row = """\n\|-\n(! scope="row" )?\| ?(\d+)[^|]*( \|\||\n\|)(.*)(\|\|)(.*)""".r
  val NamedWiiLink = """\[\[([^|]*\|)?([^]]*)\]\]""".r
  def cleanWiki(str: String) = NamedWiiLink.replaceAllIn(str, _.group(2))
  def stripQuotes(str: String) = if (str.startsWith("\"") && str.endsWith("\""))
    str.substring(1, str.length - 1) else str

  def top100(year: Int): Promise[Option[Seq[Top100.RawEntry]]] = { //CachedWS("top100-" + year){
    WS.url(urls.top100(year)).get().map { response =>
      val body = (response.xml \\ "rev").text
      var rows = Row.findAllIn(body).matchData
      val res = rows.map(m => Top100.RawEntry(
          m.group(2).toInt,
          stripQuotes(cleanWiki(m.group(6).trim)),
          stripQuotes(cleanWiki(m.group(4).trim))
        )).toSeq
      if (res.size == 100)
        Some(res)
      else
        None
    }
  }
}