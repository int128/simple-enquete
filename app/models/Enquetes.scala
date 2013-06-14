package models

import play.api.db.slick.Config.driver.simple._

case class Enquete(id: Int,
                   title: String,
                   description: Option[String],
                   answerKey: AnswerKey,
                   adminKey: AdminKey)

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

  def insert(title: String, description: Option[String]): Enquete = {
    val adminKey = AdminKey.random()
    val answerKey = AnswerKey.random()
    val id = Enquetes.ins.insert(title, description, answerKey, adminKey)
    Enquete(id, title, description, answerKey, adminKey)
  }

  val findByAdminKeyQuery = for {
    adminKey <- Parameters[AdminKey]
    e <- Enquetes if e.adminKey is adminKey
  } yield e

  def findByAdminKey(adminKey: AdminKey)(implicit session: Session) =
    findByAdminKeyQuery(adminKey).firstOption

  val findIdByAdminKeyQuery = for {
    adminKey <- Parameters[AdminKey]
    e <- Enquetes if e.adminKey is adminKey
  } yield e.id

  def findIdByAdminKey(adminKey: AdminKey)(implicit session: Session): Option[Int] =
    findIdByAdminKeyQuery(adminKey).firstOption

  val findByAnswerKeyQuery = for {
    answerKey <- Parameters[AnswerKey]
    e <- Enquetes if e.answerKey is answerKey
  } yield e

  def findByAnswerKey(answerKey: AnswerKey)(implicit session: Session) =
    findByAnswerKeyQuery(answerKey).firstOption

  val findIdByAnswerKeyQuery = for {
    answerKey <- Parameters[AnswerKey]
    e <- Enquetes if e.answerKey is answerKey
  } yield e.id

  def findIdByAnswerKey(answerKey: AnswerKey)(implicit session: Session): Option[Int] =
    findIdByAnswerKeyQuery(answerKey).firstOption

  def updateQuery(id: Int) = for {
    e <- Enquetes if e.id is id
  } yield (e.title ~ e.description)

}
