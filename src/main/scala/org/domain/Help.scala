package org.domain

import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.lib.AnyObjectId

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
}
