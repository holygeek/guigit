package org.guigit

import java.io.File
import java.io.IOException
import java.awt.geom.Rectangle2D
import java.awt.Shape
import java.awt.Color

import prefuse.Display
import prefuse.data.Graph
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
import org.visual.layout.GitGraphLayout
import org.gui.GuiGit
import org.domain.GraphBuilder

object App
{
  def main(args:Array[String]) {
    var branches = Array("HEAD")
    if (args.length != 0)
      branches = args
    val graphBuilder = new GraphBuilder(branches) 
    graphBuilder.build()
    if (!graphBuilder.ok)
      exit(1)
    val gitGraph = graphBuilder.gitGraph
    val graph = graphBuilder.graph

    var (vis, actions) = createVisualization(graph)
    vis.putAction("gitGraphLayoutAction", new GitGraphLayout(gitGraph))
    actions = actions ::: List("gitGraphLayoutAction")

    val display = createDisplay(vis)
    val guigit = new GuiGit(display)

    actions.foreach(vis.run(_))
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
    return color
  }

  // def getLayoutActions(graph: Graph):ActionList = {
  //   var layout = new ActionList()

  //   // var nodeLinkTreeLayout = new NodeLinkTreeLayout("graph")
  //   // nodeLinkTreeLayout.setOrientation(prefuse.Constants.ORIENT_TOP_BOTTOM)
  //   // layout.add(nodeLinkTreeLayout)

  //   val gitGraphLayout = new GitGraphLayout(graph, Nil)
  //   layout.add(gitGraphLayout)

  //   return layout
  // }

  def getRepaintActions():ActionList = {
    var repaint = new ActionList(Activity.INFINITY)
    repaint.add(new RepaintAction())
    repaint
  }

  def getNameActionPairs():List[(String, Action)] = {
    return List("shape"   -> getShapeAction("graph.nodes"),
         "color"   -> getColorActions(),
         "repaint" -> getRepaintActions())
  }

  def createRendererFactory():RendererFactory = {
    val rf = new DefaultRendererFactory()
    val nodeRenderer = new ShapeRenderer(org.visual.Constants.NODE_SIZE)
    rf.setDefaultRenderer(nodeRenderer)
    return rf
  }

  def createVisualization(graph: Graph):(Visualization, List[String]) = {
    val vis = new Visualization()
    vis.add("graph", graph)

    vis.setRendererFactory(createRendererFactory())

    var actions:List[String] = Nil
    for((name, action) <- getNameActionPairs) {
      vis.putAction(name, action)
      actions = actions ::: List(name)
    }
    return (vis, actions)
  }

  def createDisplay(vis:Visualization):Display = {
    val display = new Display(vis)
    display.addControlListener(new DragControl())
    display.addControlListener(new PanControl())
    display.addControlListener(new ZoomControl())
    display.addControlListener(new ControlAdapter())
    return display
  }
}
