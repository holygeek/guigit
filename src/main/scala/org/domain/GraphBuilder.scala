package org.domain

import scala.collection.JavaConversions._

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.LogCommand
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk

import prefuse.data.Graph
import prefuse.data.Table
import prefuse.data.Node


class GraphBuilder(branches:Array[String]) {
  var graph = new Graph(true /*directed*/)
  val gitGraph = new GitGraph(graph)
  var ok = false

  def build(): GraphBuilder = {
    var good = false
    val nodesTable = graph.getNodeTable()
    nodesTable.addColumn("revcommit", classOf[RevCommit])

    var rootCommits: scala.List[Node] = Nil

    try {
      var builder = new FileRepositoryBuilder()
      var repository = builder.setGitDir(null).readEnvironment().findGitDir().build()
      gitGraph.revWalk = new RevWalk(repository)

      var g = new Git(repository)
      var log = g.log()

      //repository.getAllRefs()
      //          .keySet()
      //          .filter(_.matches("^refs/(heads|remotes)/"))
      //          .foreach(refname => log.add(repository.resolve(refname)))
      branches.foreach(branch => {
        val objectId = repository.resolve(branch)
        log.add(objectId)
        gitGraph.addBranch(objectId)
      })

      for(commit:RevCommit <- log.call().iterator()) {
        var node = gitGraph.connect(commit, commit.getParents())
        if (commit.getParentCount() == 0)
          rootCommits = rootCommits ::: scala.List(node)
      }
      updateSpanningTree(graph, rootCommits)
      ok = true
    } catch {
      case e : Exception => {
                    println("guigit:")
                    e.printStackTrace()
                    graph = null
               }
    }

    return this
  }
  private def updateSpanningTree(graph: Graph, rootCommits: scala.List[Node]):Any = {
    val n_rootCommits = rootCommits.size()
    if (n_rootCommits > 0) {
      println("We have " + n_rootCommits + " root commit(s)")
      val firstRoot = Help.createSpanningTreeFor(graph, rootCommits.head)
      println("First root node:\n" + Help.format(firstRoot))
    }
  }

}
