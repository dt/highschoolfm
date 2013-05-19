package libs

import models._

import play.api.Logger
import play.api.libs.ws.WS
import play.api.Play.current
import org.apache.commons.lang3.StringEscapeUtils
import scala.concurrent.{ExecutionContext, Future}

object Chart {
  def importAll()(implicit ctx: ExecutionContext) {
    val imports = for (year <- (1960 to 2011)) yield { Chart.top100(year).map(Top100.importYear(year, _)) }
  }

  def top100(year: Int)(implicit ctx: ExecutionContext): Future[Seq[RawChartEntry]] = Longboard.top100(year)

  object Longboard {
    def url(year: Int) = {
      assert(year < 2012 && year > 1945);
      "http://longboredsurfer.com/charts/" + year + ".php"
    }
    def top100(year: Int)(implicit ctx: ExecutionContext): Future[Seq[RawChartEntry]] = {
      WS.url(url(year)).get().map { resp =>
        val lines = resp.body
          .split("<div class=\"entry-body\">")(1).split("</div>")(0) // slice out the list
          .split("<br />") //cut on line breaks
          .map(_.trim)

        //01. <strong>In Da Club</strong> &raquo; 50 Cent
        val Row = """(\d+). <strong>(.*)</strong> &raquo; (.*)""".r
        lines.flatMap( _ match {
          case Row(rank, title, artist) => Some(RawChartEntry(rank.toInt,
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

    def top100(year: Int)(implicit ctx: ExecutionContext): Future[Seq[RawChartEntry]] = { //CachedWS("top100-" + year){
      WS.url(url(year)).get().map { response =>
        val body = (response.xml \\ "rev").text
        var rows = Row.findAllIn(body).matchData
        rows.map(m => RawChartEntry(
          m.group(2).toInt,
          stripQuotes(cleanWiki(m.group(6).trim)),
          stripQuotes(cleanWiki(m.group(4).trim))
        )).toSeq
      }
    }
  }
}