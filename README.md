Lunatech Phonebook	
=====================================

This application allows to edit an LDAP directory. Authentication is done using Google app accounts.

Getting started
===============

This project uses playframework 2.2. Clone the project. Edit the file `conf/application.conf` and edit the following properties:

LDAS configuration
--

```
ldap.container="your ldap domain (ie dc=lunatech,dc=com)"
ldap.host="your ldap server"
ldap.port=389
ldap.login=""
ldap.password=""
```

Google integration
--

```
google.domain="your google app domain"
google.key="your google app key"
google.secret="your google app secret key"
```
