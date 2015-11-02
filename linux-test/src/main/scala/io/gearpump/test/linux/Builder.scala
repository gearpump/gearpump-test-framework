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

import io.gearpump.cluster.AppMasterToMaster.MasterData
import io.gearpump.cluster.master.MasterSummary
import upickle.default._
import scala.sys.process.Process

trait Builder {

  def init()

  def getUiAddress: String

  def getMaster: (String, Int) = {
    val uiAddress = getUiAddress
    val masterString = Process(s"curl $uiAddress/api/v1.0/master").!!
    val master: MasterSummary = read[MasterData](masterString).masterDescription
    val host = master.leader._1.substring(master.leader._1.indexOf("@") + 1)
    val port = master.leader._2
    (host, port)
  }

}
