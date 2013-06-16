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

  def insert(enqueteId: Int,
             questionId: Int,
             order: Int,
             description: String)(implicit s: Session): QuestionOption = {
    val id = ins.insert(enqueteId, questionId, order, description)
    QuestionOption(enqueteId, questionId, id, order, description)
  }

  def find(enqueteId: Int,
           questionId: Int)(implicit s: Session): List[QuestionOption] = findByIdQuery(enqueteId, questionId).list

  def update(enqueteId: Int,
             questionId: Int,
             questionOptionId: Int,
             order: Int,
             description: String)(implicit s: Session) = for {
    o <- QuestionOptions
    if (o.enqueteId is enqueteId) && (o.questionId is questionId) && (o.id is questionOptionId)
  } yield o.order ~ o.description

}
