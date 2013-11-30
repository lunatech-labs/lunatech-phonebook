package controllers

import play.api._
import play.api.mvc._
import models._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import play.api.libs.concurrent.Execution.Implicits._

import java.net.URL
import libs.openid.OpenID
import com.google.gdata.client.appsforyourdomain.UserService
import com.google.gdata.client.authn.oauth.GoogleOAuthParameters
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer
import com.google.gdata.client.authn.oauth.OAuthParameters.OAuthType
import com.google.gdata.data.appsforyourdomain.provisioning.UserEntry
import com.google.gdata.data.appsforyourdomain.provisioning.UserFeed
import org.joda.time.{DateMidnight, Interval}
import play.api.libs.json.JsPath
import play.api.data.validation.ValidationError
import play.api.http.{Writeable, ContentTypes, ContentTypeOf}

object Application extends Controller with Secured {


 val GOOGLE_OP = "https://www.google.com/accounts/o8/id"
 
  def index = IsAuthenticated { username => implicit request =>
    Ok(views.html.index(username, Person.search(None)))
  }

  val personForm = Form(
    mapping(
      "fullname" -> nonEmptyText,
      "lastname" -> optional(text),
      "officePhone" -> optional(text verifying pattern("""[0-9.+]+""".r, error="A valid phone number is required")),
      "mobilePhone" -> optional(text verifying pattern("""[0-9.+]+""".r, error="A valid phone number is required")),
      "alternatePhone" -> optional(text verifying pattern("""[0-9.+]+""".r, error="A valid phone number is required")),      
      "email" -> optional(email),
      "displayname" -> optional(text),
      "address" -> optional(text) ,
      "homePhone" -> optional(text verifying pattern("""[0-9.+]+""".r, error="A valid phone number is required")),
      "tags" -> optional(text),
      "dn" -> text
    )(Person.apply)(Person.unapply)
  )

  def edit(dn: String) =  IsAuthenticated { username => implicit request =>
    Ok(views.html.edit(username, personForm.fill(Person.getPerson(dn))))
  }

  def update(dn: String) = IsAuthenticated { username => implicit request =>
    personForm.bindFromRequest.fold(
      errors => { BadRequest(views.html.edit(username, errors)) },
      person => {
        val updatedPerson = Person.update(person)
        Redirect(routes.Application.edit(updatedPerson.dn)).flashing("success" -> "Contact has been updated")
      }
    )
  }

  def delete(dn: String) = IsAuthenticated { username => implicit request =>
     Person.delete(dn)
     Redirect(routes.Application.index()).flashing("success" -> "Contact has been deleted")
  }

  def create() = IsAuthenticated { username => implicit request =>
    Ok(views.html.add(username, personForm))
  }

  def add() = IsAuthenticated { username => implicit request =>
    personForm.bindFromRequest.fold(
      errors => {
      BadRequest(views.html.add(username, errors)) },
      person => {
        val p = Person.add(person)
        Redirect(routes.Application.edit(p.dn)).flashing("success" -> "Contact has been created")
      }
    )
  }

// -- Authentication

  /**
   * Login page.
   */
  def login = Action { implicit request =>
    Ok(views.html.login())
  }

  /**
   * Handle login form submission.
   */
  def authenticate = Action { implicit request =>
  // We are using our open id

    AsyncResult(OpenID.redirectURL(GOOGLE_OP, routes.Application.callback.absoluteURL(true), Seq("email" -> "http://schema.openid.net/contact/email", "firstname" -> "http://schema.openid.net/namePerson/first", "lastname" -> "http://schema.openid.net/namePerson/last")).map(url => Redirect(url))
      .recover { case e:Throwable => Redirect(routes.Application.login) })
  }

  def callback() = Action { implicit request =>

    Async (
      OpenID.verifiedId map ( info => {
        val originalUrl = request.session.get("originalUrl")
        session.data.empty
        val email = info.attributes("email")
        val firstname = info.attributes("firstname")
        val lastname = info.attributes("lastname")

        if (!isOnWhiteList(email))
          throw UnexpectedException(Option("Not allowed"))
        
        originalUrl match {
          case Some(url) => Redirect(url).withSession("email" -> email, "firstname" -> firstname, "lastname" -> lastname)
          case _ => Redirect(routes.Application.index).withSession("email" -> email, "firstname" -> firstname, "lastname" -> lastname)
        }        
      })
        recover { case e:Throwable => e.printStackTrace();Logger.error("error " + e.getMessage); Redirect(routes.Application.login) })
  }

 /**
   * Logout and clean the session.
   */
  def logout = Action {
    Redirect(routes.Application.login).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }

}


/**
 * Provide security features
 */
trait Secured {

  /**
   * Retrieve the connected user email.
   */
  private def username(request: RequestHeader) = request.session.get("email")

  /**
   * Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) = { 
    Results.Redirect(routes.Application.login).withSession("originalUrl" -> request.uri)
  }

  // --

  /**
   * Action for authenticated users.
   */
  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) { user =>
    Action(request => f(user)(request))
  }

  def isOnWhiteList(email:String) = {
    import play.api.Play.current
    val CONSUMER_KEY = Play.configuration.getString("google.key")
    val CONSUMER_SECRET =  Play.configuration.getString("google.secret")
    val DOMAIN =  Play.configuration.getString("google.domain")

    val oauthParameters = new GoogleOAuthParameters()
    oauthParameters.setOAuthConsumerKey(CONSUMER_KEY.get)
    oauthParameters.setOAuthConsumerSecret(CONSUMER_SECRET.get)
    oauthParameters.setOAuthType(OAuthType.TWO_LEGGED_OAUTH)
    val signer = new OAuthHmacSha1Signer()
    val feedUrl = new URL("https://apps-apis.google.com/a/feeds/" + DOMAIN.get + "/user/2.0")

    val service = new UserService("ProvisiongApiClient")
    service.setOAuthCredentials(oauthParameters, signer)
    service.useSsl()
    val resultFeed = service.getFeed(feedUrl,  classOf[UserFeed])

    import scala.collection.JavaConversions._
    val users =  resultFeed.getEntries.toSet
    val filteredUsers = users.map( entry => entry.getTitle().getPlainText() + "@" + DOMAIN.get)

    filteredUsers.contains(email)
  }

}
 
