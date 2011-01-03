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
  private val alreadyPositioned = new HashMap[RevCommit, Boolean] 
  private val hasDepth = new HashMap[RevCommit, Boolean]
  private var gridList = new HashMap[Int, Int]()

  def firstPass(): Unit = {
    gitGraph.branches.foreach(objectId => {
      try {
        val commit = gitGraph.revWalk.parseCommit(objectId)
        setPosition(commit, 0, 0)
      } catch {
        case e: Exception => {
          println("guigit:")
          e.printStackTrace()
        }
      }
    })
  }

  // TODO use higher order function for going over first and
  // second pass
  override def run(frac: Double): Unit = {
    print("Firstpass")
    firstPass()
    println(" done")
    print("2nd pass")
    gitGraph.branches.foreach(objectId => {
      try {
        val commit = gitGraph.revWalk.parseCommit(objectId)
        setDepth(commit, 0)
      } catch {
        case e: Exception => {
          println("guigit:")
          e.printStackTrace()
        }
      }
    })
    println(" done")
    print("3rd pass")
    setXYPosition()
    println(" done")
  }

  private def setXYPosition() {
    m_vis.items("graph.nodes").foreach(
      (obj:Any) => {
        val item = obj.asInstanceOf[VisualItem]
        val yOffset = item.yOffset
        val depth = item.depth + yOffset
        setX(item, null, item.x * ITEMGAP)
        setY(item, null, (item.y + depth) * ITEMGAP)
      }
    )
  }

  private def setDepth(commit: RevCommit, currDepth: Int): Any = {
    val node = gitGraph.getNode(commit)
    if (hasDepth.getOrElse(commit, false)) {
      val depth = node.depth
      if (depth < currDepth) {
        node.depth =  currDepth - node.y
      }
      return
    }

    node.depth = currDepth
    hasDepth(commit) = true

    val parents = commit.getParents()
    if (parents == null)
      return

    val nextDepth = currDepth + node.yOffset

    parents.foreach(
              (commit: RevCommit)
                  => setDepth(commit, nextDepth)
    )
  }

  private def setPosition(commit: RevCommit, row: Int, minCol: Int): Any = {
    val node = gitGraph.getNode(commit)
    if (alreadyPositioned.getOrElse(commit, false)) {
      if (node.y <= row)
        node.yOffset= row - node.y
      return
    }

    var col = gridList.getOrElse(row, 0)
    if (col < minCol)
      col = minCol
    node.x = col
    node.y = row
    gridList(row) = col + 1
    alreadyPositioned(commit) = true

    // commit.getParentCount() give NPE when there's no parent - TODO send bug
    // report to jgit
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
