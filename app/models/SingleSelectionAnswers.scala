package models

import play.api.db.slick.Config.driver.simple._
import java.util.UUID

object SingleSelectionAnswers extends Table[(UUID, Int, Int)]("single_selection_answer") {

  def answererId = column[UUID]("answerer_id")
  def questionId = column[Int]("question_id")
  def questionOptionId = column[Int]("question_option_id")

  def pk = primaryKey(s"pk_$tableName", answererId ~ questionId)

  def questionFk = foreignKey(s"fk_${tableName}_question", questionId, Questions)(_.id)

  def * = answererId ~ questionId ~ questionOptionId

}
