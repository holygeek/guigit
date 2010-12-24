package org.guigit

import java.io.File
import java.io.IOException
import java.awt.geom.Rectangle2D
import java.awt.Shape
import java.awt.Color
import javax.swing.SwingUtilities
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.LogCommand
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

import prefuse.Display
import prefuse.data.Graph
import prefuse.data.Table
import prefuse.data.Node
import prefuse.action.Action
import prefuse.visual.NodeItem
import prefuse.Visualization
import prefuse.render.LabelRenderer
import prefuse.render.ShapeRenderer
import prefuse.render.AbstractShapeRenderer
import prefuse.render.DefaultRendererFactory
import prefuse.render.RendererFactory
import prefuse.action.assignment.ColorAction
import prefuse.visual.VisualItem
import prefuse.util.ColorLib
import prefuse.action.ActionList
import prefuse.action.assignment.ShapeAction
import prefuse.activity.Activity
import prefuse.action.layout.graph.ForceDirectedLayout
import prefuse.action.layout.graph.NodeLinkTreeLayout
import prefuse.action.RepaintAction
import prefuse.controls.DragControl
import prefuse.controls.PanControl
import prefuse.controls.ZoomControl
import prefuse.visual.expression.InGroupPredicate

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.collection.mutable.LinkedList

import org.domain.Help
import org.visual.ControlAdapter
import org.gui.GuiGit

object App
{
  def main(args:Array[String]) {
    var good = false
    val nodesTable = new Table()
    nodesTable.addColumn("revcommit", classOf[RevCommit])
    val rowIdFor = new HashMap[RevCommit, Int]
    val edgeMap = new HashMap[RevCommit, Array[RevCommit]]
    var rootCommitIds: scala.List[Int] = Nil

    try {
      var builder = new FileRepositoryBuilder()
      var repository = builder.readEnvironment()
                              .findGitDir()
                              .build()

      val allrefs = repository.getAllRefs()
      var g = new Git(repository)
      var log = g.log()

      for(refname <- allrefs.keySet();
         if refname.matches("^refs/(heads|remotes)/")) {
                      println("Adding: " + refname)
                      log.add(repository.resolve(refname))
      }

      //var a = 1
      log.call()
          .iterator()
          .foreach(
            (commit:RevCommit) => {
              //if (a < 100) {
                val row = nodesTable.addRow()
                nodesTable.set(row, "revcommit", commit)
                rowIdFor += commit -> row
                val n_parent = commit.getParentCount()
                if (n_parent > 0)
                  edgeMap += commit -> commit.getParents()
                else
                  rootCommitIds = rootCommitIds ::: scala.List(row)
              //}
              //a += 1
            }
          )
      good = true
    } catch {
      case e : Exception => {
                    println("guigit:")
                    e.printStackTrace()
               }
    }

    if (!good)
      exit()

    val graph = createGraph(nodesTable, edgeMap, rowIdFor)
    val vis = createVisualization(graph, rootCommitIds)
    val display = createDisplay(vis)
    val guigit = new GuiGit(display)

    List("shape", "color", "layout", "repaint").foreach(vis.run(_))
  }

  def createGraph(nodesTable: Table,
                  edgeMap: HashMap[RevCommit, Array[RevCommit]],
                  rowIdFor: HashMap[RevCommit, Int]) = {
    val graph = new Graph(nodesTable, true /* directed */)
    createEdges(graph, edgeMap, rowIdFor)
    graph
  }

  def createEdges(graph:Graph, edgeMap:HashMap[RevCommit, Array[RevCommit]],
                                rowIdFor:HashMap[RevCommit, Int]):Unit = {
    val edges = graph.getEdgeTable()
    edgeMap.foreach((parentMap) => {
          val commitId = rowIdFor.getOrElse(parentMap._1, -1)
          parentMap._2.foreach((commit) => {
              val parentId:Int = rowIdFor.getOrElse(commit, -1)
              if (commitId != -1 && parentId != -1) {
                graph.addEdge(parentId, commitId)
              }
              else
                println("FIXME: Got -1")
          })
      })
  }

  def getShapeAction(nodesGroup:String):ShapeAction = {
    var nodeShapeAction = new ShapeAction(nodesGroup)
    nodeShapeAction.setDefaultShape(prefuse.Constants.SHAPE_ELLIPSE)
    nodeShapeAction
  }

  def getColorActions():ActionList = {
    //val textColorAction = new ColorAction("graph.nodes",
    //          VisualItem.TEXTCOLOR,
    //          ColorLib.gray(0))

    // Nodes
    var nodeStrokeAction = new ColorAction("graph.nodes",
           VisualItem.STROKECOLOR,
           ColorLib.color(Color.blue))
    var nodeFillColorAction = new ColorAction("graph.nodes",
           VisualItem.FILLCOLOR,
           ColorLib.color(Color.orange))

    // Edges
    var edgeColorAction = new ColorAction("graph.edges",
           VisualItem.STROKECOLOR,
           ColorLib.gray(200))
    var arrowHeadColorAction = new ColorAction("graph.edges",
           VisualItem.FILLCOLOR,
           ColorLib.gray(200))

    var color = new ActionList()
    //color.add(textColorAction)
    color.add(nodeStrokeAction)
    color.add(nodeFillColorAction)
    color.add(edgeColorAction)
    color.add(arrowHeadColorAction)
    color
  }

  def getLayoutActions():ActionList = {
    var layout = new ActionList()
    var nodeLinkTreeLayout = new NodeLinkTreeLayout("graph")
    nodeLinkTreeLayout.setOrientation(prefuse.Constants.ORIENT_TOP_BOTTOM)
    layout.add(nodeLinkTreeLayout)
    layout
  }

  def getRepaintActions():ActionList = {
    var repaint = new ActionList(Activity.INFINITY)
    repaint.add(new RepaintAction())
    repaint
  }

  def getNameActionPairs():List[(String, Action)] = {
    List("shape"   -> getShapeAction("graph.nodes"),
         "color"   -> getColorActions(),
         "layout"  -> getLayoutActions(),
         "repaint" -> getRepaintActions())
  }

  def createRendererFactory():RendererFactory = {
    val rf = new DefaultRendererFactory()
    val nodeRenderer = new ShapeRenderer(10)
    rf.setDefaultRenderer(nodeRenderer)
    rf
  }

  def createVisualization(graph: Graph, rootCommitIds: scala.List[Int]):Visualization = {
    val vis = new Visualization()
    vis.add("graph", graph)

    vis.setRendererFactory(createRendererFactory())

    val n_rootCommits = rootCommitIds.size()
    if (n_rootCommits > 0) {
      println("We have " + n_rootCommits + " root commit(s)")
      val firstRoot = Help.createSpanningTreeFor(graph, rootCommitIds(0))
      println("First root node:\n" + Help.format(firstRoot))
    }

    for((name, action) <- getNameActionPairs) {
      vis.putAction(name, action)
    }
    vis
  }

  def createDisplay(vis:Visualization):Display = {
    val display = new Display(vis)
    display.addControlListener(new DragControl())
    display.addControlListener(new PanControl())
    display.addControlListener(new ZoomControl())
    display.addControlListener(new ControlAdapter())
    display
  }
}
