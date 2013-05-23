package models

/**
 * Enumeration.
 *
 * @tparam A type of elements
 * @see http://stackoverflow.com/questions/1898932/case-classes-vs-enumerations-in-scala
 */
trait Enum[A <: {val name: String}] {

  trait Value { self: A =>
    _values :+= this
    val name = toString
  }

  private var _values = List.empty[A]
  def values = _values

  def byName(name: String): A = values.find(_.name == name) match {
    case Some(e) => e
    case None => throw new NoSuchElementException(name)
  }

}
