package org.guigit;

import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import scala.collection.jcl.MutableIterator.Wrapper

object App extends Application
{
  implicit def javaIteratorToScalaIterator[A](it : java.util.Iterator[A])
                                                              = new Wrapper(it)

  try {
    var builder = new FileRepositoryBuilder()
    var repository = builder.setGitDir(new File("/home/nazri/src/git/.git"))
                            .readEnvironment()
                            .findGitDir()
                            .build();

    var g = new Git(repository);
    var log = g.log().add(repository.resolve("HEAD"));

    var a = 1
    log.call().iterator().foreach(
                            (commit:RevCommit) => {
                              if (a < 2) {
                                println(commit.getFullMessage())
                              }
                              a += 1
                            }
                          )
  } catch {
    case e:IOException => println("guigit: " + e.getStackTrace())
    case e:NoHeadException => println("guigit: " + e.getStackTrace())
  }

}
