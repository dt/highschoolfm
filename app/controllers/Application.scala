package controllers

import libs.Rdio
import play.api.libs.oauth._
import play.api.mvc._

object Application extends Controller {

  val RdioOAuth = OAuth(ServiceInfo(
    "http://api.rdio.com/oauth/request_token",
    "http://api.rdio.com/oauth/access_token",
    "https://www.rdio.com/oauth/authorize", Rdio.Keys),
    false)

  def sessionTokenPair(implicit request: RequestHeader): Option[RequestToken] = {
    for {
      token <- request.session.get("token")
      secret <- request.session.get("secret")
    } yield {
      RequestToken(token, secret)
    }
  }

  def authenticate = Action { request =>
    request.queryString.get("oauth_verifier").flatMap(_.headOption).map { verifier =>
      val tokenPair = sessionTokenPair(request).get
      // We got the verifier; now get the access token, store it and back to index
      RdioOAuth.retrieveAccessToken(tokenPair, verifier) match {
        case Right(t) => {
          // We received the authorized tokens in the OAuth object - store it before we proceed
          Redirect(routes.Application.index).withSession("token" -> t.token, "secret" -> t.secret)
        }
        case Left(e) => throw e
      }
    }.getOrElse(
      RdioOAuth.retrieveRequestToken("http://" + request.domain + ":9000/auth") match {
        case Right(t) => {
          // We received the unauthorized tokens in the OAuth object - store it before we proceed
          Redirect(RdioOAuth.redirectUrl(t.token)).withSession("token" -> t.token, "secret" -> t.secret)
        }
        case Left(e) => throw e
      })
  }

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
}