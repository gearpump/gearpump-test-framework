package org.apache.gearpump.test.linux

import java.io.File

import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import io.gearpump.cluster.AppMasterToMaster.AppMasterSummary
import io.gearpump.cluster.MasterToAppMaster.AppMasterDataDetailRequest
import io.gearpump.cluster.master.MasterProxy
import io.gearpump.streaming.appmaster.StreamAppMasterSummary
import io.gearpump.streaming.executor.Executor.ExecutorSummary
import io.gearpump.util.ActorUtil._
import io.gearpump.util.{Constants, Graph}
import upickle.Js
import upickle.default._

import scala.concurrent.Future
import scala.sys.process.{Process, _}

object Util extends App {
  lazy val httpProxy = "http_proxy" -> "http://child-prc.intel.com:913"
  lazy val httpsProxy = "https_proxy" -> "http://child-prc.intel.com:913"
  lazy val address = "https://github.com/intel-hadoop/gearpump.git"
  lazy val build = "sbt clean assembly packArchive"
  lazy val sourceRoot = "/root/gearpump-yarn-test/gearpump"
  lazy val targetRoot = sourceRoot + "/output/target"
  lazy val packDir = new File(targetRoot + "/pack")
  lazy val master: ActorRef = getMasterActorRef
  lazy val delayTime = 20 * 1000
  lazy val examples = getExamples

  override def main(args: Array[String]): Unit = {

    println("yarn test start!")

    println("stop")
  }

  def buildProject(): Unit = {
    val sourceDir = new File(sourceRoot)
    println(s"git clone $address")
    var result = Process(s"git clone $address $sourceRoot", None, httpProxy, httpsProxy).!
    //if fold already exists, remove it
    if (result == 128 ) {
      println("already exist, remove and download")
      Process(s"rm -rf $sourceRoot").!
      result = Process(s"git clone $address $sourceRoot", None, httpProxy, httpsProxy).!
    }
    println(s"$build")
    val buildResult = Process(build, sourceDir, httpProxy, httpsProxy).!
    println(s"build return: $buildResult")
    //if build failed, what to do?

  }

  def getExamples: Array[String] = {
    Process("ls examples", packDir).lineStream.toArray[String]
  }

  def getEnvironmentVariable(master: (String, Int)): (String, String) = {
    val (host, port) = master
    val masterAddress = host + ":" + port
    //"$JAVA_OPTS -Dgearpump.cluster.masters.0=IDHV22-01:3000 -Dgearpump.hostname=IDHV22-01:3000"
    val javaOpts = s"-Dgearpump.cluster.masters.0=$masterAddress -Dgearpump.hostname=$host"
    val env = "JAVA_OPTS" -> javaOpts
    env
  }

  def submitApp(example: String, options: String, master: (String, Int)): Unit = {
    println("submit application start")
    val env = getEnvironmentVariable(master)
    var submitApp = s"bin/gear app -jar examples/$example"
    if (options != null) {
      submitApp = s"$submitApp $options"
    }

    Process(submitApp, packDir, env).!
    println(s"submit application successfully")
  }

  def getAppInfo(contain: String, master: (String, Int)): String = {
    println("get application info start")
    val env = getEnvironmentVariable(master)
    val appInfo = (Process("bin/gear info", packDir, env) #| s"grep $contain").!!
    appInfo
  }

  def killApp(appId: String, master: (String, Int)): Unit = {
    println("kill application start")
    val env = getEnvironmentVariable(master)
    val killApp = s"bin/gear kill -appid $appId"

    Process(killApp, packDir, env).!
  }

  def getAppId(contain: String, master: (String, Int)): String = {
    val appInfo = Util.getAppInfo(contain, master)
    val start = appInfo.indexOf("application:")
    appInfo.substring(start + 13, appInfo.indexOf(",", start))
  }

  def getAppDetail(appId: String): StreamAppMasterSummary = {
    println("get app detail start")
    implicit val graphReader: upickle.default.Reader[Graph[Int, String]] = upickle.default.Reader[Graph[Int, String]] {
      case Js.Obj(verties, edges) =>
        val vertexList = upickle.default.readJs[List[Int]](verties._2)
        val edgeList = upickle.default.readJs[List[(Int, String, Int)]](edges._2)
        Graph(vertexList, edgeList)
    }
    val uiAddress = YarnBuilder.getUiAddress
    val appDetailString = Process(s"curl $uiAddress/api/v1.0/appmaster/$appId?detail=true").!!
    read[StreamAppMasterSummary](appDetailString)
  }

  def getExecutor(appId: String, executorId: String): ExecutorSummary = {
    val uiAddress = YarnBuilder.getUiAddress
    val executorString = Process(s"curl $uiAddress/api/v1.0/appmaster/$appId/executor/$executorId").!!
    read[ExecutorSummary](executorString)
  }

  def killProcess(processId: String): Unit = {
    Process(s"kill $processId").!
  }

  def isRunning(appId: String): Boolean = {
    val appDetail = getAppDetail(appId)
    val currentTime = System.currentTimeMillis()
    val appClock = appDetail.clock
    val delay = Math.abs(currentTime - appClock)
    println(s"delay: $delay")
    var running = false
    if (Math.abs(currentTime - appClock) < delayTime) {
      running = true
    }
    running
  }

  def getMasterActorRef: ActorRef = {
    import scala.collection.JavaConverters._
    val hostPort = YarnBuilder.getMaster
    val masters = List(s"${hostPort._1}:${hostPort._2}").asJava
    val config = ConfigFactory.load()
      .withValue(Constants.GEARPUMP_CLUSTER_MASTERS, ConfigValueFactory.fromAnyRef(masters))
      .withValue(Constants.GEARPUMP_HOSTNAME, ConfigValueFactory.fromAnyRef(hostPort._1))
      .withValue("akka.actor.provider", ConfigValueFactory.fromAnyRef("akka.remote.RemoteActorRefProvider"))

    import scala.collection.JavaConversions._
    val masterCluster = config.getStringList(Constants.GEARPUMP_CLUSTER_MASTERS).toList.flatMap(io.gearpump.util.Util.parseHostList)
    val system = ActorSystem("master", config)
    import scala.concurrent.duration._
    system.actorOf(MasterProxy.props(masterCluster, 1 day), s"masterproxy${system.name}")
  }

  def getDetail(appId: Int): Future[AppMasterSummary] = {
    //    implicit val ec: ExecutionContext = ExecutionContext.global
    import scala.concurrent.ExecutionContext.Implicits.global
//    val master = getMasterActorRef
    val request = AppMasterDataDetailRequest(appId)
    println("get app detail")
    askAppMaster[AppMasterSummary](master, appId, request)
  }

}
