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
  private val ITEMGAP = org.visual.Constants.NODE_SIZE * 3
  private val alreadyPositioned = new HashMap[RevCommit, Boolean] 
  override def run(frac: Double): Unit = {
    var row = 0
    var col = 0
    gitGraph.branches.foreach(objectId => {
      try {
        val commit = gitGraph.revWalk.parseCommit(objectId)
        setPosition(commit, row, col)
        col += 1
      } catch {
        case e: Exception => {
          println("guigit:")
          e.printStackTrace()
        }
      }
    })
    // branches.foreach(branch => { })
  }

  private def setPosition(commit: RevCommit, row: Int, col: Int): Any = {
    if (! alreadyPositioned.getOrElse(commit, true))
      return

    val node = gitGraph.getNode(commit)
    val item = m_vis.getVisualItem("graph.nodes", node)
    println("Set ax for " + item " at ("  + row + ", " + col + ")")
    setX(item, null, col * ITEMGAP)
    setY(item, null, row * ITEMGAP)
    alreadyPositioned(commit) = true

    // commit.getParentCount() give NPE when there's no parent - TODO send bug report to jgit
    val parents = commit.getParents()
    if (parents == null)
      return

    var first = true
    parents.foreach(cmt => {
      if (first) {
        setPosition(cmt, row + 1, col)
        first = false
      }
      else
        setPosition(cmt, row + 1, col + 1)
    })
  }
}
