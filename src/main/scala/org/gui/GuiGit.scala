package org.gui

import prefuse.Display

import java.awt.geom.Point2D
import java.awt.Toolkit
import javax.swing.JFrame
import javax.swing.WindowConstants

class GuiGit(display:Display, title: String) {
  val frame = new JFrame(title)
  frame.add(display)
  val screenSize = Toolkit.getDefaultToolkit().getScreenSize()
  frame.setSize(800, (screenSize.height/1.25).intValue)
  frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  frame.setLocationRelativeTo(null)

  frame.setVisible(true)

  // val center = new Point2D.Double(0, 0)
  // display.panTo(center)
}
