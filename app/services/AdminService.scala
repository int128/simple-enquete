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
                         answerType: String,
                         questionOptions: List[QuestionOptionDto])

  case class EnqueteDto(title: String,
                        description: Option[String],
                        answerLink: String,
                        questions: List[QuestionDto])

  def create(dto: EnqueteDto): AdminKey = DB.withTransaction { implicit session =>
    val adminKey = AdminKey.random()

    val eId = Enquetes.ins.insert(dto.title, dto.description, AnswerKey.random(), adminKey)

    val qIds = Questions.ins.insertAll(
      dto.questions.zipWithIndex.map { case (q, qIndex) => (eId, qIndex, q.description, q.answerType) }: _*)

    QuestionOptions.ins.insertAll(
      dto.questions zip qIds flatMap { case (q, qId) =>
        q.questionOptions.zipWithIndex.map { case (o, oIndex) => (eId, qId, oIndex, o.description) }
      }: _*)

    adminKey
  }

  def findByAdminKey(adminKey: AdminKey,
                     answerKeyToLink: (AnswerKey) => String
                     ): Option[EnqueteDto] = DB.withTransaction { implicit session =>
    Enquetes.findByAdminKey(adminKey).map { case (eId, title, description, answerKey, _) =>
      val questions = Questions.findById(eId).map { case (_, qId, _, qDescription, answerType) =>
        val options = QuestionOptions.findById(eId, qId).map { case (_, _, oId, _, oDescription) =>
          QuestionOptionDto(Some(oId), oDescription)
        }
        QuestionDto(Some(qId), qDescription, answerType, options)
      }
      EnqueteDto(title, description, answerKeyToLink(answerKey), questions)
    }
  }

  def findByAnswerKey(answerKey: AnswerKey,
                      answerKeyToLink: (AnswerKey) => String
                      ): Option[EnqueteDto] = DB.withTransaction { implicit session =>
    Enquetes.findByAnswerKey(answerKey).map { case (eId, title, description, _, _) =>
      val questions = Questions.findById(eId).map { case (_, qId, _, qDescription, answerType) =>
        val options = QuestionOptions.findById(eId, qId).map { case (_, _, oId, _, oDescription) =>
          QuestionOptionDto(Some(oId), oDescription)
        }
        QuestionDto(Some(qId), qDescription, answerType, options)
      }
      EnqueteDto(title, description, answerKeyToLink(answerKey), questions)
    }
  }

  def update(adminKey: AdminKey, dto: EnqueteDto): Boolean = DB.withTransaction { implicit session =>
    Enquetes.findIdByAdminKey(adminKey) match {
      case Some(eId) =>
        Enquetes.updateQuery(eId).update((dto.title, dto.description))

        dto.questions.zipWithIndex.foreach { case (q, qIndex) =>
          q.id match {
            case None =>
              val qId = Questions.ins.insert(eId, qIndex, q.description, q.answerType)

              QuestionOptions.ins.insertAll(
                q.questionOptions.zipWithIndex.map { case (o, oIndex) => (eId, qId, oIndex, o.description) }: _*)

            case Some(qId) =>
              Questions.updateQuery(eId, qId).update((qIndex, q.description, q.answerType))

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
