package models

sealed abstract class QuestionType(val code: Int) extends QuestionType.Value

case object SingleSelection extends QuestionType(1)
case object MultipleSelection extends QuestionType(2)
case object Numeric extends QuestionType(10)
case object Text extends QuestionType(20)

object QuestionType extends Enum[QuestionType] {

  def byCode(code: Int): QuestionType = values.find(_.code == code) match {
    case Some(e) => e
    case None => throw new NoSuchElementException(code.toString)
  }

}
