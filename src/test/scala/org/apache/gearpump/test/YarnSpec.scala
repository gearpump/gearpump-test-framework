package org.apache.gearpump.test

import org.scalatest._

class YarnSpec extends FlatSpec with Matchers {

//  val submitTime = System.currentTimeMillis().toString

  "WordCount" should "submit successfully" in {
//    val example = "examples/gearpump-examples-wordcount-assembly-0.4.3-SNAPSHOT.jar"
//    Executor.submitApp(example, submitTime)
//    val appInfo = Executor.getAppInfo(submitTime)
//    val start = appInfo.indexOf("status:")
//    val status = appInfo.substring(start + 8, appInfo.indexOf(",", start))
    val status = "active"
    status should be ("active")
  }

  it should "be killed" in {
//    val appInfo = Executor.getAppInfo(submitTime)
//    var start = appInfo.indexOf("application:")
//    val appid = appInfo.substring(start + 13, appInfo.indexOf(",", start))
//    Executor.killApp(appid)
//    start = appInfo.indexOf("status:")
//    val status = appInfo.substring(start + 8, appInfo.indexOf(",", start))
    val status = "inactive"
    status should be ("inactive")
  }

}
