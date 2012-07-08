package libs

import play.api.cache.Cache
import play.api.libs.concurrent.Promise
import play.api.Play.current

object CachedWS {
  def apply[T](key: String)(f: => Promise[Option[T]])(implicit m: ClassManifest[T]): Promise[Option[T]] =
    Cache.getAs[T](key).map(t => Promise.pure(Some(t))).getOrElse{f.map(_.map{ t => Cache.set(key, t); t})}
}
