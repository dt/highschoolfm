package models


case class Track(id: Track.Id = new Track.Id,
  artist: String,
  title: String,
  rdio: Option[RdioInfo] = None
)
case class RdioInfo(key: String, streamable: Boolean)

object Track extends MetaModel[ObjectId, Track]("track") {
  case class WithRank(year: Int, rank: Int, track: Track)

  def findOrAdd(artist: String, title: String): Track = {
    val q = Query("artist" -> artist, "title" -> title)
    db.upsert(q)
    db.findOne(q).get
  }
}
