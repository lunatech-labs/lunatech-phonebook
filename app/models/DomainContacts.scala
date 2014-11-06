package models

import scala.xml.{Node, XML, Elem}

/**
 * Created by yueli on 31/10/14.
 */
case class DomainContacts(id:String, fullName: String, officePhone: Option[String],
                          mobilePhone: Option[String], homePhone: Option[String],
                          email: Option[String], address: Option[String], dn: String)

object DomainContacts {

  def getXML(content:String):Elem = {
    XML.loadString(content)
  }

  def getAllContact(xml:Elem): List[DomainContacts] = {
    var contacts = List[DomainContacts]()
    (xml \ "entry").foreach { entryNode =>
      contacts ::= getContact(XML.loadString(entryNode.mkString))
    }
    contacts
  }

  def getContact(xml:Elem):DomainContacts = {
    var id = ""
    var fullname = ""
    var officePhone = ""
    var mobilePhone = ""
    var email = ""
    var address = ""
    var homePhone = ""
    var dn = ""
    xml.foreach { entryNode =>
      (entryNode \ "link").foreach { linkNode =>
        if ((linkNode \ "@rel").text == "self") {
          id = (linkNode \ "@href").text
        }
      }
      fullname = (entryNode \ "title").text
      (entryNode \ "phoneNumber").foreach { phoneNode =>
        (phoneNode \ "@rel").text match {
          case "http://schemas.google.com/g/2005#work" => officePhone = phoneNode.text.stripPrefix("")
          case "http://schemas.google.com/g/2005#home" => homePhone = phoneNode.text.stripMargin
          case "http://schemas.google.com/g/2005#mobile" => mobilePhone = phoneNode.text.stripMargin
        }
      }
      (entryNode \ "email").foreach { emailNode =>
        if ((emailNode \ "@rel").text == "http://schemas.google.com/g/2005#work") {
          email = (emailNode \ "@address").text
        }
      }
      address = (entryNode \ "postalAddress").text
      dn = (entryNode \ "extendedProperty" \ "@value").text
    }
    new DomainContacts(id, fullname, Option(officePhone), Option(mobilePhone),
      Option(homePhone), Option(email), Option(address), dn)
  }

  def getDN(xml:Elem) = {
    (xml \ "extendedProperty" \ "@value").text
  }

  def getID(xml:Elem) = {
    var id = ""
    (xml \ "link").foreach { linkNode =>
      if((linkNode \ "@rel").text == "self"){
        id = (linkNode \ "@href").text
      }
    }
    id
  }

  def contactTemplate(contact: DomainContacts) = {
    val dn = "cn="+contact.fullName+",ou=Addressbook,dc=lunatech,dc=com"
    ("""
      |<entry xmlns='http://www.w3.org/2005/Atom' xmlns:gd='http://schemas.google.com/g/2005'>
      |  <category scheme='http://schemas.google.com/g/2005#kind'
      |      term='http://schemas.google.com/contact/2008#contact'/>
      |  <title>"""+ contact.fullName +"""</title>
      |  <gd:name>
      |    <gd:fullName>"""+ contact.fullName +"""</gd:fullName>
      |  </gd:name>
      |  <gd:phoneNumber rel='http://schemas.google.com/g/2005#work' primary='true'>
      |    """+ contact.officePhone.getOrElse("none") + """
      |  </gd:phoneNumber>
      |  <gd:phoneNumber rel='http://schemas.google.com/g/2005#home'>
      |    """+ contact.homePhone.getOrElse("none") +"""
      |  </gd:phoneNumber>
      |  <gd:phoneNumber rel='http://schemas.google.com/g/2005#mobile'>
      |    """+ contact.mobilePhone.getOrElse("none") + """
      |  </gd:phoneNumber>
      |  <gd:structuredPostalAddress rel='http://schemas.google.com/g/2005#work' primary='true'>
      |    <gd:formattedAddress>"""+ contact.address.getOrElse("none") +"""</gd:formattedAddress>
      |  </gd:structuredPostalAddress>
      |  <gd:email rel='http://schemas.google.com/g/2005#work' primary='true' address='"""+ contact.email.getOrElse("") +"""'/>
      |  <gd:extendedProperty name='ldap-dn' value='""" + dn + """' />
      |</entry>
    """).stripMargin
  }

  def toPerson(contact:DomainContacts):Person = {
    val fullname = contact.fullName
    val officePhone = contact.officePhone
    val mobilePhone = contact.mobilePhone
    val email = contact.email
    val address = contact.address
    val homePhone = contact.homePhone
    val dn = contact.dn
    new Person(fullname, None, officePhone, mobilePhone, None, email, None,
      address, homePhone, None, dn)
  }



}
