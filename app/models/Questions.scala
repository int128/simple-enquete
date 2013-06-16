package models

import play.api.db.slick.Config.driver.simple._

case class Question(enqueteId: Int,
                    id: Int,
                    order: Int,
                    description: String,
                    questionType: QuestionType) {

  def questionOptions(implicit s: Session): List[QuestionOption] = QuestionOptions.find(enqueteId, id)

  def fetch(implicit s: Session): FetchedQuestion = FetchedQuestion(this, questionOptions)

}

case class FetchedQuestion(question: Question, questionOptions: List[QuestionOption])

object Questions extends Table[Question]("question") {

  def enqueteId   = column[Int]("enquete_id")
  def id          = column[Int]("question_id", O.PrimaryKey, O.AutoInc)
  def order       = column[Int]("question_order")
  def description = column[String]("description")
  def questionType = column[QuestionType]("question_type")

  def fk = foreignKey("fk_question_enquete", enqueteId, Enquetes)(_.id)

  def * = enqueteId ~ id ~ order ~ description ~ questionType <> (Question, Question.unapply(_))
  def ins = enqueteId ~ order ~ description ~ questionType returning id

  implicit val questionTypeMapper = MappedTypeMapper.base[QuestionType, Int](_.code, QuestionType.byCode(_))

  val findByIdQuery = for {
    enqueteId <- Parameters[Int]
    q <- Questions if q.enqueteId is enqueteId
  } yield q

  def insert(enqueteId: Int,
             order: Int,
             description: String,
             questionType: QuestionType)(implicit s: Session): Question = {
    val id = Questions.ins.insert(enqueteId, order, description, questionType)
    Question(enqueteId, id, order, description, questionType)
  }

  def find(enqueteId: Int)(implicit s: Session): List[Question] = findByIdQuery(enqueteId).list

  def update(enqueteId: Int, questionId: Int, order: Int, description: String)(implicit s: Session) = for {
    q <- Questions
    if (q.enqueteId is enqueteId) && (q.id is questionId)
  } yield order ~ description

}
