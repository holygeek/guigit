package org.visual.layout

import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._

import prefuse.data.tuple.TupleSet
import prefuse.data.Graph
import prefuse.action.layout.Layout
import prefuse.data.Node
import prefuse.visual.NodeItem
import prefuse.visual.VisualItem

import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk

import org.domain.GitGraph

class GitGraphLayout(gitGraph: GitGraph) extends Layout {
  class NodeDetail(val node: Node) {
    def x = node.get("x").asInstanceOf[Int]
    def y = node.get("y").asInstanceOf[Int]
    def yOffset = node.get("yOffset").asInstanceOf[Int]
    def depth = node.get("depth").asInstanceOf[Int]
    def x_= (x:Int) = node.set("x", x)
    def y_= (y:Int) = node.set("y", y)
    def yOffset_= (yOffset:Int) = node.set("yOffset", yOffset)
    def depth_= (depth: Int) = node.set("depth", depth)
  }
  implicit def nodeDetailWrapper(node: Node) = new NodeDetail(node)
  implicit def nodeDetailWrapper(item: VisualItem) = new NodeDetail(item.asInstanceOf[NodeItem])

  private val ITEMGAP = org.visual.Constants.NODE_SIZE * 4
  private val hasPosition = new HashMap[RevCommit, Boolean]
  private val hasPropagated =new HashMap[RevCommit, Boolean]
  private var gridList = new HashMap[Int, Int]()
  var branches:scala.List[RevCommit] = Nil
  gitGraph.branches.foreach(objectId => {
    try {
      branches = branches ::: List(gitGraph.revWalk.parseCommit(objectId))
    } catch {
      case e: Exception => {
        println("guigit:")
        e.printStackTrace()
        exit
      }
    }
  })

  def setPosition() {
    var i = 0
    branches.foreach(commit => { setPosition(commit, 0, i); i += 1} )
  }

  def propagateOffset() {
    branches.foreach(commit => propagateOffset(commit, 0) )
  }
  def valign() {
    var i = 0
    branches.foreach(commit => { valign(commit, i); i += 1 } )
  }

  // TODO use higher order function for going over first and
  // second pass
  override def run(frac: Double): Unit = {

    println("1. Set position");
    setPosition()
    println("2. Propagate offset")
    propagateOffset()
    println("3. valign")
    valign()
    println("4. Set XY position")
    setXYPosition()
    println("done")
  }

  private def valign(commit: RevCommit, x: Int) {
    // val parents = commit.getParents()
    // if (parents == null)
    //   return
    // var x = 0
    // parents.foreach(
    //   (commit: RevCommit) => {
    //     val node = gitGraph.getNode(commit)
    //   })
  }

  private def setXYPosition() {
    m_vis.items("graph.nodes").foreach(
      (obj:Any) => {
        val item = obj.asInstanceOf[VisualItem]
        setX(item, null, item.x * ITEMGAP)
        setY(item, null, item.y * ITEMGAP)
      }
    )
  }

  private def propagateOffset(commit: RevCommit, currY: Int): Any = {
    if (alreadyPropagated(commit))
      return

    hasPropagated(commit) = true

    val node = gitGraph.getNode(commit)
    if (node.y < currY) {
      node.y = currY + 1
    }

    val parents = commit.getParents()
    if (parents == null)
      return

    parents.foreach(
              (commit: RevCommit)
                  => propagateOffset(commit, node.y + 1)
    )
  }

  private def alreadyPositioned(commit: RevCommit) = hasPosition.getOrElse(commit, false)
  private def alreadyPropagated(commit: RevCommit) = hasPropagated.getOrElse(commit, false)

  private def setPosition(commit: RevCommit, row: Int, minCol: Int): Any = {
    val node = gitGraph.getNode(commit)
    if (alreadyPositioned(commit)) {
      //println("commit " + commit.getShortMessage() + " is already positioned")
      if (node.y < row)
        node.y = row + 1
      return
    }

    var col = gridList.getOrElse(row, 0)
    if (col < minCol)
      col = minCol
    node.x = col
    node.y = row
    gridList(row) = col + 1
    hasPosition(commit) = true

    // commit.getParentCount() give NPE when there's no parent - TODO send bug
    // report to jgit (it should return 0 instead
    val parents = commit.getParents()
    if (parents == null) {
      return
    }

    var nextMinCol = minCol
    parents.foreach(
              (commit: RevCommit)
                  => {
                    //          commit does not work, have to parse it, find out why
                    setPosition(gitGraph.revWalk.parseCommit(commit), row + 1, nextMinCol)
                    nextMinCol += 1
                  }
           )
  }
}
