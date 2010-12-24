package org.domain

import scala.collection.JavaConversions._

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.LogCommand
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.lib.Repository

import prefuse.data.Graph
import prefuse.data.Table
import prefuse.data.Node

class GraphBuilder {
  var graph = new Graph(true /*directed*/)

  def build(): GraphBuilder = {
    var good = false
    val nodesTable = graph.getNodeTable()
    nodesTable.addColumn("revcommit", classOf[RevCommit])

    var rootCommits: scala.List[Node] = Nil

    try {
      var builder = new FileRepositoryBuilder()
      var repository = builder.readEnvironment().findGitDir().build()

      var g = new Git(repository)
      var log = g.log()

      repository.getAllRefs()
                .keySet()
                .filter(_.matches("^refs/(heads|remotes)/"))
                .foreach(refname => log.add(repository.resolve(refname)))

      var graphEdges = new GraphEdges(graph)
      for(commit:RevCommit <- log.call().iterator()) {
        var node = graphEdges.connect(commit, commit.getParents())
        if (commit.getParentCount() == 0)
          rootCommits = rootCommits ::: scala.List(node)
      }
      updateSpanningTree(graph, rootCommits)
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
