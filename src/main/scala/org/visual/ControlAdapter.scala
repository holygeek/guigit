package org.visual

import prefuse.Visualization
import prefuse.action.Action
import prefuse.visual.VisualItem
import prefuse.visual.NodeItem

import org.eclipse.jgit.revwalk.RevCommit

import scala.collection.JavaConversions._

import java.awt.event.MouseEvent

import org.domain.Help

class ControlAdapter extends prefuse.controls.ControlAdapter {
  val HAND_CURSOR = new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)
  val DEFAULT_CURSOR =  new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR)

  override def itemClicked(item: VisualItem, e: MouseEvent):Unit = {
    if (! item.isInstanceOf[NodeItem] )
      return

    var revcommit = item.asInstanceOf[VisualItem]
                      .get("revcommit")
                      .asInstanceOf[RevCommit]
    println(Help format revcommit)
  }
  /*
  override def itemEntered(item: VisualItem, e: MouseEvent):Unit = {
    if (item.isInstanceOf[NodeItem] )
      item.getVisualization().getDisplay(0).setCursor(HAND_CURSOR)
  }
  override def itemExited(item: VisualItem, e: MouseEvent):Unit = {
    if (item.isInstanceOf[NodeItem] )
      item.getVisualization().getDisplay(0).setCursor(DEFAULT_CURSOR)
  }
  */
}
