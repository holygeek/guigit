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
  private var gridList = new HashMap[Int, Int]()
  override def run(frac: Double): Unit = {
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

  private def setPosition(commit: RevCommit, row: Int): Any = {
    if (alreadyPositioned.getOrElse(commit, false)) {
      return
    }

    var col = gridList.getOrElse(row, 0)
    val node = gitGraph.getNode(commit)
    val item = m_vis.getVisualItem("graph.nodes", node)
    setX(item, null, col * ITEMGAP)
    setY(item, null, row * ITEMGAP)
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
