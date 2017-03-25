package model.redmine

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.collection.mutable

/**
  * Created by cheb on 7/10/16.
  */
case class Issue(
             id: Long,
           subject: String
           ) {

}

object Issue {

  implicit val issue = {
    (__ \ "id").read[Long] and
      (__ \ "subject").read[String]
  }

  def toSkypeMessage(issue: Issue): String = {
    val buffer = mutable.Buffer[String]()
    buffer.append("Id: " + issue.id + "\n")
    buffer.append("Subject: " + issue.subject)
    buffer.mkString("\n")
  }

  }
