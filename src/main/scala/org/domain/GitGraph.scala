package org.domain

import scala.collection.mutable.ListBuffer

import prefuse.data.Graph
import prefuse.data.Node

import scala.collection.mutable.HashMap
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk

/* Helper class to create edges connecting the nodes in graph */
class GitGraph(graph:Graph) {
  private val nodeFor = new HashMap[RevCommit, Node]
  val branches = new ListBuffer[ObjectId]()
  var revWalk: RevWalk = null

  def addBranch(objectId: ObjectId):Any = branches.append(objectId)

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
        return node
      })
  }
  def getNode(commit: RevCommit): Node = {
    return nodeFor.getOrElse(commit, null)
  }
}
