package models

import play.api.db.slick.Config.driver.simple._

object Enquetes extends Table[(Int, String, Option[String], AnswerKey, AdminKey)]("enquete") {

  def id          = column[Int]("enquete_id", O.PrimaryKey, O.AutoInc)
  def title       = column[String]("title")
  def description = column[Option[String]]("description")
  def answerKey   = column[AnswerKey]("answer_key")
  def adminKey    = column[AdminKey]("admin_key")

  def * = id ~ title ~ description ~ answerKey ~ adminKey
  def ins = title ~ description ~ answerKey ~ adminKey returning id

  implicit val answerKeyTypeMapper = MappedTypeMapper.base[AnswerKey, String](_.value, AnswerKey(_))
  implicit val adminKeyTypeMapper = MappedTypeMapper.base[AdminKey, String](_.value, AdminKey(_))

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
