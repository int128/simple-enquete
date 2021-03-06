package controllers

import services._
import services.EnqueteService._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object Application extends Controller {

  implicit val optionReads = Json.format[QuestionOptionDto]
  implicit val questionReads = Json.format[QuestionDto]
  implicit val enqueteReads = Json.format[EnqueteDto]

  def index = Action { Ok(views.html.index()) }

  def admin = Action { Ok(views.html.admin()) }

  def createEnquete = Action(parse.json) { request =>
    request.body.validate[EnqueteDto].map { dto =>
      Ok(Json.obj("adminKey" -> EnqueteService.create(dto)))
    }.recoverTotal { e =>
      BadRequest(Json.obj("error" -> JsError.toFlatJson(e)))
    }
  }

  def queryEnquete(adminKey: String) = Action {
    EnqueteService.findByAdminKey(adminKey) match {
      case Some(e) => Ok(Json.obj("enquete" -> e))
      case None => NotFound("not found")
    }
  }

  def updateEnquete(adminKey: String) = Action(parse.json) { request =>
    request.body.validate[EnqueteDto].map { dto =>
      EnqueteService.update(adminKey, dto) match {
        case true =>
          EnqueteService.findByAdminKey(adminKey) match {
            case Some(e) => Ok(Json.obj("enquete" -> e))
            case None => NotFound("not found")
          }
        case false => NotFound("not found")
      }
    }.recoverTotal { e =>
      BadRequest(Json.obj("error" -> JsError.toFlatJson(e)))
    }
  }

  def showEnqueteForAnswer(answerKey: String) = Action { request =>
    EnqueteService.findByAnswerKey(answerKey) match {
      case Some(e) =>
        request.cookies.get("k") match {
          case Some(_) => Ok(views.html.answer(e, answerKey))
          case None    => Ok(views.html.answer(e, answerKey)).withCookies(Cookie("k", UUID.randomUUID().toString))
        }
      case None => NotFound("not found")
    }
  }

  def answerEnquete(answerKey: String) = Action(parse.urlFormEncoded) { request =>
    request.cookies.get("k") match {
      case Some(cookieKey) => {
        val uid = UUID.fromString(cookieKey.value)
        val answers = request.body.map { case (qId, oIds) => (qId.toInt, oIds) }
        AnsweringService.answer(answerKey, uid, answers) match {
          case Left(Some(eId)) => Ok(eId.toString)
          case Left(None) => NotFound("not found")
          case Right(_) => BadRequest("bad request")
        }
      }
      case None => BadRequest("bad request")
    }
  }

}
