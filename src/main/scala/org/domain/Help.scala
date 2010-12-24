package org.domain

import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.lib.AnyObjectId

import prefuse.data.Graph
import prefuse.data.Node

/* A wrapper around RevCommit */
object Help {

  val ABBREVIATE_LENGTH = 8
  def format(revCommit:RevCommit):String = {
    val person = revCommit.getCommitterIdent()
    var buf = new StringBuffer()
    buf.append("commit ").append(revCommit.getId().name())
       .append("\n")
       .append("Author: ").append(person.getName()).append(" <").append(person.getEmailAddress())
       .append(">\n")
       .append("Date:   ").append(revCommit.getCommitTime()).append("\n\n")
       .append(revCommit.getFullMessage()).toString()
  }

  def createSpanningTreeFor(graph: Graph, rootNode:Node):RevCommit = {
    graph.getSpanningTree(rootNode)
    rootNode.get("revcommit").asInstanceOf[RevCommit]
  }
}
