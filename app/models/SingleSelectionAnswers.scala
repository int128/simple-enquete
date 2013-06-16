package models

import play.api.db.slick.Config.driver.simple._
import java.util.UUID

case class SingleSelectionAnswer(answererId: UUID,
                                 questionId: Int,
                                 questionOptionId: Int)

object SingleSelectionAnswers extends Table[SingleSelectionAnswer]("single_selection_answer") {

  def answererId = column[UUID]("answerer_id")
  def questionId = column[Int]("question_id")
  def questionOptionId = column[Int]("question_option_id")

  def pk = primaryKey(s"pk_$tableName", answererId ~ questionId)

  def questionFk = foreignKey(s"fk_${tableName}_question", questionId, Questions)(_.id)

  def * = answererId ~ questionId ~ questionOptionId <> (SingleSelectionAnswer, SingleSelectionAnswer.unapply _)

  def create(one: SingleSelectionAnswer)(implicit s: Session): SingleSelectionAnswer = {
    SingleSelectionAnswers.insert(one)
    one
  }

}
