package models

import play.api.db.slick.Config.driver.simple._

case class Enquete(id: Int,
                   title: String,
                   description: Option[String],
                   answerKey: AnswerKey,
                   adminKey: AdminKey) {

  def questions(implicit s: Session): List[Question] = Questions.find(id)

  def fetch(implicit s: Session): FetchedEnquete = FetchedEnquete(this, questions.map(_.fetch))

}

case class FetchedEnquete(enquete: Enquete, questions: List[FetchedQuestion])

object Enquetes extends Table[Enquete]("enquete") {

  def id          = column[Int]("enquete_id", O.PrimaryKey, O.AutoInc)
  def title       = column[String]("title")
  def description = column[Option[String]]("description")
  def answerKey   = column[AnswerKey]("answer_key")
  def adminKey    = column[AdminKey]("admin_key")

  def * = id ~ title ~ description ~ answerKey ~ adminKey <> (Enquete, Enquete.unapply(_))
  def ins = title ~ description ~ answerKey ~ adminKey returning id

  implicit val answerKeyTypeMapper = MappedTypeMapper.base[AnswerKey, String](_.value, AnswerKey(_))
  implicit val adminKeyTypeMapper = MappedTypeMapper.base[AdminKey, String](_.value, AdminKey(_))

  val findByAdminKeyQuery = for {
    adminKey <- Parameters[AdminKey]
    e <- Enquetes if e.adminKey is adminKey
  } yield e

  val findByAnswerKeyQuery = for {
    answerKey <- Parameters[AnswerKey]
    e <- Enquetes if e.answerKey is answerKey
  } yield e

  def insert(title: String, description: Option[String])(implicit s: Session): Enquete = {
    val adminKey = AdminKey.random()
    val answerKey = AnswerKey.random()
    val id = Enquetes.ins.insert(title, description, answerKey, adminKey)
    Enquete(id, title, description, answerKey, adminKey)
  }

  def find(adminKey: AdminKey)(implicit s: Session): Option[Enquete] = findByAdminKeyQuery(adminKey).firstOption

  def find(answerKey: AnswerKey)(implicit s: Session): Option[Enquete] = findByAnswerKeyQuery(answerKey).firstOption

  def update(id: Int, title: String, description: Option[String])(implicit s: Session) = for {
    e <- Enquetes
    if e.id is id
  } yield title ~ description

}
