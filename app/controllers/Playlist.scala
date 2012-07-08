package controllers

import libs.Rdio
import models._
import org.joda.time.DateTime
import play.api.libs.concurrent.Promise
import play.api.Logger
import play.api.mvc._

object Playlist extends Controller {
  def age(age: Int) = born((new DateTime).getYear - age)
  def born(year: Int) = forYear(year+18)

  def shuffler(year: Int)(t: RankedTrack) = {
    val yearWeight = (4 - (year - t.year)) / 4.0
    val rank = (100.0 - t.rank) / 100.0
    scala.util.Random.nextInt(100) * yearWeight * rank * -1
  }

  def forYear(year: Int) = Action { request => Async {
    val host = if (request.domain.endsWith("highschool.fm")) request.domain else "highschool.fm"
    Rdio.playbackToken(host).map{ _.map{ token =>
      val tracks = Top100.forYears(year-3 to year).toSeq.flatMap(Top100.tracks(_)).sortBy(shuffler(year))
      if (tracks.isEmpty)
        NotFound("Sorry, we don't have charts for " + year)
      else
        Ok(views.html.year(year.toString, tracks, token))
    }.getOrElse(InternalServerError("No playback token from Rdio?!"))}
  }}
}