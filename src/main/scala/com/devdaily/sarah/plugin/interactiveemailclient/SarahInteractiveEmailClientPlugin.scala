package com.devdaily.sarah.plugin.interactiveemailclient

import com.devdaily.sarah.plugins._
import java.util.Date
import scala.collection.mutable.ListBuffer
import net.liftweb.json._
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import javax.mail.MessagingException
import scala.collection.mutable.HashMap
import java.util.logging.FileHandler
import java.util.logging.Logger
import akka.actor.ActorSystem
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

//import akka.dispatch._
//import akka.util.duration._
//import akka.util.Timeout
//import java.util.concurrent.Executors



// TODO read configuration file on startup
// TODO handle multiple email accounts
// TODO may have problems when network connection is down, test 
// DONE don't notify about the same message more than one time

class SarahInteractiveEmailClientPlugin 
extends SarahPlugin {

  val relativePropertiesFilename = "InteractiveEmailClient.properties"
    
  // used by any Future calls
  implicit val actorSystem = ActorSystem("CurrentTimeActorSystem")
    
  // TODO move these phrases to the properties file
  val NO_NEW_EMAIL_MESSAGE = "There are no new email messages."
  val STAND_BY_MESSAGE = "Stand by, I'm checking now."

  // override this with property file data
  var phrasesICanHandle = new ListBuffer[String]()
  
  // sarah sets this directory for us
  var canonPluginDirectory = ""
    
  var emailAccount: InteractiveEmailAccount = null
  implicit val formats = DefaultFormats // Brings in default date formats etc.
  
  // don't tell the user about the email message more than once
  val messagesAlreadyToldTheUserAbout = new ListBuffer[SarahEmailMessage]
  
  def getCurrentTime = System.currentTimeMillis

  // sarah callback
  def textPhrasesICanHandle: List[String] = {
      return phrasesICanHandle.toList
  }

  // sarah callback
  override def setPluginDirectory(dir: String) {
      canonPluginDirectory = dir
      println("starting plugin")
      // load up the emailClient object
      readConfigFile(getCanonPropertiesFilename)
      // TODO in the future, allow for multiple phrases
      println("before phrasesICanHandle")
      phrasesICanHandle += emailAccount.getWhatYouSay
      println("leaving startPlugin")
  }

  // no longer called by sarah
  def startPlugin = {}

  def getCanonPropertiesFilename: String = {
      return canonPluginDirectory + PluginUtils.getFilepathSeparator + relativePropertiesFilename
  }

  // sarah callback. handle our phrase if it is given.
  // TODO i no longer use the phrases in the properties file
  def handlePhrase(phrase: String): Boolean = {
      if (phrase.trim.toLowerCase.matches("(get|check) (email|e-mail)")) {
          val future1 = Future {
              brain ! PleaseSay(STAND_BY_MESSAGE)  // don't need Await
          }
          val future2 = Future {
              val result = getListOfEmailMessages
              brain ! PleaseSay(result)
          }
          return true
      } else {
          return false
      }
  }
  
  def getListOfEmailMessages: String = {
    println("entered getListOfEmailMessages")
    var whatToSay = NO_NEW_EMAIL_MESSAGE
    try {
      whatToSay = checkEmailAccounts
      return whatToSay
    } catch {
      // TODO i can dig deeper into these exceptions to find out what really happened
      case me: MessagingException => println(me.getMessage)
               return "Got a messaging exception when trying to check email."
      case e:  Exception => println("(EmailClient) Caught a plain Exception: " + e.getMessage) 
               return "Got some type of exception when trying to check email."
      case unknown: Throwable => println("(EmailClient) Caught an unknown problem/exception.")
               return "Had a problem when checking email."
    }
  }

  def checkEmailAccounts: String = {
    println("entered checkEmailAccounts")
    // TODO i don't need to do this on every check
    val canonPropertiesFilename = canonPluginDirectory + PluginUtils.getFilepathSeparator + relativePropertiesFilename
    readConfigFile(canonPropertiesFilename)
    
    // 1) get list of messages
    val imap = new ImapReader
    println("(EmailClient) calling getRecentMessages ...")
    val messages = imap.getRecentMessages(emailAccount, emailAccount.getUsersOfInterest)
    println("(EmailClient) got messages, #messages = " + messages.length)
    if (messages.length == 0) return NO_NEW_EMAIL_MESSAGE
    
    // 2) got at least one message, continue
    val addresses = new ListBuffer[String]
    for (message <- messages) {
      if (!messagesAlreadyToldTheUserAbout.contains(message)) {
        val addy = message.getEmailAddressAsString
        addresses += addy
        messagesAlreadyToldTheUserAbout += message
      } else {
        println("already told user about current message, not going to tell them again")
        println(format("person (%s), subject (%s)", message.getEmailAddressAsString, message.subject))
      }
    }
    if (addresses.size == 0) return NO_NEW_EMAIL_MESSAGE
    
    // 3) found at least one new address, put a sentence together, and send it to sarah 
    val whatToSay = getEmailTextToSay(addresses.toList)
    
    return whatToSay

  }
  
  
  def getEmailTextToSay(addresses: List[String]): String = {
    val emailsByPerson = getEmailCountByPerson(addresses)
    if (emailsByPerson.size == 0) {
      println("emailsByPerson.size == 0")
      return "There were no new email messages."
    } else {
      println("emailsByPerson.size != 0")
      val sb = new StringBuilder
      var count = 0
      for ((key, value) <- emailsByPerson) {
        println("key = " + key)
        println("value = " + value)
        if (count == 0) { sb.append("You have "); count += 1 }
        if (value == 1)
          sb.append(format("%d new message from %s. ", value, key))
        else
          sb.append(format("%d new messages from %s. ", value, key))
      }
      return sb.toString
    }
    return ""
  }
  
  // a map of [email address] to [emails by this person]
  def getEmailCountByPerson(addresses: List[String]): HashMap[String, Integer] = {
    val emailsByPerson = new HashMap[String, Integer]
    for (address <- addresses) {
      if (emailsByPerson.keySet.contains(address)) {
        // already in map, increment count
        val currentCount = emailsByPerson.get(address).get
        emailsByPerson(address) = currentCount + 1
      } else {
        // not in map, set count to 1
        emailsByPerson(address) = 1
      }
    }
    return emailsByPerson
  }
  
  def readConfigFile(canonConfigFilename: String) {
    println("entered readConfigFile")
    println("canonConfigFilename = " + canonConfigFilename)
    println("reading file contents, getting 'text'")
    val text = PluginUtils.getFileContentsAsString(canonConfigFilename)
    println("parsing text to json")
    val json = parse(text)
    println("extracting emailAccount")
    emailAccount = json.extract[InteractiveEmailAccount]
    println("printing emailAccount")
    println(emailAccount)
    val l = emailAccount.getUsersOfInterest
    l.foreach(println)
  }
  
} // end of EmailClientPlugin


// this class is slightly different than DDEmailClient
case class InteractiveEmailAccount(
    whatYouSay: String,
    accountName: String,
    username: String,
    password: String,
    mailbox: String,
    imapServerUrl: String,
    protocol: String,
    usersOfInterest: List[String]
    )
{ 
  def getWhatYouSay = whatYouSay
  def getAccountName = accountName
  def getUsername = username
  def getPassword = password
  def getMailbox = mailbox
  def getImapServerUrl = imapServerUrl
  def getProtocol = protocol
  def getUsersOfInterest = usersOfInterest
  
  override def toString: String = {
    return format("acct (%s), user (%s), url (%s)", accountName, username, imapServerUrl)
  }
}



/**
 * Wrap a JavaMail Message. Needed because we handle messages here, but get them from
 * another function, and that function closes the mailbox when it finishes. If we 
 * try to read JavaMail messages directly from that function, the mailbox will already
 * be closed, and we'll throw a MailboxClosedException (because messages are loaded
 * lazily).
 */
case class SarahEmailMessage(from: Array[javax.mail.Address],
                             subject: String,
                             sentDate: Date,
                             receivedDate: Date,
                             content: AnyRef)
{
  
  // returns the first email address found
  def getEmailAddressAsString: String = {
    for (address <- from) {
      // assuming name address is like "Alvin Alexander <alvin...@..."
      val tmp = address.toString.split("<")
      return tmp(0).trim
    }
    return "unknown"
  }

  // TODO this can be cleaned up once the hash code algorithm is working better
  override def equals(that: Any): Boolean = {
    var result = false
    if (! that.isInstanceOf[SarahEmailMessage]) result = false
    val tmp = that.asInstanceOf[SarahEmailMessage]
    if (this.hashCode == that.hashCode) result = true
    println("this.hash = " + this.hashCode)
    println("that.hash = " + that.hashCode)
    return result
  }

  // TODO improve this
  override def hashCode = subject.hashCode + getEmailAddressAsString.hashCode
                        + sentDate.hashCode
}


                            
                            
                            












