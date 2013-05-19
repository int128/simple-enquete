package models

import scala.util._

case class AnswerKey(value: String)

object AnswerKey {
  def random(): AnswerKey = new AnswerKey(Random.alphanumeric.take(Random.nextInt(16) + 16).mkString)
}
