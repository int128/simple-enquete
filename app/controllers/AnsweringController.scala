package controllers

import models._
import play.api.mvc._
import play.api.db.slick.DB
import play.api.Play.current
import java.util.UUID

object AnsweringController extends Controller {

  def show(answerKey: String) = Action { implicit request =>
    DB.withTransaction { implicit session =>
      Enquetes.find(AnswerKey(answerKey)) match {
        case Some(e) =>
          request.cookies.get("k") match {
            case Some(_) => Ok(views.html.answer(e.fetch))
            case None    => Ok(views.html.answer(e.fetch)).withCookies(Cookie("k", UUID.randomUUID().toString))
          }
        case None => NotFound("not found")
      }
    }
  }

  def answer(answerKey: String) = Action(parse.urlFormEncoded) { request =>
    request.cookies.get("k") match {
      case Some(cookieKey) => {
        val uid = UUID.fromString(cookieKey.value)
        val questionAndAnswers = request.body.map { case (qId, oIds) => (qId.toInt, oIds) }

        DB.withTransaction { implicit session =>
          Enquetes.find(AnswerKey(answerKey)) match {
            case Some(enquete) =>
              val questions = enquete.questions

              val givenQuestionIds = questionAndAnswers.keySet
              val expectedQuestionIds = questions.map(_.id).toSet

              givenQuestionIds == expectedQuestionIds match {
                case true =>
                  questions.forall {
                    case Question(_, qId, _, _, answerType) if answerType == SingleSelection =>
                      // TODO: check if can toInt
                      questionAndAnswers.get(qId).fold(false)(_.length == 1)

                  } match {
                    case true =>
                      questions.map {
                        case Question(_, qId, _, _, answerType) if answerType == SingleSelection =>
                          SingleSelectionAnswers.create(SingleSelectionAnswer(uid, qId, questionAndAnswers(qId).head.toInt))
                      }
                      Ok(enquete.id.toString)

                    case false => BadRequest("bad request")
                  }

                case false => BadRequest("bad request")
              }

            case None => NotFound("not found")
          }
        }
      }

      case None => BadRequest("bad request")
    }
  }

}
