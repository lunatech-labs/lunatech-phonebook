package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._
import play.Logger
import java.util
import util.{Date, Calendar}
import org.joda.time.DateMidnight
import java.math.BigDecimal
import play.api.Play 

import com.novell.ldap.LDAPAttribute
import com.novell.ldap.LDAPAttributeSet
import com.novell.ldap.LDAPConnection
import com.novell.ldap.LDAPEntry
import com.novell.ldap.LDAPException
import com.novell.ldap.LDAPModification
import com.novell.ldap.LDAPSearchResults


case class Person(fullname: String, lastname: Option[String], officePhone: Option[String], mobilePhone: Option[String], alternatePhone: Option[String],email: Option[String], 
	displayname: Option[String], address: Option[String], homePhone: Option[String], tags: Option[String], dn: String) {

	def md5 = {
    	import java.security.MessageDigest
    	MessageDigest.getInstance("MD5").digest(email.getOrElse("").getBytes).map("%02X".format(_)).mkString.toLowerCase
	}
}
 

object Person {

	private def filterOnPerson(filter: Option[String]): String = {
		filter match { 
			case None => "(&(objectClass=person)(cn=*))"
			case Some(q)=> "(&(objectClass=person)(|(sn=" + q + "*)))"
		}
	}

	def search(query: Option[String]) = {
		val filter = filterOnPerson(query)
		val searchBase = "dc=lunatech,dc=com"
		val searchScope = LDAPConnection.SCOPE_SUB
    
		val lc = connect()
		try {
	    	import scala.collection.JavaConversions._
	   		val searchResults = lc.search(searchBase, searchScope, filter, null, false)

	   		import scala.collection.mutable.Set
			val set = Set.empty[LDAPEntry]     
			while (searchResults.hasMore)
				set += searchResults.next()
			
	   		set.toList.map(x => toPerson(x)).sortBy(_.fullname)
			
		} finally {
	    	lc.disconnect()
		}
	}

	def getPerson(dn: String):Person = {
		val lc = connect();
		try {
			toPerson(lc.read(dn))
		} finally {
	    	lc.disconnect();
		}
    }

    def update(person: Person) {
		val modificationsArray = updateLDAPModification(person)
		val lc = connect()
		try {
		    lc.modify(person.dn, modificationsArray)
		} finally {
		    lc.disconnect()
		}
	}

	def add(person: Person) = {
		val attributes = new LDAPAttributeSet()
   		
   		attributes.add(new LDAPAttribute("objectClass", "inetOrgPerson"))
   		attributes.add(new LDAPAttribute("cn", person.fullname))
   		attributes.add(new LDAPAttribute("sn", person.fullname))
   		
   		person.displayname match { 
   			case Some(x) => attributes.add(new LDAPAttribute("displayName", x))
			case None => attributes.add(new LDAPAttribute("displayName", person.fullname))
		}
   		person.homePhone.map(x => attributes.add(new LDAPAttribute("homePhone", x)))
   		person.address.map(x => attributes.add(new LDAPAttribute("homePostalAddress", x)))
   		person.email.map(x => attributes.add(new LDAPAttribute("mail", x)))
   		person.mobilePhone.map(x => attributes.add(new LDAPAttribute("mobile", x)))
		person.alternatePhone.map(x => attributes.add(new LDAPAttribute("pager", x)))
		person.officePhone.map(x => attributes.add(new LDAPAttribute("telephoneNumber", x)))
		person.tags.map(x => attributes.add(new LDAPAttribute("employeeType", x)))

   		val dn = "cn=" + person.fullname + ",ou=Addressbook,dc=lunatech,dc=com"
   		import scala.collection.JavaConversions._
		val newEntry = new LDAPEntry(dn, attributes) 
		val lc = connect()
		try {
		    lc.add(newEntry)
		  	Person(person.fullname, person.lastname, person.officePhone, person.mobilePhone, person.alternatePhone, person.email, 
			person.displayname, person.address, person.homePhone, person.tags, dn)
		} finally {
		    lc.disconnect()
		}
	}


	def delete(dn: String) {
		val lc = connect()
		try {
		    lc.delete(dn)
		} finally {
		    lc.disconnect()
		}
	}

	
    private def updateLDAPModification(person: Person) = {
		val lc = connect()
		try {
			val entry = lc.read(person.dn)

			// What change between this person and the entry?
			// First lookup what we need to delete
			val attributesSet = entry.getAttributeSet()
			import scala.collection.mutable.Set
			val modifications = Set.empty[LDAPModification]     
		
			if (attributesSet.getAttribute("cn") != null )
	    		modifications += new LDAPModification(LDAPModification.REPLACE, new LDAPAttribute("cn", person.fullname))
			if (attributesSet.getAttribute("displayName") != null)
	    		modifications += new LDAPModification(LDAPModification.REPLACE, new LDAPAttribute("displayName", person.displayname.getOrElse("")))
			if (attributesSet.getAttribute("homePhone") != null)
	  			modifications += new LDAPModification(LDAPModification.REPLACE, new LDAPAttribute("homePhone", person.homePhone.getOrElse("")))
			if (attributesSet.getAttribute("homePostalAddress") != null)
	    		modifications += new LDAPModification(LDAPModification.REPLACE, new LDAPAttribute("homePostalAddress", person.address.getOrElse("")))
			if (attributesSet.getAttribute("mail") != null)
	    		modifications += new LDAPModification(LDAPModification.REPLACE, new LDAPAttribute("mail", person.email.getOrElse("")))
			if ( attributesSet.getAttribute("sn") != null)
	    		modifications += new LDAPModification(LDAPModification.REPLACE, new LDAPAttribute("sn", person.lastname.getOrElse("")))
			if (attributesSet.getAttribute("mobile") != null)
	    		modifications += new LDAPModification(LDAPModification.REPLACE, new LDAPAttribute("mobile", person.mobilePhone.getOrElse("")))
			if (attributesSet.getAttribute("pager") != null)
	    		modifications += new LDAPModification(LDAPModification.REPLACE, new LDAPAttribute("pager", person.alternatePhone.getOrElse("")))
			if (attributesSet.getAttribute("telephoneNumber") != null)
	    		modifications += new LDAPModification(LDAPModification.REPLACE, new LDAPAttribute("telephoneNumber", person.officePhone.getOrElse("")))
			if (attributesSet.getAttribute("employeeType") != null)
	    		modifications += new LDAPModification(LDAPModification.REPLACE, new LDAPAttribute("employeeType", person.tags.getOrElse("")))

		
			modifications.toArray
		} finally {
		    lc.disconnect();
		}
    }

	def toPerson(entry: LDAPEntry):Person = {
		
		val attributePostalAddress = Option(entry.getAttribute("homePostalAddress"))
		val attributePager = Option(entry.getAttribute("pager"))
		var attributeDisplayName = Option(entry.getAttribute("displayName"))
		val attributeDn = entry.getDN()
		val attributeMail = Option(entry.getAttribute("mail"))
		val attributeCn = entry.getAttribute("cn")
		val attributeHomePhone = Option(entry.getAttribute("homePhone"))
		val attributeSn = Option(entry.getAttribute("sn"))
		val attributeMobile = Option(entry.getAttribute("mobile"))
		val attributeTelephoneNumber = Option(entry.getAttribute("telephoneNumber"))
		val attributeEmployeeType = Option(entry.getAttribute("employeeType"))
	 
	 	Person(attributeCn.getStringValue, attributeSn.map(_.getStringValue), attributeTelephoneNumber.map(_.getStringValue), attributeMobile.map(_.getStringValue), attributePager.map(_.getStringValue), attributeMail.map(_.getStringValue), 
	 		attributeDisplayName.map(_.getStringValue), attributePostalAddress.map(_.getStringValue), attributeHomePhone.map(_.getStringValue), attributeEmployeeType.map(_.getStringValue),
	 		attributeDn)
    }

    def connect():LDAPConnection = {

    	val configuration = Play.current.configuration
		val ldapHost = configuration.getString("ldap.host")
		val ldapPort = configuration.getInt("ldap.port")
		val loginDN = configuration.getString("ldap.login")
		val password = configuration.getString("ldap.password")
		val ldapVersion = LDAPConnection.LDAP_V3

		val lc = new LDAPConnection()
		lc.connect(ldapHost.get, ldapPort.get);
		lc.bind(ldapVersion, loginDN.get, password.get.getBytes("UTF8"));

		lc
    }

}

