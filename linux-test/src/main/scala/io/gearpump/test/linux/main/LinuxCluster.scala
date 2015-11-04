/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gearpump.test.linux.main

import java.io.File

import io.gearpump.cluster.main.{ArgumentsParser, CLIOption}
import io.gearpump.test.linux.LinuxClusterBuilder

import scala.sys.process.Process

object LinuxCluster extends ArgumentsParser{

  override val options: Array[(String, CLIOption[Any])] = Array(
    "cluster" -> CLIOption[String]("<gearpump cluster, spilt with ','>", required = true),
    "masters" -> CLIOption[String]("<masters of cluster, split with ','>", required = true),
    "ui" -> CLIOption[String]("<ui address of cluster>", required = true),
    "spec" -> CLIOption[String]("<specifies a suite class to run>", required = false, defaultValue = null),
    "workernum" -> CLIOption[Int]("<how many workers to start>", required = false, defaultValue = Some(2)))

  def main(args: Array[String]): Unit = {
    val config = parse(args)
    if (config == null) {
      println("please provide necessary arguments with -cluster, -masters and -ui")
      return
    }

    val cluster = parseNodes(config.getString("cluster"))
    println(s"cluster: $cluster")
    val masters = parseNodes(config.getString("masters"))
    val ui = config.getString("ui")
    val spec = config.getString("spec")
    val workerNum = config.getInt("workernum")

    if (cluster == null || masters == null || ui == null) {
      println("please provide cluster, masters and ui configuration with -cluster,-masters and -ui")
      return
    }

    import LinuxClusterBuilder._
    replace("""cluster=".*"""", s"""cluster="$cluster"""", deployFileName, deployFileDir)
    replace("""masters=".*"""", s"""masters="$masters"""", deployFileName, deployFileDir)
    replace("""ui=".*"""", s"""ui="$ui"""", deployFileName, deployFileDir)
    replace("worker_num=[0-9]*", s"worker_num=$workerNum", deployFileName, deployFileDir)

    runTest(spec)

  }

  def parseNodes(cluster: String): String = {
    val nodes = cluster.split(",")
    if (nodes.length < 1) {
      println("no argument provided")
      null
    } else {
      nodes.mkString(" ")
    }
  }

  def replace(origin: String, now: String, fileName: String, dir: File): Unit = {
    val cmd = Seq(
      "sed",
      "-i",
      s"s/$origin/$now/",
      fileName
    )
    Process(cmd, dir).!
  }

  def runTest(spec: String): Unit = {
    //scala -classpath "lib/scalatest_2.11-2.2.4.jar:lib/*" org.scalatest.tools.Runner
    // -R lib/gearpump-linux-test-0.1.jar -s org.apache.gearpump.test.linux.LinuxLocalSpec
    // -f report.log -o

    val cmd = Seq(
      "scala",
      "-classpath",
      "lib/scalatest_2.11-2.2.4.jar:lib/*",
      "org.scalatest.tools.Runner",
      "-R",
      "lib/gearpump-linux-test-0.1.jar"
    )
    var result = cmd
    if (spec != null) {
      result = cmd ++ Seq("-s", s"io.gearpump.test.linux.$spec")
    }
    result = result ++ Seq("-f", "report.log", "-o")

    val rootDir = new File(LinuxClusterBuilder.testFrameworkRoot)
    Process(result, rootDir).!
  }

}
