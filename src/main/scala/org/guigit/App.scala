package org.guigit;

import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import scala.collection.JavaConversions._

object App extends Application
{

  try {
    var builder = new FileRepositoryBuilder()
    var repository = builder.readEnvironment()
                            .findGitDir()
                            .build();

    val allrefs = repository.getAllRefs()
    allrefs.keySet().foreach(
                      (refname:String) => {
                        println("Ref: " + refname + ": " + allrefs.get(refname))
                      }
                    )

    var g = new Git(repository);
    var log = g.log().add(repository.resolve("HEAD"))


    var a = 1
    log.call().iterator().foreach(
                            (commit:RevCommit) => {
                              if (a < 5) {
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
