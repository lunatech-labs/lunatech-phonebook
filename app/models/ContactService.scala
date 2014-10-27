package models

import com.fasterxml.jackson.databind.JsonNode
import play.api.libs.ws.{Response, WS}
import play.libs.Json

import scala.concurrent.Future
import scala.io.Source

/**
 * Created by yueli on 23/10/14.
 */
object ContactService {

  //google OAuth 2.0 configuration
  val googleOAuthConf = "https://accounts.google.com/.well-known/openid-configuration"
  val authorEndpoint = "authorization_endpoint"
  val tokenEndpoint = "token_endpoint"

  //google OAuth 2.0 configuration file
  val googleOAuthConfFile = getgoogleOpenIdConf
  //google contacts scope
  val contactsScope = "https://www.google.com/m8/feeds/"

  //google contacts API
  val contactsAPI = contactsScope + "contacts/default/full/"

  //get google OAuth 2.0 configuration file from URI
  private def getgoogleOpenIdConf: JsonNode = {
    Json.parse(Source.fromURL(googleOAuthConf).mkString)
  }

  // get authorization endpoint address
  def getAuthorEndpoint: String = {
    googleOAuthConfFile.get(authorEndpoint).asText()
  }

  // get token endpoint address
  def getTokenEndpoint: String = {
    googleOAuthConfFile.get(tokenEndpoint).asText()
  }

  // return the login address
  def getLoginAddress(redirectURI:String, clientID:String): String = {
    getAuthorEndpoint + "?" + "redirect_uri=" + redirectURI +
      "&response_type=code&client_id=" + clientID +
      "&scope=" + contactsAPI + "&approval_prompt=force" + "&access_type=offline"
  }

  //get access token for communicate with Google api
  def getAccessToken(code:String, redirectURI:String, clientID:String, clientSecret:String): Future[Response] = {
    import play.api.Play.current
    WS.url(getTokenEndpoint).post(Map("code" -> Seq(code),
      "redirect_uri" -> Seq(redirectURI),
      "client_id" -> Seq(clientID),
      "client_secret" -> Seq(clientSecret),
      "grant_type" -> Seq("authorization_code")))
  }

  def getAllContacts(accessToken:String): Future[Response] = {
    import play.api.Play.current
    WS.url(contactsAPI)
      .withHeaders("GData-Version" -> "3.0")
      .withQueryString("access_token" -> accessToken).get()
  }

  def getContact(id:String, accessToken:String): Future[Response] = {
    import play.api.Play.current
    WS.url(id).withQueryString("access_token" -> accessToken).get()
  }

  def createContact(requestBody:String, access_token:String): Future[Response] = {
    import play.api.Play.current
    WS.url(contactsAPI)
      .withHeaders("GData-Version" -> "3.0")
      .withHeaders("Content-Type" -> "application/atom+xml")
      .withHeaders("Authorization" -> access_token)
      .post(requestBody)
  }

  def updateContact(id:String, access_token:String, requestBody:String): Future[Response] = {
    import play.api.Play.current
    WS.url(id)
      .withHeaders("GData-Version" -> "3.0")
      .withHeaders("If-Match" -> "*")
      .withHeaders("Content-Type" -> "application/atom+xml")
      .withHeaders("Authorization" -> access_token)
      .put(requestBody)
  }

  def delContact(id:String, access_token:String): Future[Response] = {
    import play.api.Play.current
    WS.url(id)
      .withHeaders("If-Match" -> "*")
      .withHeaders("GData-Version" -> "3.0")
      .withQueryString("access_token" -> access_token).delete()
  }
}
