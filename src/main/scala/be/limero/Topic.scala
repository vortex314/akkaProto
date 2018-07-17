package be.limero

object Topic {
  val pattern = "(dst/|src/|)([^/]*)/([^/]*)/([^/]*)".r
}

class Topic(full:String) {
  val Topic.pattern(dir,device, service, property) = full;
  val string: String=toString

  override def toString: String = device + "/" + service + "/" + property

  def matches: String => Boolean = pattern => string.matches(pattern)

}
