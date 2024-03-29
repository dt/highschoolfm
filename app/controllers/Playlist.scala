package controllers

import libs.Rdio
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
import models._
import org.joda.time.DateTime
import play.api.libs.concurrent.Promise
import play.api.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._

object Playlist extends Controller {
  def age(age: Int) = born((new DateTime).getYear - age)
  def born(year: Int) = forYear(year+18)

  def shuffler(year: Int)(t: ChartItem) = {
    val yearWeight = ((4 - (year - t.year)) / 4.0) + 1
    val rankWeight = math.min((100.0 - t.rank) / 100.0, 0.35)
    scala.util.Random.nextInt(400) * yearWeight * rankWeight * -1
  }

  def forYear(year: Int) = Action { request => Async {
    val host = if (request.domain.endsWith("highschool.fm")) request.domain else "highschool.fm"
    Rdio.playbackToken(host).map{ _.map{ token =>
      val tracks = Top100.forYears(year-3 to year).flatten.flatten.sortBy(shuffler(year))
      if (tracks.isEmpty)
        NotFound("Sorry, we don't have charts for " + year)
      else
        Ok(views.html.year(year.toString, tracks, token))
    }.getOrElse(InternalServerError("No playback token from Rdio?!"))}
  }}

  def load(year: Int) = Action {
    val count = Top100.loadYear(year)
    Ok("loaded %s".format(count))
  }

  def loadAll() = Action {
    def loadYear(year: Int): String = {
      val c = Top100.loadYear(year)
      "%s:\t%s\n".format(year, c)
    }

    val chunks: Enumerator[String] = Enumerator.enumerate(1960 to 2011)
      .through(Enumeratee.map(loadYear))
      .andThen(Enumerator.eof[String])
    Ok.stream(chunks)
  }
}