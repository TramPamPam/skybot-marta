package services

import javax.inject.Singleton

import model.UserMessage
import org.joda.time.{DateTimeZone, DateTime}

import scala.util.Random

/**
  * Created by ekreative on 7/9/2016.
  */
object HelloService {
  val keywords = Seq("^hi.*", "^прив(е|і)т.*", "^здоров\\s+.*", "^(good)?\\s*(morning|evening|afternoon|night).*", "^hello.*")
  val morningSmiles = Seq("(coffee)", "(yawn)", "(sun)", "(chai)")
  val afternoonSmiles = Seq("(cool)", "(penguin)", "(waiting)", "(wave)", "(computerrage)", "(pi)", "(turkey)", "(tandoori)", "(rock)")
  val eveningSmiles = Seq("(gran)", "(holidayspirit)", "(headphones)", "(rock)", "(zombie)", "(d)", "(fireworks)")
  val nightSmiles = Seq("|-)","(sadness)","(talktothehand)","(sarcastic)")

  def hasKeywords(message: String): Boolean = {
    keywords.exists(k => message.toLowerCase.matches(k))
  }

  def doAction(msg: UserMessage, sendService: SendMessageService): Unit = {
    val hour = new DateTime(DateTimeZone.forID("Europe/Kiev")).getHourOfDay
    hour match {
      case x if x > 6 && x < 12 => sendService.sendMessage(msg.from, "Good morning "+ msg.realName + " " + Random.shuffle(morningSmiles).head)
      case x if x > 12 && x < 18 => sendService.sendMessage(msg.from, "Good afternoon "+ msg.realName + " " + Random.shuffle(afternoonSmiles).head)
      case x if x > 18 && x < 24 => sendService.sendMessage(msg.from, "Good evening "+ msg.realName + " " + Random.shuffle(eveningSmiles).head)
      case x if x > 24 && x < 6 => sendService.sendMessage(msg.from, "Are you seriously? Please go to bed "+ msg.realName + " " + Random.shuffle(nightSmiles).head)
    }
  }

}
