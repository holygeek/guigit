package org.visual

import scala.collection.JavaConversions._

import prefuse.controls.SubtreeDragControl

import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import java.awt.geom.Point2D

import prefuse.Display
import prefuse.data.Node
import prefuse.data.Graph
import prefuse.visual.NodeItem
import prefuse.visual.VisualItem

class SideTreeDragControl extends SubtreeDragControl {
  var minX = 0.0
  var minY = 0.0
  var down:Point2D = null
  var wasFixed = false;

  override def itemPressed(item: VisualItem, e: MouseEvent) {
    if (!SwingUtilities.isLeftMouseButton(e)) return;
    if ( !item.isInstanceOf[NodeItem] ) return;
    minX = item.getX()
    minY = item.getY()

    val d = e.getComponent().asInstanceOf[Display];
    down = d.getAbsoluteCoordinate(e.getPoint(), down);
    wasFixed = item.isFixed();
    item.setFixed(true);
  }

  override def itemDragged(item: VisualItem, e: MouseEvent) {

    if (!SwingUtilities.isLeftMouseButton(e)) return;
    if ( !(item.isInstanceOf[NodeItem]) ) return;

    val d = e.getComponent().asInstanceOf[Display];
    val tmp = d.getAbsoluteCoordinate(e.getPoint(), null);
    val dx = tmp.getX()-down.getX();
    val dy = tmp.getY()-down.getY();

    val graph = d.getVisualization.getGroup("graph").asInstanceOf[Graph]
    org.domain.Help.createSpanningTreeFor(graph, item.asInstanceOf[Node])

    updateLocations(item.asInstanceOf[NodeItem], dx, dy)
    down.setLocation(tmp);
    item.getVisualization().repaint();
  }

  private def updateLocations(n: NodeItem, dx: Double, dy: Double) {
    var x = n.getX();
    var y = n.getY();
    if (x >= minX && y >= minY) {
      n.setStartX(x); n.setStartY(y);
      x += dx; y += dy;
      n.setX(x);    n.setY(y);
      n.setEndX(x); n.setEndY(y);
    }
    
    val children = n.children()
    children.foreach((child) => {
        updateLocations(child.asInstanceOf[NodeItem], dx, dy)
      })
  }
}
