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
    request.body.validate[EnqueteToCreate].map { enqueteToCreate =>
      DB.withTransaction { implicit session =>
        Enquetes.insert(enqueteToCreate.title, enqueteToCreate.description) match { case enquete =>
          enqueteToCreate.questions.zipWithIndex.foreach { case (questionToCreate, questionIndex) =>
            Questions.insert(
              enquete.id,
              questionIndex,
              questionToCreate.description,
              QuestionType.byName(questionToCreate.questionType)) match { case question =>
              questionToCreate.questionOptions.zipWithIndex.foreach { case (questionOptionToCreate, questionOptionIndex) =>
                QuestionOptions.insert(
                  enquete.id,
                  question.id,
                  questionOptionIndex,
                  questionOptionToCreate.description)
              }
            }
          }

          Ok(Json.obj("adminKey" -> enquete.adminKey.value))
        }
      }
    }.recoverTotal { error =>
      BadRequest(Json.obj("error" -> JsError.toFlatJson(error)))
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
        case Some(enqueteResponse) => Ok(Json.obj("enquete" -> enqueteResponse))
        case None => NotFound("not found")
      }
    }
  }

  def update(adminKey: String) = Action(parse.json) { implicit request =>
    request.body.validate[EnqueteToUpdate].map { enqueteToUpdate =>
      DB.withTransaction { implicit session =>
        Enquetes.find(AdminKey(adminKey)) match {
          case Some(enquete) =>
            Enquetes.update(enquete.id, enqueteToUpdate.title, enqueteToUpdate.description)

            enqueteToUpdate.questions.zipWithIndex.foreach { case (questionToUpdate, questionIndex) =>
              questionToUpdate.id match {
                case None =>
                  Questions.insert(
                    enquete.id,
                    questionIndex,
                    questionToUpdate.description,
                    QuestionType.byName(questionToUpdate.questionType)) match { case question =>
                    questionToUpdate.questionOptions.zipWithIndex.foreach { case (questionOptionToUpdate, questionOptionIndex) =>
                      QuestionOptions.insert(
                        enquete.id,
                        question.id,
                        questionOptionIndex,
                        questionOptionToUpdate.description)
                    }
                  }

                case Some(questionId) =>
                  Questions.update(
                    enquete.id,
                    questionId,
                    questionIndex,
                    questionToUpdate.description)
                  questionToUpdate.questionOptions.zipWithIndex.foreach { case (questionOptionToUpdate, questionOptionIndex) =>
                    questionOptionToUpdate.id match {
                      case None =>
                        QuestionOptions.insert(
                          enquete.id,
                          questionId,
                          questionOptionIndex,
                          questionOptionToUpdate.description)

                      case Some(questionOptionId) =>
                        QuestionOptions.update(
                          enquete.id,
                          questionId,
                          questionOptionId,
                          questionOptionIndex,
                          questionOptionToUpdate.description)
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
