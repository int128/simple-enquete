package models

import scala.util._

case class AdminKey(value: String)

object AdminKey {
  def random(): AdminKey = new AdminKey(Random.alphanumeric.take(Random.nextInt(16) + 16).mkString)
}
