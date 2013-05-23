package models

import play.api.db.slick.Config.driver.simple._

object Questions extends Table[(Int, Int, Int, String, QuestionType)]("question") {

  def enqueteId   = column[Int]("enquete_id")
  def id          = column[Int]("question_id", O.PrimaryKey, O.AutoInc)
  def order       = column[Int]("question_order")
  def description = column[String]("description")
  def questionType = column[QuestionType]("question_type")

  def fk = foreignKey("fk_question_enquete", enqueteId, Enquetes)(_.id)

  def * = enqueteId ~ id ~ order ~ description ~ questionType
  def ins = enqueteId ~ order ~ description ~ questionType returning id

  implicit val questionTypeMapper = MappedTypeMapper.base[QuestionType, Int](_.code, QuestionType.byCode(_))

  val findByIdQuery = for {
    enqueteId <- Parameters[Int]
    q <- Questions if q.enqueteId is enqueteId
  } yield q

  def findById(eId: Int)(implicit session: Session) = findByIdQuery(eId).list

  def updateQuery(eId: Int, qId: Int) = for {
    q <- Questions if (q.enqueteId is eId) && (q.id is qId)
  } yield (q.order ~ q.description ~ q.questionType)

}
