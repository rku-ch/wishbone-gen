package ch.epfl.lap.wishbone_gen

object NameUtils {
  val OutputSuffix = "_o"
  val InputSuffix = "_i"

  def stripSuffix(name: String): String = {
    name.substring(0, name.length()-2)
  }
}
