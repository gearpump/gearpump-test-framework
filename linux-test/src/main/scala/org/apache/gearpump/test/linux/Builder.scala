package org.apache.gearpump.test.linux

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
