package models

import scala.collection.concurrent

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Writes._

case class ChartItem(year: Int, rank: Int, track: Track)
case class Track(artist: String, title: String, var rdio: Option[RdioInfo] = None)
case class RdioInfo(key: String, streamable: Boolean)

case class RawChartEntry(rank: Int, artist: String, title: String)

import collection.JavaConversions._
import java.util.concurrent.ConcurrentHashMap

object ModelJson {
  implicit val RdioInfoWrites = Json.writes[RdioInfo]
  implicit val RdioInfoReads = Json.reads[RdioInfo]

  implicit val TrackReads = Json.reads[Track]
  implicit val TrackWrites = Json.writes[Track]

  implicit val ChartItemWrites = Json.writes[ChartItem]
  implicit val ChartItemReads = Json.reads[ChartItem]
}

object Track {
  def empty[K, V]: concurrent.Map[K, V] = new ConcurrentHashMap[K, V]()
  val all: concurrent.Map[String, concurrent.Map[String, Track]] = empty

  def artistTracks(artist: String): concurrent.Map[String, Track] =
   all.getOrElseUpdate(artist.toLowerCase, empty[String, Track])

  def findOrAdd(artist: String, title: String): Track =
    artistTracks(artist).getOrElseUpdate(title.toLowerCase, Track(artist, title))

}

object Top100 {
  val charts: concurrent.Map[Int, Seq[ChartItem]] = Track.empty

  def load() = {
    import ModelJson._
    val str = scala.io.Source.fromFile("charts.json").mkString
    val parsed = Json.parse(str)
    val everything = Json.fromJson[Seq[ChartItem]](parsed).get
    for { (y, items) <- everything.groupBy(_.year) } Top100.charts.put(y, items)
  }

  load()

  def dump() = {
    //val out = new PrintWriter("charts.json")
    //val reloaded = for { t <- Top100.charts.values.flatten } yield { t.copy(track = Track.findOrAdd(t.track.artist, t.track.title)) }
    //out.print(Json.toJson(reloaded).toString)
    //out.close
  }

  def forYear(year: Int) = charts.get(year)
  def forYears(years: Seq[Int]) = years.map(charts.get)

  def importYear(year: Int, items: Traversable[RawChartEntry]) = {
    val tracks = items.map(t => ChartItem(year, t.rank, Track.findOrAdd(t.artist, t.title)))
      .toSeq.sortBy(_.rank)
    charts.put(year, tracks)
  }
}

