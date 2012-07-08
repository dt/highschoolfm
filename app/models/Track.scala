package models

case class RdioTrack(key: String, streamable: Boolean)

case class TrackData(artist: String, title: String)

case class Track(id: Track.Id = new Track.Id,
  artist: String,
  title: String,
  rdio: Option[RdioTrack] = None
)

object Track extends MetaModel[ObjectId, Track]("track") {
  def findOrAdd(artist: String, title: String): Track = {
    val q = Query("artist" -> artist, "title" -> title)
    db.upsert(q)
    db.findOne(q).get
  }
}
