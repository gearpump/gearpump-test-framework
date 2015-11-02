package io.gearpump.test.linux

import java.io.File

import scala.sys.process.{Process, _}

object YarnBuilder extends Builder {

  lazy val sourceRoot = "/root/gearpump-yarn-test/gearpump"
  lazy val hadoopRoot = "/root/hadoop/hadoop-2.7.1"
  lazy val targetRoot = sourceRoot + "/output/target"
  lazy val hdfsRoot = "/user/gearpump/"
  lazy val packDir = new File(targetRoot + "/pack")
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

