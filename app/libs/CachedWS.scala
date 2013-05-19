package libs

import play.api.cache.Cache
import play.api.Play.current
import scala.concurrent.{ExecutionContext, Future}

object CachedWS {
  def apply[T](key: String)(f: => Future[Option[T]])(implicit m: scala.reflect.ClassTag[T], ctx: ExecutionContext): Future[Option[T]] =
    Cache.getAs[T](key).map(t => Future.successful(Some(t))).getOrElse{f.map(_.map{ t => Cache.set(key, t); t})}
}
