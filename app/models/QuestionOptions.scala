package models

import play.api.db.slick.Config.driver.simple._

case class QuestionOption(enqueteId: Int,
                          questionId: Int,
                          id: Int,
                          order: Int,
                          description: String)

object QuestionOptions extends Table[QuestionOption]("question_option") {

  def enqueteId   = column[Int]("enquete_id")
  def questionId  = column[Int]("question_id")
  def id          = column[Int]("question_option_id", O.PrimaryKey, O.AutoInc)
  def order       = column[Int]("question_option_order")
  def description = column[String]("description")

  def enqueteIdFk  = foreignKey("fk_question_option_enquete", enqueteId, Enquetes)(_.id)
  def questionIdFk = foreignKey("fk_question_option_question", questionId, Questions)(_.id)

  def * = enqueteId ~ questionId ~ id ~ order ~ description <> (QuestionOption, QuestionOption.unapply(_))
  def ins = enqueteId ~ questionId ~ order ~ description returning id

  val findByIdQuery = for {
    (enqueteId, questionId) <- Parameters[(Int, Int)]
    o <- QuestionOptions if (o.enqueteId is enqueteId) && (o.questionId is questionId)
  } yield o

  def findById(eId: Int, qId: Int)(implicit session: Session) = findByIdQuery(eId, qId).list

  def updateQuery(eId: Int, qId: Int, oId: Int) = for {
    o <- QuestionOptions if (o.enqueteId is eId) && (o.questionId is qId) && (o.id is oId)
  } yield (o.order ~ o.description)

}
