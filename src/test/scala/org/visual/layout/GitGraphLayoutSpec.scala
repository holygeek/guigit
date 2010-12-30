package org.visual.layout

import org.scalatest.WordSpec
import org.scalatest.BeforeAndAfterAll

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.storage.file.FileRepository
import org.eclipse.jgit.lib.Repository

import org.domain.GitGraph
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter

class GitGraphLayoutSpec extends WordSpec with BeforeAndAfterAll {
  private var testWorkDir: String = null
  private var workDirectory: File = null
  private var iWorkIn = "testworkdir/GitGraphLayoutSpec"

  private def addAndCommitFile(git: Git, name: String, content: String, commitMsg: String) {
    val os = new BufferedWriter(new FileWriter(new File(workDirectory, name)))
    os.write(content, 0, content.length())
    os.newLine()
    os.close()
    val addCommand = git.add()
    addCommand.addFilepattern(name)
    addCommand.call()
    git.commit().setAuthor("Au Thor", "author@example.com").setMessage(commitMsg).call()
  }

  private def createTestRepository() {
    val gitDir = new File(workDirectory, ".git")
    val gitRepo = new FileRepository(gitDir)
    gitRepo.create()
    val git = new Git(gitRepo)
    addAndCommitFile(git, "a.txt", "one", "one")
    addAndCommitFile(git, "a.txt", "two", "two")
    addAndCommitFile(git, "a.txt", "three", "three")
    addAndCommitFile(git, "a.txt", "four", "four")
  }

  override def beforeAll(configMap: Map[String, Any]) {
    require({
      testWorkDir = System.getProperty("user.dir") + "/target/" + iWorkIn;
      workDirectory = new File(testWorkDir)
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

    createTestRepository()
  }

  // override def afterAll(configMap: Map[String, Any]) {
  //   def deleteDirectory(file:File) : Boolean = {
  //     def deleteFile(dfile : File) : Boolean = {
  //       if(dfile.isDirectory){
  //         val subfiles = dfile.listFiles
  //         if(subfiles != null)
  //           subfiles.foreach{ f => deleteFile(f) }
  //       }
  //       dfile.delete
  //     }
  //     deleteFile(file)
  //   }
  //   require(deleteDirectory(workDirectory), "directory " + iWorkIn + " must be deleted")
  // }

  "A GitGraphLayout" should {
    "be created successfully from GitGraph" in {
      println("Test workDir is " + testWorkDir)
    }
  }
}
