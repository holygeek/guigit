package org.guigit

import java.io.File
import java.io.IOException
import java.awt.Toolkit
import javax.swing.JFrame
import javax.swing.WindowConstants
import javax.swing.SwingUtilities
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.LogCommand
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

import prefuse.Display
import prefuse.data.Graph
import prefuse.data.Table
import prefuse.Visualization

import scala.collection.JavaConversions._

object App extends Application
{

  var good = false
  try {
    var builder = new FileRepositoryBuilder()
    var repository = builder.readEnvironment()
                            .findGitDir()
                            .build();

    val allrefs = repository.getAllRefs()
    allrefs.keySet().foreach(
                      (refname:String) => {
                        println("Ref: " + refname + ": " + allrefs.get(refname))
                      }
                    )

    var g = new Git(repository);
    var log = g.log().add(repository.resolve("HEAD"))


    var a = 1
    log.call().iterator().foreach(
                            (commit:RevCommit) => {
                              if (a < 5) {
                                println(commit.getFullMessage())
                              }
                              a += 1
                            }
                          )
    good = true
  } catch {
    case e:IOException => println("guigit: " + e.getStackTrace())
    case e:NoHeadException => println("guigit: " + e.getStackTrace())
  }

  if (!good)
    exit()

  val vis = new Visualization()
  val display = new Display(vis)

  val frame = new JFrame("GuiGit")
  frame.add(display)
  val screenSize = Toolkit.getDefaultToolkit().getScreenSize()
  frame.setSize(800, (screenSize.height/1.25).intValue)
  frame.setBackground(java.awt.Color.blue)
  frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  frame.setLocationRelativeTo(null)

  frame.setVisible(true)
}
