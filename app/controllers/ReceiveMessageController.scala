package controllers

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import model.UserMessage
import model.redmine.Issue
import play.api.Configuration
import play.api.mvc.{Action, Controller}
import com.codahale.jerkson.{ParsingException, Json => json}
import play.api.cache.CacheApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import services._
import model.Task
import utils.DB

import scala.concurrent.ExecutionContext

/**
  * Created by ekreative on 7/9/2016.
  */
@Singleton
class ReceiveMessageController @Inject()(actorSystem: ActorSystem, sendService: SendMessageService)(implicit exec: ExecutionContext, ws: WSClient, cache: CacheApi, db: DB,conf: Configuration) extends Controller {

  new TaskScheduleService(actorSystem, sendService, db).startPlanning

  def receive = Action(parse.json) {
    request =>
      println("request body ="+request.body)
      val messages = request.body.as[Seq[UserMessage]]
      println(messages)
      for (msg <- messages) {
        msg.content.toLowerCase match {
          //Ping:
          case x if x.matches("^ping\\s*") => sendService.sendMessage(msg.from, "pong")

          //Admin tasks:
          case x if "admin-start-tasks".equals(x) && msg.from.equals("8:antonekreative") => new TaskScheduleService(actorSystem, sendService, db).startPlanning
          case x if x.matches("^start task \\d+") && msg.from.equals("8:antonekreative") => new TaskScheduleService(actorSystem, sendService, db).launchTask(Task.tasks(x.toLowerCase.replaceAll("[^0-9]+", "").toInt))

          //What marta can subcsribe to
          case x if x.toLowerCase.matches("^tasks.*") => sendService.sendMessage(msg.from, "Here is s list of all tasks:\n" + Task.tasks.map(t => "%d) %s".format(t.id, t.title)).mkString("\n"))

          //Users subscribes
          case x if x.toLowerCase.matches("^my\\s+tasks\\s*") => {
            val myTasksIds = db.getTasksByUser(msg.from)
            val myTasks = Task.tasks.filter(t => myTasksIds.contains(t.id))
            myTasks.nonEmpty match {
              case true => sendService.sendMessage(msg.from, "Here is s list of all your tasks:\n" + myTasks.map(t => "%d) %s".format(t.id, t.title)).mkString("\n"))
              case false => sendService.sendMessage(msg.from, "I'm sorry but seems you don't have any tasks (shake)")
            }
          }

          //What Marta can do
          case x if HelpService.hasKeywords(x) => HelpService.showHelp(msg, sendService)

          //Subcsribe/unsubscribe
          case x if SubscribeService.hasKeywords(x) => SubscribeService.doAction(msg, sendService, db)

          //Hello
          case x if HelloService.hasKeywords(x) => HelloService.doAction(msg, sendService)

          //Open door
          case x if new DoorOpenerService().hasKeywords(x) =>
            new DoorOpenerService().openDoor(msg.from, sendService)

          //Show all
          case x if new ListService().hasKeywords(x) =>
            new ListService().showList(x, msg.from, sendService)

          ///Show concrete
          case x if new WhoisService().hasKeywords(x) =>
            new WhoisService().trySearch(x, msg.from, sendService)

          case x if new RedmineService().hasKeywords(x) => {
//            new RedmineService().findMine(msg.from, sendService)
            val request = ws.url( new RedmineService().baseUrl+"issues.json?assigned_to_id=me&key="+ new RedmineService().apiKey).get.map {
              response =>
//                var issues = Seq[Issue]()
//                if (response.status == 200) {
//                  issues = json.parse[Seq[Issue]](Json.parse(response.body).\("issues").get.toString())
//                } else {
////                  sendService.sendMessage(userID, "Failed get data from Redmine server")
//                }
                Ok(Json.parse(response.body).\("issues").get.toString())
            }

          }

          //case "redmine" => new RedmineService().doCheck(msg.from, sendService)
          case _ => sendService.sendMessage(msg.from, "Sorry %s, but I don't understand what you want. I'm not smart enough".format(msg.realName))
                    sendService.sendMessage(msg.from, "(sadness)")
        }
      }
      Ok("OK")
  }
}
