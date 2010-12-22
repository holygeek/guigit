package org.guigit

import java.io.File
import java.io.IOException
import java.awt.Toolkit
import java.awt.geom.Rectangle2D
import java.awt.Shape
import java.awt.Color
import java.awt.geom.Point2D
import javax.swing.JFrame
import javax.swing.WindowConstants
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
import prefuse.Visualization
import prefuse.render.LabelRenderer
import prefuse.render.ShapeRenderer
import prefuse.render.AbstractShapeRenderer
import prefuse.render.DefaultRendererFactory
import prefuse.action.assignment.ColorAction
import prefuse.visual.VisualItem
import prefuse.util.ColorLib
import prefuse.action.ActionList
import prefuse.action.assignment.ShapeAction
import prefuse.activity.Activity
import prefuse.action.layout.graph.ForceDirectedLayout
import prefuse.action.RepaintAction
import prefuse.controls.DragControl
import prefuse.controls.PanControl
import prefuse.controls.ZoomControl
import prefuse.visual.expression.InGroupPredicate

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

object App extends Application
{

  var good = false
  val nodesTable = new Table()
  nodesTable.addColumn("revcommit", classOf[RevCommit])
  val rowIdFor = new HashMap[RevCommit, Int]
  val edgeMap = new HashMap[RevCommit, Array[RevCommit]]

  try {
    var builder = new FileRepositoryBuilder()
    var repository = builder.readEnvironment()
                            .findGitDir()
                            .build();

    val allrefs = repository.getAllRefs()
    var g = new Git(repository);
    var log = g.log()

    allrefs.keySet().foreach(
                      (refname:String) => {
                        if (refname.matches("^refs/heads/")
                              || refname.matches("^refs/remotes/")) {
                          println("Adding: " + refname)
                          log.add(repository.resolve(refname))
                        }
                      }
                    )



    var a = 1
    log.call()
        .iterator()
        .foreach(
          (commit:RevCommit) => {
            if (a < 100) {
              val row = nodesTable.addRow()
              nodesTable.set(row, "revcommit", commit)
              rowIdFor += commit -> row
              edgeMap += commit -> commit.getParents()
            }
            a += 1
          }
        )
    good = true
  } catch {
    case e : Exception => { println("guigit:");  e.printStackTrace() }
  }

  if (!good)
    exit()

  val graph = new Graph(nodesTable, true /* directed */)
  val edges = graph.getEdgeTable()
  edgeMap.foreach((parentMap) => {
        val commitId = rowIdFor.getOrElse(parentMap._1, -1)
        parentMap._2.foreach((commit) => {
            val parentId:Int = rowIdFor.getOrElse(commit, -1)
            if (commitId != -1 && parentId != -1)
              graph.addEdge(commitId, parentId)
            else
              println("FIXME: Got -1")
        })
    })

  val vis = new Visualization()
  vis.add("graph", graph)

  val rf = new DefaultRendererFactory()
  val nodeRenderer = new ShapeRenderer(10)
  rf.setDefaultRenderer(nodeRenderer)
  vis.setRendererFactory(rf)
  //val textColorAction = new ColorAction("graph.nodes",
  //          VisualItem.TEXTCOLOR,
  //          ColorLib.gray(0))

  // Nodes
  var nodeShapeAction = new ShapeAction("graph.nodes")
  nodeShapeAction.setDefaultShape(prefuse.Constants.SHAPE_ELLIPSE)
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

  var layout = new ActionList(Activity.INFINITY);
  layout.add(new ForceDirectedLayout("graph"));
  layout.add(new RepaintAction());

  vis.putAction("shape", nodeShapeAction)
  vis.putAction("color", color);
  vis.putAction("layout", layout);

  val display = new Display(vis)
  // drag individual items around
  display.addControlListener(new DragControl());
  // pan with left-click drag on background
  display.addControlListener(new PanControl());
  // zoom with right-click drag
  display.addControlListener(new ZoomControl());

  val frame = new JFrame("GuiGit")
  frame.add(display)
  val screenSize = Toolkit.getDefaultToolkit().getScreenSize()
  frame.setSize(800, (screenSize.height/1.25).intValue)
  frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  frame.setLocationRelativeTo(null)

  frame.setVisible(true)
  val center = new Point2D.Double(0, 0)
  display.panTo(center)
  vis.run("shape")
  vis.run("color")
  vis.run("layout")
}
