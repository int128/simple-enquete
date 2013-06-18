package controllers

import models._
import play.api.mvc._
import play.api.db.slick.DB
import play.api.Play.current
import java.util.UUID

object AnsweringController extends Controller {

  def show(answerKey: String) = Action { implicit request =>
    DB.withTransaction { implicit session =>
      Enquetes.find(AnswerKey(answerKey)).map(_.fetch)
    } match {
      case Some(enquete) =>
        request.cookies.get("k") match {
          case Some(_) => Ok(views.html.answer(enquete))
          case None    => Ok(views.html.answer(enquete)).withCookies(Cookie("k", UUID.randomUUID().toString))
        }

      case None => NotFound("not found")
    }
  }

  def answer(answerKey: String) = Action(parse.urlFormEncoded) { request =>
    request.cookies.get("k") match {
      case Some(cookieKey) => {
        DB.withTransaction { implicit session =>
          Enquetes.find(AnswerKey(answerKey)) match {
            case Some(enquete) =>
              val answererId = UUID.fromString(cookieKey.value)
              val answerOf = (question: Question) => request.body.get(question.id.toString)

              for (question <- enquete.questions) {
                question.questionType match {
                  case SingleSelection =>
                    answerOf(question) match {
                      case Some(Seq(answer)) =>
                        SingleSelectionAnswers.create(SingleSelectionAnswer(answererId, question.id, answer.toInt))

                      case None => /* TODO: bad request */
                    }
                }
              }

              NotImplemented

            case None => NotFound("not found")
          }
        }
      }

      case None => BadRequest("bad request")
    }
  }

}
