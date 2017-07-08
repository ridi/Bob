package utils

object ImplicitConversions {
  implicit class ArrayThings(obj: Any) {
    def isIn(target: Seq[Any]): Boolean = target contains obj
    def isNotIn(target: Seq[Any]): Boolean = !isIn(target)
  }

  implicit class NumberThings(n: Int) {
    val isOdd: Boolean = n % 2 == 1
  }
}
