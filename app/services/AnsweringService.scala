package services

import models._
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import play.api.Play.current
import java.util.UUID

object AnsweringService {

  def answer(answerKey: AnswerKey, uid: UUID, questionAndAnswers: Map[Int, Seq[String]]): Either[Option[Int], Unit] = DB.withTransaction {
    implicit session =>

    Enquetes.findIdByAnswerKey(answerKey) match {
      case Some(eId) =>
        val questions = Questions.findById(eId).map { case (_, qId, _, _, answerType) => (qId, answerType) }

        val givenQuestionIds = questionAndAnswers.keySet
        val expectedQuestionIds = questions.map(_._1).toSet

        (givenQuestionIds == expectedQuestionIds) match {
          case true =>
            questions.forall {
              case (qId, answerType) if answerType == "singleSelection" =>
                // TODO: check if can toInt
                questionAndAnswers.get(qId).fold(false)(_.length == 1)

            } match {
              case true =>
                questions.map {
                  case (qId, answerType) if answerType == "singleSelection" =>
                    SingleSelectionAnswers.insert((uid, qId, questionAndAnswers(qId).head.toInt))
                }
                Left(Some(eId))

              case false => Right()
            }

          case false => Right()
        }

      case None => Left(None)
    }
  }

}
