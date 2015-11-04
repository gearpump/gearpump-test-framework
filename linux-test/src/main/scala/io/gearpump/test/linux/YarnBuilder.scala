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

import scala.sys.process.{Process, _}
import Util._

object YarnBuilder extends Builder {

  lazy val hadoopRoot = "/root/hadoop/hadoop-2.7.1"
  lazy val hdfsRoot = "/user/gearpump/"
  private var isInit = false

  def init(): Unit = {
    if (!isInit) {
      Util.buildProject()
      uploadToHdfs()
      deployGearpump()
      isInit = true
    }
  }


  def uploadToHdfs(): Unit = {
    println("upload gearpump to HDFS")
    val hadoopDir = new File(hadoopRoot)
    val targetDir = new File(targetRoot)
    Process(s"sbin/start-dfs.sh", hadoopDir).!
    Process(s"bin/hdfs dfs -mkdir $hdfsRoot", hadoopDir).!
    var gearpumpTar = (Process("ls", targetDir) #| "grep tar.gz").!!
    gearpumpTar = gearpumpTar.replace("\n", "")
    var uploadResult = Process(s"bin/hdfs dfs -put $targetRoot/$gearpumpTar $hdfsRoot", hadoopDir).!
    if (uploadResult != 0) {
      Process(s"bin/hdfs dfs -rm -f $hdfsRoot$gearpumpTar", hadoopDir).!
      uploadResult = Process(s"bin/hdfs dfs -put $targetRoot/$gearpumpTar $hdfsRoot", hadoopDir).!
    }
    println("upload to HDFS successfully")
  }

  def deployGearpump(): Unit = {
    println("deploy gearpump on yarn")
    val hadoopDir = new File(hadoopRoot)
    val targetDir = new File(targetRoot)
    val packDir = new File(targetRoot + "/pack")
    Process("sbin/stop-yarn.sh", hadoopDir).!
    Process("sbin/start-yarn.sh", hadoopDir).!
    var gearpumpVersion = (Process("ls", targetDir) #| "grep tar.gz").!!
    gearpumpVersion = gearpumpVersion.replace("\n", "").replace(".tar.gz", "")
    println(s"gearpump version: $gearpumpVersion")
    Process(s"bin/yarnclient -version $gearpumpVersion -config conf/yarn.conf", packDir).!
    println("deploy on yarn successfully")
  }

  def getUiAddress: String = {
    val hadoopDir = new File(hadoopRoot)
    val appList = (Process(s"bin/yarn application -list", hadoopDir) #| "grep http").!!
    val startIndex = appList.indexOf("http")
    val endIndex = appList.indexOf("\n", startIndex)
    appList.substring(startIndex, endIndex)
  }

}

