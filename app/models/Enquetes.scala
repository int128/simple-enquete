package models

import play.api.db.slick.Config.driver.simple._

object Enquetes extends Table[(Int, String, Option[String], String, String)]("enquete") {
  def id          = column[Int]("enquete_id", O.PrimaryKey, O.AutoInc)
  def title       = column[String]("title")
  def description = column[Option[String]]("description")
  def answerKey   = column[String]("answer_key")
  def adminKey    = column[String]("admin_key")

  def * = id ~ title ~ description ~ answerKey ~ adminKey
  def ins = title ~ description ~ answerKey ~ adminKey returning id

  val findByAdminKeyQuery = for {
    adminKey <- Parameters[String]
    e <- Enquetes if e.adminKey is adminKey
  } yield e

  def findByAdminKey(adminKey: String)(implicit session: Session) =
    findByAdminKeyQuery(adminKey).firstOption

  val findIdByAdminKeyQuery = for {
    adminKey <- Parameters[String]
    e <- Enquetes if e.adminKey is adminKey
  } yield e.id

  def findIdByAdminKey(adminKey: String)(implicit session: Session): Option[Int] =
    findIdByAdminKeyQuery(adminKey).firstOption

  val findByAnswerKeyQuery = for {
    answerKey <- Parameters[String]
    e <- Enquetes if e.answerKey is answerKey
  } yield e

  def findByAnswerKey(answerKey: String)(implicit session: Session) =
    findByAnswerKeyQuery(answerKey).firstOption

  val findIdByAnswerKeyQuery = for {
    answerKey <- Parameters[String]
    e <- Enquetes if e.answerKey is answerKey
  } yield e.id

  def findIdByAnswerKey(answerKey: String)(implicit session: Session): Option[Int] =
    findIdByAnswerKeyQuery(answerKey).firstOption

  def updateQuery(id: Int) = for {
    e <- Enquetes if e.id is id
  } yield (e.title ~ e.description)
}
