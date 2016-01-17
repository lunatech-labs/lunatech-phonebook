package controllers

import java.io.OutputStream

import com.google.api.client.util.ByteStreams
import play.api._
import play.api.mvc._
import models._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import java.math.BigInteger
import java.security.SecureRandom

import play.api.libs.concurrent.Execution.Implicits._

import java.net.URL
import libs.openid.OpenID
import com.google.api.client.auth.oauth2.TokenResponseException
import com.google.api.client.googleapis.auth.oauth2.{GoogleCredential, GoogleAuthorizationCodeTokenRequest, GoogleTokenResponse}
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.oauth2.Oauth2
import com.google.api.services.oauth2.model.Tokeninfo
import com.google.gdata.client.appsforyourdomain.UserService
import com.google.gdata.client.authn.oauth.GoogleOAuthParameters
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer
import com.google.gdata.client.authn.oauth.OAuthParameters.OAuthType
import com.google.gdata.data.appsforyourdomain.provisioning.UserEntry
import com.google.gdata.data.appsforyourdomain.provisioning.UserFeed

import com.lunatech.openconnect.Authenticate

import play.api.Play.current

import scala.io.Source


object Application extends Controller with Secured {

  def index = IsAuthenticated { username => implicit request =>
    val content = DomainContactsService.getAllContacts
    val contacts = DomainContacts.getAllContact(content).toSeq.sortBy(_.fullName)
    Ok(views.html.index(username, Person.search(None), contacts))
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

        // update domain contact
        Logger.info(updatedPerson.tags.getOrElse(""))
        val contact = Person.toContact(updatedPerson)
        val requestBody = DomainContacts.contactTemplate(contact)
        val updateResponse = DomainContactsService.updateContact(updatedPerson.tags.get, requestBody)
        if(updateResponse.getStatusCode == 200){
          Logger.info(updateResponse.getStatusCode.toString)
        }
        Redirect(routes.Application.edit(updatedPerson.dn)).flashing("success" -> "Contact has been updated")
      }
    )
  }

  def delete(dn: String) = IsAuthenticated { username => implicit request =>
    val person = Person.getPerson(dn)
    DomainContactsService.delContact(person.tags.get)
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

        //add domain contact
        val contact = Person.toContact(person)
        Logger.info(DomainContacts.contactTemplate(contact))
        val response = DomainContactsService.createContact(DomainContacts.contactTemplate(contact))
        if(response.getStatusCode == 201) {
          val content = DomainContacts.getXML(Source.fromInputStream(response.getContent).mkString)
          println(content.toString())
          val id = DomainContacts.getID(content)
          val p = Person.add(Person.addcontactsid(id, person))
          Logger.info(p.tags.getOrElse(""))
          Redirect(routes.Application.edit(p.dn)).flashing("success" -> "Contact has been created")
        }else{
          BadRequest
        }
      }
    )
  }

  // Domain shared contacts start

  val contactForm = Form(
    mapping(
      "id" -> text,
      "fullname" -> nonEmptyText,
      "officePhone" -> optional(text verifying pattern("""[0-9.+]+""".r, error="A valid phone number is required")),
      "mobilePhone" -> optional(text verifying pattern("""[0-9.+]+""".r, error="A valid phone number is required")),
      "homePhone" -> optional(text verifying pattern("""[0-9.+]+""".r, error="A valid phone number is required")),
      "email" -> optional(email),
      "address" -> optional(text) ,
      "dn" -> text
    )(DomainContacts.apply)(DomainContacts.unapply)
  )

  def importToSharedContacts = IsAuthenticated { username => implicit request =>
    Person.search(None).foreach { person =>
      val contact = Person.toContact(person)
      val response = DomainContactsService.createContact(DomainContacts.contactTemplate(contact))
      if(response.getStatusCode == 201){
        val content = DomainContacts.getXML(Source.fromInputStream(response.getContent).mkString)
        val id = DomainContacts.getID(content)
        val p = Person.addcontactsid(id, person)
        val updatedperson = Person.update(p)
        Logger.info(updatedperson.tags.get)
      }
      println("==================")
      Thread.sleep(1000)
    }
    Ok
  }

  def deleteall = IsAuthenticated { username => implicit request =>
    val content = DomainContactsService.getAllContacts
    val contacts = DomainContacts.getAllContact(content).toSeq.sortBy(_.fullName)
    contacts.foreach { contact =>
      val status = DomainContactsService.delContact(contact.id)
      Logger.info(status.toString)
      Thread.sleep(300)
    }
    Ok
  }

  def getContactForm = IsAuthenticated { username => implicit request =>
    Ok(views.html.contactForm(username, contactForm, routes.Application.createContact().url))
  }

  def getContact(id:String) = IsAuthenticated { username => implicit request =>
    val content = DomainContactsService.getContact(id)
    val contact = DomainContacts.getContact(content)
    Logger.info(contact.fullName)
    println(contact.id)
    println(contact.email)
    println(contact.officePhone)
    println(contact.address)
    Ok(views.html.contactForm(username, contactForm.fill(contact), routes.Application.updateContact(id).url))
  }

  // not use
  def createContact = IsAuthenticated { username => implicit request =>
    contactForm.bindFromRequest().fold(
      errors => {
        BadRequest(views.html.contactForm(username, errors , routes.Application.createContact().url))
      },
      contact => {
        val response = DomainContactsService.createContact(DomainContacts.contactTemplate(contact))
        if(response.getStatusCode == 201){
          Redirect(routes.Application.index()).flashing("success" -> "New google contacts created")
        }else{
          Redirect(routes.Application.index()).flashing("success" -> "Failed to create new contact in lunatech.com")
        }
      }
    )
  }


  def updateContact(id:String) = IsAuthenticated { username => implicit request =>
    contactForm.bindFromRequest().fold(
      errors => {
        BadRequest(views.html.contactForm(username, errors , routes.Application.updateContact(id).url))
      },
      contact => {
        val person = DomainContacts.toPerson(contact)
        Person.update(Person.addcontactsid(id, person))
        val requestBody = DomainContacts.contactTemplate(contact)
        val response = DomainContactsService.updateContact(id, requestBody)
        if(response.getStatusCode == 200){
          println(Source.fromInputStream(response.getContent).getLines())
          Redirect(routes.Application.index()).flashing("success" -> "updated contact")
        }else{
          BadRequest
        }
      }
    )
  }

  def delContact(id:String) = IsAuthenticated { username => implicit request =>
    val url = id
    val content = DomainContactsService.getContact(id)
    val dn = DomainContacts.getDN(content)
    Person.delete(dn)
    val status = DomainContactsService.delContact(id)
    if(status == 200){
      Redirect(routes.Application.index()).flashing("success" -> "Contact has been deleted")
    }else{
      Redirect(routes.Application.updateContact(url)).flashing("sucess" -> "An error accured")
    }
  }

  def findContact = Action { implicit request =>
    val response = DomainContactsService.findContactByDN("cn=test yue,ou=Addressbook,dc=lunatech,dc=com")
    println(Source.fromInputStream(response.getContent).mkString)
    Ok

  }

  // Domain shared contacts end

// -- Authentication


  /**
   * Login page.
   */
  def login = Action { implicit request =>
    if(Play.isProd) {
      val clientId: String = Play.configuration.getString("google.clientId").get
      val state: String = new BigInteger(130, new SecureRandom()).toString(32)

      Ok(views.html.login(clientId)).withSession("state" -> state)
    } else {
      Redirect(routes.Application.index).withSession("email" -> "developer@lunatech.com")
    }
  }

  def authenticate(code: String, id_token: String, access_token: String) = Action { implicit request =>

    Async {
      val response = Authenticate.authenticateToken(code, id_token, access_token)

      response.map {
          case Left(parameters) => Redirect(routes.Application.index).withSession(parameters.toArray: _*)
          case Right(message) => Redirect(routes.Application.login).withNewSession.flashing("error" -> message.toString())
        }
      }
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
   def IsAuthenticated(f: => String => Request[AnyContent] => Result) =
     Security.Authenticated(username, onUnauthorized) { user =>
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
