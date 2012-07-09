package models

case class Top100(id: Top100.Id,
  tracks: Seq[Top100Item]
)
case class Top100Item(pos: Int, track: Track.Id)

object Top100 extends MetaModel[Int, Top100]("top100") {
  case class RawEntry(pos: Int, artist: String, title: String)
  def forYear(year: Int) = db.findOneById(year)
  def forYears(years: Seq[Int]) = db.findAll(years)

  def importYear(year: Int, items: Traversable[RawEntry]) = {
    val tracks = items.map(t => Top100Item(t.pos, Track.findOrAdd(t.artist, t.title).id)).toSeq.sortBy(_.pos)
    db.save(Top100(year, tracks))
  }

  def tracks(chart: Top100): Seq[Track.WithRank] = {
    val allTracks = Track.db.findAll(chart.tracks.map(_.track)).map(i => i.id -> i).toMap
    chart.tracks.flatMap(item => allTracks.get(item.track).map(t => Track.WithRank(chart.id, item.pos, t)))
  }
}
