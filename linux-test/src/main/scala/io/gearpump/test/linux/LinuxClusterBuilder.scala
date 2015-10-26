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

package io.gearpump.test.linux

import java.io.File

import scala.sys.process.Process

object LinuxClusterBuilder extends Builder{

  lazy val testFrameworkRoot = getTestFrameworkRoot
  lazy val deployFileDir = new File(testFrameworkRoot + "/deploy")
  lazy val deployFileName = "deployGearpump.sh"
  lazy val port = "8090"
  private var isInit = false

  override def init(): Unit = {
    if (!isInit) {
      Util.buildProject()
      deployGearpump()
      isInit = true
      Thread.sleep(5000)
    }
  }

  def deployGearpump(): Unit =  {
    println("Deploy Gearpump on Linux cluster")
    val targetDir = new File(Util.targetRoot)
    var gearpumpVersion = (Process("ls", targetDir) #| "grep tar.gz").!!
    gearpumpVersion = gearpumpVersion.replace("gearpump-", "").replace("\n", "").replace(".tar.gz", "")
    println(s"gearpump version: $gearpumpVersion")
    val cmd = Seq(
      "sed",
      "-i",
      s"""s/gearpumpVersion=".*"/gearpumpVersion="$gearpumpVersion"/""",
      deployFileName
    )
    Process(cmd, deployFileDir).!
    val setGearpumpSource = Seq (
      "sed",
      "-i",
      s"""s#gearpumpSource=".*"#gearpumpSource="${Util.targetRoot}"#""",
      deployFileName
    )
    Process(setGearpumpSource, deployFileDir).!
    Process(s"chmod 777 $deployFileName", deployFileDir).!
    val result = Process(s"./$deployFileName deploy", deployFileDir).!
    if (result == 127) {
      Process(s"sed -i s/.$$// $deployFileName", deployFileDir).!
      Process(s"./$deployFileName deploy", deployFileDir).!
    }
    println("Gearpump deployed on cluster")
  }

  def stopGearpump(): Unit = {
    println("Stop Gearpump")
    Process(s"./$deployFileName stop", deployFileDir).!
  }

  override def getUiAddress: String = {
    val ui = Process(s"grep ui= $deployFileName", deployFileDir).!!
    ui.substring(ui.indexOf("ui=") + 4, ui.indexOf("\n") - 1) + ":" + port
  }

  def getTestFrameworkRoot: String = {
    val pwd = Process("pwd").!!.replace("\n", "")
    val start = pwd.indexOf("gearpump-test-framework")
    val end = pwd.indexOf("/", start)
    val root = if (end == -1) pwd else pwd.substring(0, end)
    root
  }

}
