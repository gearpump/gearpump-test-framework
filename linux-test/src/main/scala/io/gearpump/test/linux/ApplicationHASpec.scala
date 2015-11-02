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

import Util._
import org.scalatest._

class ApplicationHASpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  lazy val appName = "app" + System.currentTimeMillis()
  lazy val timeForRunning = 60 * 1000
  lazy val master = YarnBuilder.getMaster
  lazy val uiAddress = YarnBuilder.getUiAddress

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
    val running = isRunning(appId, uiAddress)
    running should be (right = true)
  }

  it should "recover after killing an executor" in {
    val appId = getAppId(appName, master)
    val executorJvmName = getExecutor(appId,"0",uiAddress).jvmName
    val processorId = executorJvmName.substring(0, executorJvmName.indexOf("@"))
    println(s"process id: $processorId")
    killProcess(processorId)
    Thread.sleep(timeForRunning)
    val running = isRunning(appId, uiAddress)
    running should be (right = true)
  }

  it should "recover after killing appMaster" in {
    val appId = getAppId(appName, master)
    val executorJvmName = getExecutor(appId,"-1",uiAddress).jvmName
    val processorId = executorJvmName.substring(0, executorJvmName.indexOf("@"))
    println(s"process id: $processorId")
    killProcess(processorId)
    Thread.sleep(timeForRunning)
    val running = isRunning(appId, uiAddress)
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
