package org.domain

import prefuse.data.Graph
import prefuse.data.Node

import scala.collection.mutable.HashMap
import org.eclipse.jgit.revwalk.RevCommit

/* Helper class to create edges connecting the nodes in graph */
class GraphEdges(graph:Graph) {
  private val nodeFor = new HashMap[RevCommit, Node]

  def connect(commit: RevCommit, parents: Array[RevCommit]): Node = {
    val node = createOrGetNode(commit)
    for(parent <- parents) {
      val parentNode = createOrGetNode(parent)
      graph.addEdge(node, parentNode)
    }
    return node
  }

  private def createOrGetNode(commit: RevCommit): Node = {
    nodeFor.getOrElse(commit, {
        val node = graph.addNode()
        node.set("revcommit", commit)
        nodeFor(commit) = node
        node
      })
  }
}
