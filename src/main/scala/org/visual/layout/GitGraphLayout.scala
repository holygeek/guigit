package org.visual.layout

import scala.collection.mutable.HashMap

import prefuse.data.tuple.TupleSet
import prefuse.data.Graph
import prefuse.action.layout.Layout
import prefuse.data.Node
import prefuse.visual.NodeItem

import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk

import org.domain.GitGraph

class GitGraphLayout(gitGraph: GitGraph) extends Layout {
  private val ITEMGAP = org.visual.Constants.NODE_SIZE * 4
  private val alreadyPositioned = new HashMap[RevCommit, Boolean] 
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
    println("Firstpass")
    firstPass()
    println("Firstpass done")
    println("2nd pass")
    gitGraph.branches.foreach(objectId => {
      try {
        val commit = gitGraph.revWalk.parseCommit(objectId)
        setXYPosition(commit, 0)
      } catch {
        case e: Exception => {
          println("guigit:")
          e.printStackTrace()
        }
      }
    })
    println("2nd pass done")
  }

  private def moveDown(commit: RevCommit, row: Int): Any = {

    val node = gitGraph.getNode(commit)

    val col = gridList.getOrElse(row - 1, 0)
    gridList(row) = col + 1
    node.set("y", row)

    val parents = commit.getParents()
    if (parents == null)
      return

    parents.foreach(
              (commit: RevCommit) => {
                      val cmt = commit
                      // val cmt = gitGraph.revWalk.parseCommit(commit)
                      if (alreadyPositioned.getOrElse(cmt, false))
                        moveDown(cmt, row)
              }
           )
  }

  private def setXYPosition(commit: RevCommit, currDown: Int): Any = {
    val node = gitGraph.getNode(commit)
    val item = m_vis.getVisualItem("graph.nodes", node)
    val yOffset = item.get("yOffset").asInstanceOf[Int]

    val nextDown = currDown + yOffset
    setX(item, null, item.get("x").asInstanceOf[Int] * ITEMGAP)
    setY(item, null, (item.get("y").asInstanceOf[Int] + nextDown) * ITEMGAP)
    val parents = commit.getParents()
    if (parents == null) {
      return
    }

    parents.foreach(
              (commit: RevCommit)
                  => setXYPosition(commit, nextDown)
    )
  }

  private def setPosition(commit: RevCommit, row: Int, minCol: Int): Any = {
    val node = gitGraph.getNode(commit)
    if (alreadyPositioned.getOrElse(commit, false)) {
      val currRow = node.get("y").asInstanceOf[Int]
      if (currRow <= row) {
        val yOffset = row - currRow
        node.set("yOffset", yOffset)
      }
      return
    }

    var col = gridList.getOrElse(row, minCol)
    node.set("x", col)
    node.set("y", row)
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
