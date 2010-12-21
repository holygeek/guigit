import sbt._

class GuigitProject(info: ProjectInfo) extends DefaultProject(info)
{
    val repository_javanet = "javanet" at "http://download.java.net/maven/2"
    val repository_jgit = "jgit" at "http://download.eclipse.org/jgit/maven"

    val library_jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "0.9.3"
    val library_prefuse = "org.prefuse" % "prefuse" % "beta-20071021"
}
