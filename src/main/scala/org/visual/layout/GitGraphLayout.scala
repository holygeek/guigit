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
        setPosition(commit, 0)
      } catch {
        case e: Exception => {
          println("guigit:")
          e.printStackTrace()
        }
      }
    })
  }

  override def run(frac: Double): Unit = {
    firstPass()
    gitGraph.branches.foreach(objectId => {
      try {
        val commit = gitGraph.revWalk.parseCommit(objectId)
        setXYPosition(commit)
      } catch {
        case e: Exception => {
          println("guigit:")
          e.printStackTrace()
        }
      }
    })
  }

  private def moveDown(commit: RevCommit, row: Int): Any = {

    val node = gitGraph.getNode(commit)
    val item = m_vis.getVisualItem("graph.nodes", node)
    val currY = item.getY()

    val col = gridList.getOrElse(row - 1, 0)
    gridList(row) = col + 1
    setY(item, null, row * ITEMGAP)

    val parents = commit.getParents()
    if (parents == null) {
      return
    }

    parents.foreach(
              (commit: RevCommit) => {
                      val cmt = gitGraph.revWalk.parseCommit(commit)
                      if (alreadyPositioned.getOrElse(cmt, false))
                        moveDown(cmt, row)
              }
           )
  }

  private def setXYPosition(commit: RevCommit): Any = {
    val node = gitGraph.getNode(commit)
    val item = m_vis.getVisualItem("graph.nodes", node)
    setX(item, null, item.get("x").asInstanceOf[Int] * ITEMGAP)
    setY(item, null, item.get("y").asInstanceOf[Int] * ITEMGAP)
    val parents = commit.getParents()
    if (parents == null) {
      return
    }

    parents.foreach(
              (commit: RevCommit)
                  => setXYPosition(gitGraph.revWalk.parseCommit(commit))
    )
  }

  private def setPosition(commit: RevCommit, row: Int): Any = {
    val node = gitGraph.getNode(commit)
    val item = m_vis.getVisualItem("graph.nodes", node)
    if (alreadyPositioned.getOrElse(commit, false)) {
      val currRow = (item.getX() / ITEMGAP).asInstanceOf[Int]
      if (currRow > row)
        moveDown(commit, row + 1)
      return
    }

    var col = gridList.getOrElse(row, 0)
    item.set("x", col)
    item.set("y", row)
    // setX(item, null, col * ITEMGAP)
    // setY(item, null, row * ITEMGAP)
    gridList(row) = col + 1
    alreadyPositioned(commit) = true

    // commit.getParentCount() give NPE when there's no parent - TODO send bug
    // report to jgit
    val parents = commit.getParents()
    if (parents == null) {
      return
    }

    parents.foreach(
              (commit: RevCommit)
                  => setPosition(gitGraph.revWalk.parseCommit(commit), row + 1))
  }
}
