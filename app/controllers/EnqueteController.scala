package controllers

import play.api.mvc._
import play.api.libs.json._
import play.api.db.slick.DB
import play.api.Play.current
import models._

object EnqueteController extends Controller {

  case class QuestionOptionToCreate(description: String)
  case class QuestionToCreate(description: String,
                              questionType: String,
                              questionOptions: List[QuestionOptionToCreate])
  case class EnqueteToCreate(title: String,
                             description: Option[String],
                             questions: List[QuestionToCreate])

  implicit val fQuestionOptionToCreate = Json.format[QuestionOptionToCreate]
  implicit val fQuestionToCreate = Json.format[QuestionToCreate]
  implicit val fEnqueteToCreate = Json.format[EnqueteToCreate]

  case class QuestionOptionResponse(id: Int,
                                    description: String)
  case class QuestionResponse(id: Int,
                              description: String,
                              questionType: String,
                              questionOptions: List[QuestionOptionResponse])
  case class EnqueteResponse(title: String,
                             description: Option[String],
                             answerLink: String,
                             questions: List[QuestionResponse])

  implicit val fQuestionOptionResponse = Json.format[QuestionOptionResponse]
  implicit val fQuestionResponse = Json.format[QuestionResponse]
  implicit val fEnqueteResponse = Json.format[EnqueteResponse]

  case class QuestionOptionToUpdate(id: Option[Int],
                                    description: String)
  case class QuestionToUpdate(id: Option[Int],
                              description: String,
                              questionType: String,
                              questionOptions: List[QuestionOptionToUpdate])
  case class EnqueteToUpdate(title: String,
                             description: Option[String],
                             questions: List[QuestionToUpdate])

  implicit val fQuestionOptionToUpdate = Json.format[QuestionOptionToUpdate]
  implicit val fQuestionToUpdate = Json.format[QuestionToUpdate]
  implicit val fEnqueteToUpdate = Json.format[EnqueteToUpdate]

  def create = Action(parse.json) { request =>
    request.body.validate[EnqueteToCreate].map { dto =>
      DB.withTransaction { implicit session =>
        val enquete = Enquetes.insert(dto.title, dto.description)
        dto.questions.zipWithIndex.foreach { case (q, qIndex) =>
          val question = Questions.insert(enquete.id, qIndex, q.description, QuestionType.byName(q.questionType))
          q.questionOptions.zipWithIndex.foreach { case (o, oIndex) =>
            QuestionOptions.insert(enquete.id, question.id, oIndex, o.description)
          }
        }
        enquete
      } match {
        case e => Ok(Json.obj("adminKey" -> e.adminKey.value))
      }
    }.recoverTotal { e =>
      BadRequest(Json.obj("error" -> JsError.toFlatJson(e)))
    }
  }

  def get(adminKey: String) = Action { implicit request =>
    DB.withTransaction { implicit session =>
      Enquetes.find(AdminKey(adminKey)).map { enquete =>
        EnqueteResponse(
          enquete.title,
          enquete.description,
          routes.AnsweringController.show(enquete.answerKey.value).absoluteURL(),
          enquete.questions.map { question =>
            QuestionResponse(
              question.id,
              question.description,
              question.questionType.name,
              question.questionOptions.map { questionOption =>
                QuestionOptionResponse(
                  questionOption.id,
                  questionOption.description)
              })
          })
      } match {
        case Some(e) => Ok(Json.obj("enquete" -> e))
        case None => NotFound("not found")
      }
    }
  }

  def update(adminKey: String) = Action(parse.json) { implicit request =>
    request.body.validate[EnqueteToUpdate].map { dto =>
      DB.withTransaction { implicit session =>
        Enquetes.find(AdminKey(adminKey)) match {
          case Some(enquete) =>
            Enquetes.update(enquete.id, dto.title, dto.description)

            dto.questions.zipWithIndex.foreach { case (q, qIndex) =>
              q.id match {
                case None =>
                  val question = Questions.insert(enquete.id, qIndex, q.description, QuestionType.byName(q.questionType))
                  q.questionOptions.zipWithIndex.foreach { case (o, oIndex) =>
                    QuestionOptions.insert(enquete.id, question.id, oIndex, o.description)
                  }

                case Some(qId) =>
                  Questions.update(enquete.id, qId, qIndex, q.description)

                  q.questionOptions.zipWithIndex.foreach { case (o, oIndex) =>
                    o.id match {
                      case None =>      QuestionOptions.insert(enquete.id, qId, oIndex, o.description)
                      case Some(oId) => QuestionOptions.update(enquete.id, qId, oId, oIndex, o.description)
                    }
                  }
              }
            }

            NotImplemented("todo")

          case None => NotFound("not found")
        }
      }
    }.recoverTotal { e =>
      BadRequest(Json.obj("error" -> JsError.toFlatJson(e)))
    }
  }

  def admin = Action { Ok(views.html.admin()) }

}
