import com.mongodb.casbah.Imports._

package object models {
  type ObjectId = org.bson.types.ObjectId
  def returning[T](a: => T)(f: T => Unit): T = { f(a); a }
  object Query { def apply(clauses: (String, Any)*) = MongoDBObject.apply(clauses: _*) }
}
