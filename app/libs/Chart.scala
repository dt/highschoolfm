package libs

import models.Top100

import play.api.Logger
import play.api.libs.ws.WS
import play.api.libs.concurrent.Promise
import play.api.Play.current
import org.apache.commons.lang3.StringEscapeUtils

object Chart {
  def importAll() {
    val imports = for (year <- (1960 to 2011)) yield { Chart.top100(year).map(Top100.importYear(year, _)) }
  }

  def top100(year: Int): Promise[Seq[Top100.RawEntry]] = Longboard.top100(year)

  object Longboard {
    def url(year: Int) = {
      assert(year < 2012 && year > 1945);
      "http://longboredsurfer.com/charts/" + year + ".php"
    }
    def top100(year: Int): Promise[Seq[Top100.RawEntry]] = {
      WS.url(url(year)).get().map { resp =>
        val lines = resp.body
          .split("<div class=\"entry-body\">")(1).split("</div>")(0) // slice out the list
          .split("<br />") //cut on line breaks
          .map(_.trim)

        //01. <strong>In Da Club</strong> &raquo; 50 Cent
        val Row = """(\d+). <strong>(.*)</strong> &raquo; (.*)""".r
        lines.flatMap( _ match {
          case Row(rank, title, artist) => Some(Top100.RawEntry(rank.toInt,
            StringEscapeUtils.unescapeHtml4(artist), StringEscapeUtils.unescapeHtml4(title)))
          case other => {
            Logger.warn("Could not parse: '%s' for %d" format(other, year))
            None
          }
        })
      }
    }
  }

  object Wikipedia {
    def url(year: Int) = "http://en.wikipedia.org/w/api.php?"+
                            "action=query&prop=revisions&rvprop=content&format=xml&" +
                            "&titles=Billboard_Year-End_Hot_100_singles_of_" + year
    def Row = """\n\|-\n(! scope="row" )?\| ?(\d+)[^|]*( \|\||\n\|)(.*)(\|\|)(.*)""".r
    val NamedWiiLink = """\[\[([^|]*\|)?([^]]*)\]\]""".r
    def cleanWiki(str: String) = NamedWiiLink.replaceAllIn(str, _.group(2))
    def stripQuotes(str: String) = if (str.startsWith("\"") && str.endsWith("\""))
      str.substring(1, str.length - 1) else str

    def top100(year: Int): Promise[Seq[Top100.RawEntry]] = { //CachedWS("top100-" + year){
      WS.url(url(year)).get().map { response =>
        val body = (response.xml \\ "rev").text
        var rows = Row.findAllIn(body).matchData
        rows.map(m => Top100.RawEntry(
          m.group(2).toInt,
          stripQuotes(cleanWiki(m.group(6).trim)),
          stripQuotes(cleanWiki(m.group(4).trim))
        )).toSeq
      }
    }
  }
}