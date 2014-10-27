package models

import scala.xml.Elem

/**
 * Created by yueli on 24/10/14.
 */
case class ContactEntry(id:String, name:String,
                        officePhone:String, mobilePhone:String,
                        alternatePhone:String, homePhone:String,
                        email:String, displayname:String,
                        address:String)

object ContactEntry{

  def getAllContacts(xml:Elem): List[ContactEntry] = {
    var contacts = List[ContactEntry]()
    (xml \ "entry").foreach { entryNode =>
      var id = ""
      var name = ""
      var email = ""
      (entryNode \ "link").foreach { linkNode =>
        if((linkNode \ "@rel").text == "self"){
          id = (linkNode \ "@href").text
        }
      }
      name = (entryNode \ "title").text
      (entryNode \ "email").foreach { emailNode =>
        if((emailNode \ "@rel").text == "http://schemas.google.com/g/2005#work"){
          email = (emailNode \ "@address").text
        }
      }
      contacts ::= new ContactEntry(id, name, "", "", "", "", email, "", "")
    }
    contacts
  }

  def getContact(xml:Elem): ContactEntry = {
    var id = ""
    var name = ""
    var email = ""
    xml.foreach { entryNode =>
      (entryNode \ "link").foreach { linkNode =>
        if((linkNode \ "@rel").text == "self"){
          id = (linkNode \ "@href").text
        }
      }
      name = (entryNode \ "title").text
      (entryNode \ "email").foreach { emailNode =>
        if((emailNode \ "@rel").text == "http://schemas.google.com/g/2005#work"){
          email = (emailNode \ "@address").text
        }
      }
    }
    new ContactEntry(id, name,  "", "", "", "", email, "", "")
  }

  def entryTemplate(name:String, email:String, phone_work:String, phone_home:String, phone_mobile:String): String = {
    """
      |<entry xmlns='http://www.w3.org/2005/Atom' xmlns:gd='http://schemas.google.com/g/2005'>
      |  <category scheme='http://schemas.google.com/g/2005#kind'
      |      term='http://schemas.google.com/contact/2008#contact'/>
      |  <title>"""+ name +"""</title>
      |  <gd:name>
      |    <gd:fullName>""" + name + """</gd:fullName>
      |  </gd:name>
      |  <gd:phoneNumber rel='http://schemas.google.com/g/2005#work' primary='true'>
      |    """+ phone_work +"""
      |  </gd:phoneNumber>
      |  <gd:phoneNumber rel='http://schemas.google.com/g/2005#home'>
      |    """+ phone_home +"""
      |  </gd:phoneNumber>
      |  <gd:phoneNumber rel='http://schemas.google.com/g/2005#mobile'>
      |    """+ phone_mobile +"""
      |  </gd:phoneNumber>
      |  <gd:email rel='http://schemas.google.com/g/2005#work' primary='true' address='""" + email + """'/>
      |</entry>
    """.stripMargin
  }
}