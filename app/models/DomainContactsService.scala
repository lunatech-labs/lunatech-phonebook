package models

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.{ByteArrayContent, HttpHeaders, GenericUrl}
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.SecurityUtils
import com.google.common.io.Files
import java.io.File
import java.nio.charset.Charset
import java.util.Collections
import play.api.Play
import play.api.Play.current

import scala.xml.{XML, Elem}

/**
 * Created by yueli on 31/10/14.
 */
object DomainContactsService {

  val JSON_FACTORY = JacksonFactory.getDefaultInstance
  val SCOPE = "http://www.google.com/m8/feeds/"
  val SERVICE_ACCOUNT_EMAIL = "49412387889-okifen1e883s6jkgton4gsvbk1uceurv@developer.gserviceaccount.com"
  val DOMAINCONTACTSAPI = "https://www.google.com/m8/feeds/contacts/lunatech.com/full/?max-results=400"
  val QUERYAPI = "https://www.google.com/m8/feeds/contacts/lunatech.com/property-ldap-dn"

  val transport = GoogleNetHttpTransport.newTrustedTransport()
  val privateKey = Play.getFile("conf/pkey.p12")

  // Build service account credential.
  val credential = new GoogleCredential.Builder().setTransport(transport)
    .setJsonFactory(JSON_FACTORY)
    .setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
    .setServiceAccountScopes(Collections.singleton(SCOPE))
    .setServiceAccountPrivateKeyFromP12File(privateKey)
    .setServiceAccountUser("erik.bakker@lunatech.com")
    .build()
  val requestFactory = transport.createRequestFactory(credential)

  def getAllContacts:Elem = {
    val url = new GenericUrl(DOMAINCONTACTSAPI)
    val request = requestFactory.buildGetRequest(url)
    val response = request.execute()
    XML.loadString(response.parseAsString())
  }

  def getContact(id:String) = {
    val url = new GenericUrl(id)
    val request = requestFactory.buildGetRequest(url)
    XML.loadString(request.execute().parseAsString())
  }

  def createContact(requestBody:String) = {
    val url = new GenericUrl(DOMAINCONTACTSAPI)
    val request = requestFactory.buildPostRequest(url, ByteArrayContent.fromString("application/atom+xml", requestBody))
    request.getHeaders.setContentType("application/atom+xml")
    request.execute()
  }

  def updateContact(id:String, requestBody:String) = {
    val url = new GenericUrl(id)
    val request = requestFactory.buildPutRequest(url, ByteArrayContent.fromString("application/atom+xml", requestBody))
    request.getHeaders.setContentType("application/atom+xml").setIfMatch("*")
    request.execute()
  }

  def delContact(id:String) = {
    val url = new GenericUrl(id)
    val request = requestFactory.buildDeleteRequest(url)
    request.getHeaders.setIfMatch("*")
    request.execute().getStatusCode
  }

  def findContactByDN(dn:String) = {
    val url = new GenericUrl(QUERYAPI + "&q=" + dn)
    val request = requestFactory.buildGetRequest(url)
    request.execute()
  }

}
