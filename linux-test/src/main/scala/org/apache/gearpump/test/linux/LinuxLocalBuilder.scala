package org.apache.gearpump.test.linux

import java.io.File

import scala.sys.process.Process

object LinuxLocalBuilder {

  lazy val sourceRoot = "/root/gearpump-yarn-test/gearpump"
  lazy val targetRoot = sourceRoot + "/output/target"
  lazy val packDir = new File(targetRoot + "/pack")
  private var isInit = false
  private var local: Process = null
  private var services: Process = null


  def init(): Unit = {
    if (!isInit) {
//      Util.buildProject()
      deployGearpump()
      isInit = true
    }
  }

  def deployGearpump(): Unit = {
    println("Deploy Gearpump on Linux local")
    local = Process(s"bin/local", packDir).run()
    services = Process(s"bin/services", packDir).run()
  }

  def stopGearpump(): Unit = {
    println("Stop Gearpump")
    local.destroy()
    services.destroy()
  }

}
