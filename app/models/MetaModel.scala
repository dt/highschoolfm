package models

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoConnection
import com.mongodb.WriteConcern
import com.novus.salat.dao.{ModelCompanion, SalatDAO}
import com.novus.salat.{TypeHintFrequency, StringTypeHintStrategy, Context}
import play.api.Play
import play.api.Play.current

abstract class MetaModel[IdT : Manifest, T <: AnyRef : Manifest](val collectionName: String) {
  type Id = IdT

  object db extends ModelCompanion[T, Id] {
    implicit val ctx = MongoContext.context
    val collection = MongoConnection()("highschoolfm")(collectionName)
    override val dao =  new SalatDAO[T, Id](collection = collection){}
    def upsert(q: DBObject) = update(q, q, upsert = true, multi = false, wc = WriteConcern.SAFE)
    def upsert(q: DBObject, newObj: DBObject) = update(q, newObj, upsert = true, multi = false, wc = WriteConcern.SAFE)
    def updateMulti(q: DBObject, newObj: DBObject) = update(q, newObj, upsert = false, multi = true, wc = WriteConcern.SAFE)
    def updateOne(id: Id, newObj: DBObject) = update(Query("_id" -> id), newObj, upsert = false, multi = false, wc = WriteConcern.SAFE)
    def findAll(ids: Iterable[Id]): Iterator[T] = find(Query("_id" -> Query("$in" -> ids)))
  }

  def clearAll() = db.remove(Query())
  def refetch(id: Id): Option[T] = db.findOneById(id)
}

object MongoContext {
  implicit val context = {
    val context = new Context {
      val name = "global"
      override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.WhenNecessary, typeHint = "_t")
    }
    context.registerGlobalKeyOverride(remapThis = "id", toThisInstead = "_id")
    context.registerClassLoader(Play.classloader)
    context
  }
}

