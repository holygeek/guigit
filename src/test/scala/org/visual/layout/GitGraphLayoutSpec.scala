package org.visual.layout

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterAll

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.storage.file.FileRepository
import org.eclipse.jgit.lib.Repository

import org.domain.GitGraph
import org.domain.GraphBuilder
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter

class GitGraphLayoutSpec extends WordSpec {
  private var testWorkDir: String = null
  private var workDirectory: File = null
  private var iWorkIn = "testworkdir/GitGraphLayoutSpec"
  private var git: Git = null

  private def addAndCommitFile(name: String, content: String, commitMsg: String): RevCommit = {
    val os = new BufferedWriter(new FileWriter(new File(workDirectory, name)))
    os.write(content, 0, content.length())
    os.newLine()
    os.close()
    val addCommand = git.add()
    addCommand.addFilepattern(name)
    addCommand.call()
    git.commit().setAuthor("Au Thor", "author@example.com").setMessage(commitMsg).call()
  }

  private def initGitRepository() {
    val gitDir = new File(workDirectory, ".git")
    val gitRepo = new FileRepository(gitDir)
    gitRepo.create()
    git = new Git(gitRepo)
  }

  def createRepo(reponame: String) {
    require({
      testWorkDir = System.getProperty("user.dir") + "/target/" + iWorkIn;
      workDirectory = new File(testWorkDir, reponame)
      if (workDirectory.exists()) {
        def deleteDirectory(file:File) : Boolean = {
          def deleteFile(dfile : File) : Boolean = {
            if(dfile.isDirectory){
              val subfiles = dfile.listFiles
              if(subfiles != null)
                subfiles.foreach{ f => deleteFile(f) }
            }
            dfile.delete
          }
          deleteFile(file)
        }
        deleteDirectory(workDirectory)
      }
      workDirectory.mkdirs()
      }, "directory " + iWorkIn + " must be created successfully")

    initGitRepository()
  }

  "A GitGraphLayout" should {
    "layout 4 commmits successfully" in {
      createRepo("01-4-LinearCommits")
      addAndCommitFile("a.txt", "one", "one")
      addAndCommitFile("a.txt", "two", "two")
      addAndCommitFile("a.txt", "three", "three")
      addAndCommitFile("a.txt", "four", "four")
    }
  }

  "A GitGraphLayout" should {
    "layout a merge of 3-2 commits successfully" in {
      createRepo("02-merge-3-2-Commits")
      addAndCommitFile("a.txt", "one", "one")

      git.checkout().setCreateBranch(true).setName("right").call()
      addAndCommitFile("b.txt", "b-two", "b-two")
      val other = addAndCommitFile("b.txt", "b-three", "b-three")

      git.checkout().setName("master").call()
      addAndCommitFile("a.txt", "two", "two")
      addAndCommitFile("a.txt", "three", "three")
      git.merge().include(other).call()
    }
  }

  "A GitGraphLayout" should {
    "layout a merge of 1-1 commits successfully" in {
      val dir = "03-merge-2-3-Commits"
      createRepo(dir)
      addAndCommitFile("a.txt", "one", "one")

      git.checkout().setCreateBranch(true).setName("right").call()
      addAndCommitFile("b.txt", "b-two", "b-two")
      addAndCommitFile("b.txt", "b-three", "b-three")
      val other = addAndCommitFile("b.txt", "b-four", "b-tfour")

      git.checkout().setName("master").call()
      addAndCommitFile("a.txt", "two", "two")
      git.merge().include(other).call()

      val fullpath = new File(testWorkDir, dir).getAbsolutePath()
      val graphBuilder = new GraphBuilder(fullpath, Array("master", "right"))
      graphBuilder.build()
      val gitGraph = graphBuilder.gitGraph
      val gitGraphLayout = new GitGraphLayout(gitGraph)
      gitGraphLayout.firstPass()

      gitGraph.branches.foreach(objectId => {
        try {
          val commit = gitGraph.revWalk.parseCommit(objectId)
          showXYPosition(gitGraph, commit)
        } catch {
          case e: Exception => {
            println("guigit:")
            e.printStackTrace()
          }
        }
      })
    }
  }

  private def showXYPosition(gitGraph: GitGraph, commit: RevCommit):Any = {
    val node = gitGraph.getNode(commit)
    println(node)
    // setX(item, null, item.get("x").asInstanceOf[Int] * ITEMGAP)
    // setY(item, null, item.get("y").asInstanceOf[Int] * ITEMGAP)
    val parents = commit.getParents()
    if (parents == null) {
      return
    }

    parents.foreach(
              (commit: RevCommit)
                  => showXYPosition(gitGraph, gitGraph.revWalk.parseCommit(commit))
    )
  }

}
