package services

import java.io.{BufferedReader, InputStreamReader}

import model.User
import model.redmine.Issue
import play.api.libs.json.Json

import scala.concurrent.Await
import play.api.libs.ws.WSClient

import scala.concurrent.{Await, ExecutionContext}
import javax.inject.{Inject, Singleton}

import play.api.libs.json._
import com.codahale.jerkson.{ParsingException, Json => json}
import model.{AuthToken, User}
import play.api.cache
import play.api.cache.CacheApi
import java.net._

import scala.concurrent.duration._
import scala.io.Source

/**
  * Created by cheb on 7/10/16.
  */
class RedmineService @Inject()(implicit exec: ExecutionContext, ws: WSClient, cache: CacheApi) {

  val  keywords = Seq("^\\s*track")
  var apiKey = "1c3aae92ecac610d6b8ca0e0aba05e8b0ed02c05"
  var baseUrl = "https://redmine.ekreative.com/"

  def hasKeywords(message: String): Boolean = {
    keywords.exists(r => message.toLowerCase.matches(r))
  }

  def sayHello(userID: String, sendService: SendMessageService): Unit = {
    sendService.sendMessage(userID, "Hello from tracker")
  }

  def findMine(userID: String, sendService: SendMessageService): Unit = {

    val request = ws.url(baseUrl+"issues.json?assigned_to_id=me&key="+apiKey).get.map {
      response =>
        var issues = Seq[Issue]()
//        if (response.status == 200) {
//          issues = json.parse[Seq[Issue]](Json.parse(response.body).\("issues").get.toString())
//        } else {
//          sendService.sendMessage(userID, "Failed get data from Redmine server")
//        }
        issues
    }
    Await.result(request, 30 seconds)
  }

  def doCheck(userID: String, sendService: SendMessageService): Unit = {

    println("doRedmineCheck")
    val users: Seq[User] = cache.getOrElse[Seq[User]]("users.filter", 12.hours) {
      val request = ws.url("http://10.0.1.249/api/workers").get.map {
        response =>
          println(response.status)
          var users = Seq[User]()
          if (response.status == 200) {
            users = json.parse[Seq[User]](Json.parse(response.body).\("workers").get.toString())
          } else {
            sendService.sendMessage(userID, "Failed get data from Viktor server")
          }
          users
      }
      Await.result(request, 30 seconds)
    }
    //doCompare(users, userID, sendService)
    users
  }

  def doCompare(usersSkype: Seq[User], userID: String, sendService: SendMessageService): Unit = {
    println("doCompare")
    usersSkype.foreach(u => {

      if (u.redmineId != null) {
        println("if")
        //val req = ws.url("https://confluence.atlassian.com/kb/unable-to-connect-to-ssl-services-due-to-pkix-path-building-failed-779355358.html")
        /*val req = ws.url("https://redmine.ekreative.com/time_entries.json?limit=10&user_id=" + u.redmineId)
          .withHeaders("X-Redmine-API-Key" -> "bb2755ae5510ce161104d170de94356b9f1aa016")
          .get.map {
          resources =>
            println(resources.status)
            println(resources.body)
        }
        Await.result(req, 30 seconds)
        */
        var conn: HttpURLConnection =
          new URL( "http://redmine.ekreative.com/time_entries.json?limit=10&user_id=" + 116)
            .openConnection().asInstanceOf[HttpURLConnection]

        conn.setRequestProperty("X-Redmine-API-Key", "bb2755ae5510ce161104d170de94356b9f1aa016");
        // I am a browser. It's true. I am. Yep. Promise. I is. Cross my heart.
        conn.setRequestProperty(
          "User-Agent",
          "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2227.1 Safari/537.36"
        )

        // lets give it some time, no rush!
        conn.setConnectTimeout(20000)

        // make the connection
        conn.connect()
        var s = Source.fromInputStream(conn.getInputStream).mkString

        println(s)
      }
      println("loop")
    }
    )
  }

  //printUsers(msg, users, userID, sendService)

}
