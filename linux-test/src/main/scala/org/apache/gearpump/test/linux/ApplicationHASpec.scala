package org.apache.gearpump.test.linux

import org.apache.gearpump.test.linux.Util._
import org.scalatest._

class ApplicationHASpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  lazy val appName = "app" + System.currentTimeMillis()
  lazy val timeForRunning = 60 * 1000
  lazy val master = YarnBuilder.getMaster

  override def beforeAll() = {
    ApplicationHASpec.deployOnYarn()
  }

  "Application HA test: Example" should "submit successfully" in {
    val example = Util.examples(5)
    val options = s"-namePrefix $appName -split 100 -sum 100"
    submitApp(example, options, master)
    val appInfo = getAppInfo(appName, master)
    val start = appInfo.indexOf("status:")
    val status = appInfo.substring(start + 8, appInfo.indexOf(",", start))
    status should be ("active")
  }

  it should "be running" in {
    Thread.sleep(timeForRunning)
    val appId = getAppId(appName, master)
    val running = isRunning(appId)
    running should be (right = true)
  }

  it should "recover after killing an executor" in {
    val appId = getAppId(appName, master)
    val executorJvmName = getExecutor(appId,"0").jvmName
    val processorId = executorJvmName.substring(0, executorJvmName.indexOf("@"))
    println(s"process id: $processorId")
    killProcess(processorId)
    Thread.sleep(timeForRunning)
    val running = isRunning(appId)
    running should be (right = true)
  }

  it should "recover after killing appMaster" in {
    val appId = getAppId(appName, master)
    val executorJvmName = getExecutor(appId,"-1").jvmName
    val processorId = executorJvmName.substring(0, executorJvmName.indexOf("@"))
    println(s"process id: $processorId")
    killProcess(processorId)
    Thread.sleep(timeForRunning)
    val running = isRunning(appId)
    running should be (right = true)
  }

  it should "be killed" in {
    val appId = getAppId(appName, master)
    killApp(appId, master)
    val appInfo = getAppInfo(appName, master)
    val start = appInfo.indexOf("status:")
    val status = appInfo.substring(start + 8, appInfo.indexOf(",", start))
    status should be ("inactive")
  }

}

object ApplicationHASpec extends App {

  def deployOnYarn(): Unit = {
    import YarnBuilder._
    init()
    Thread.sleep(5000)
  }

  val wordcount = "wordcount-2.11.5-0.6.2-SNAPSHOT-assembly.jar"

}
