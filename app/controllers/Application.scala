package controllers

import services._
import services.EnqueteService._
import play.api.mvc._
import play.api.libs.json._

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

  def showEnqueteForAnswer(answerKey: String) = TODO

  def answerEnquete(answerKey: String) = TODO

}
