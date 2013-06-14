package services

import models._
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import play.api.Play.current

object AdminService {

  case class QuestionOptionDto(id: Option[Int],
                               description: String)

  case class QuestionDto(id: Option[Int],
                         description: String,
                         questionType: String,
                         questionOptions: List[QuestionOptionDto])

  case class EnqueteDto(title: String,
                        description: Option[String],
                        answerLink: Option[String],
                        questions: List[QuestionDto])

  object Create {
    case class QuestionOptionDto(description: String)

    case class QuestionDto(description: String,
                           questionType: String,
                           questionOptions: List[QuestionOptionDto])

    case class EnqueteDto(title: String,
                          description: Option[String],
                          questions: List[QuestionDto])

    def apply(dto: EnqueteDto): AdminKey = DB.withTransaction { implicit session =>
      val enquete = Enquetes.insert(dto.title, dto.description)

      val qIds = Questions.ins.insertAll(
        dto.questions.zipWithIndex.map { case (q, qIndex) =>
          (enquete.id, qIndex, q.description, QuestionType.byName(q.questionType))
        }: _*)

      QuestionOptions.ins.insertAll(
        dto.questions zip qIds flatMap { case (q, qId) =>
          q.questionOptions.zipWithIndex.map { case (o, oIndex) => (enquete.id, qId, oIndex, o.description) }
        }: _*)

      enquete.adminKey
    }
  }

  object Find {
    case class QuestionOptionDto(id: Int,
                                 description: String)

    case class QuestionDto(id: Int,
                           description: String,
                           questionType: String,
                           questionOptions: List[QuestionOptionDto])

    case class EnqueteDto(title: String,
                          description: Option[String],
                          answerLink: String,
                          questions: List[QuestionDto])

    def apply(adminKey: AdminKey, answerKeyToLink: (AnswerKey) => String): Option[EnqueteDto] = DB.withTransaction { implicit session =>
      Enquetes.findByAdminKey(adminKey).map { enquete =>
        EnqueteDto(
          enquete.title,
          enquete.description,
          answerKeyToLink(enquete.answerKey),
          Questions.findById(enquete.id).map { question =>
            QuestionDto(
              question.id,
              question.description,
              question.questionType.name,
              QuestionOptions.findById(enquete.id, question.id).map { questionOption =>
                QuestionOptionDto(
                  questionOption.id,
                  questionOption.description)
              })
          })
      }
    }

    def apply(answerKey: AnswerKey, answerKeyToLink: (AnswerKey) => String): Option[EnqueteDto] = DB.withTransaction { implicit session =>
      Enquetes.findByAnswerKey(answerKey).map { enquete =>
        EnqueteDto(
          enquete.title,
          enquete.description,
          answerKeyToLink(enquete.answerKey),
          Questions.findById(enquete.id).map { question =>
            QuestionDto(
              question.id,
              question.description,
              question.questionType.name,
              QuestionOptions.findById(enquete.id, question.id).map { questionOption =>
                QuestionOptionDto(
                  questionOption.id,
                  questionOption.description)
              })
          })
      }
    }
  }

  def update(adminKey: AdminKey, dto: EnqueteDto): Boolean = DB.withTransaction { implicit session =>
    Enquetes.findIdByAdminKey(adminKey) match {
      case Some(eId) =>
        Enquetes.updateQuery(eId).update((dto.title, dto.description))

        dto.questions.zipWithIndex.foreach { case (q, qIndex) =>
          q.id match {
            case None =>
              val qId = Questions.ins.insert(eId, qIndex, q.description, QuestionType.byName(q.questionType))

              QuestionOptions.ins.insertAll(
                q.questionOptions.zipWithIndex.map { case (o, oIndex) => (eId, qId, oIndex, o.description) }: _*)

            case Some(qId) =>
              Questions.updateQuery(eId, qId).update((qIndex, q.description, QuestionType.byName(q.questionType)))

              q.questionOptions.zipWithIndex.foreach { case (o, oIndex) =>
                o.id match {
                  case None =>      QuestionOptions.ins.insert(eId, qId, oIndex, o.description)
                  case Some(oId) => QuestionOptions.updateQuery(eId, qId, oId).update((oIndex, o.description))
                }
              }
          }
        }

        true

      case None => false
    }
  }

}
