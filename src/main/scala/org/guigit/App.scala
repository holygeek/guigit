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
import org.domain.GraphEdges
import org.visual.ControlAdapter
import org.gui.GuiGit

object App
{
  def main(args:Array[String]) {
    var good = false
    val nodesTable = new Table()
    nodesTable.addColumn("revcommit", classOf[RevCommit])
    var graph = new Graph(nodesTable, true /* directed */)

    val rowIdFor = new HashMap[RevCommit, Int]
    val edgeMap = new HashMap[RevCommit, Array[RevCommit]]
    var rootCommits: scala.List[Node] = Nil

    try {
      var builder = new FileRepositoryBuilder()
      var repository = builder.readEnvironment().findGitDir().build()

      val allrefs = repository.getAllRefs()
      var g = new Git(repository)
      var log = g.log()

      allrefs.keySet()
             .filter(refname => refname.matches("^refs/(heads|remotes)/"))
             .foreach(refname => log.add(repository.resolve(refname)))

      var graphEdges = new GraphEdges(graph)
      for(commit:RevCommit <- log.call().iterator()) {
        var node = graphEdges.connect(commit, commit.getParents())
        if (commit.getParentCount() == 0)
          rootCommits = rootCommits ::: scala.List(node)
      }

      good = true
    } catch {
      case e : Exception => {
                    println("guigit:")
                    e.printStackTrace()
               }
    }

    if (!good)
      exit()

    updateSpanningTree(graph, rootCommits)
    val vis = createVisualization(graph)
    val display = createDisplay(vis)
    val guigit = new GuiGit(display)

    List("shape", "color", "layout", "repaint").foreach(vis.run(_))
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

  def updateSpanningTree(graph: Graph, rootCommits: scala.List[Node]):Any = {
    val n_rootCommits = rootCommits.size()
    if (n_rootCommits > 0) {
      println("We have " + n_rootCommits + " root commit(s)")
      val firstRoot = Help.createSpanningTreeFor(graph, rootCommits(0))
      println("First root node:\n" + Help.format(firstRoot))
    }
  }

  def createVisualization(graph: Graph):Visualization = {
    val vis = new Visualization()
    vis.add("graph", graph)

    vis.setRendererFactory(createRendererFactory())

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
