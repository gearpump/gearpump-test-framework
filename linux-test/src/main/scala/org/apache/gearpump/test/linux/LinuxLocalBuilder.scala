package org.apache.gearpump.test.linux

import java.io.File

import scala.collection.mutable.ArrayBuffer
import scala.sys.process.{ProcessIO, Process}

object LinuxLocalBuilder extends Builder{

  lazy val sourceRoot = "/root/gearpump-yarn-test/gearpump"
  lazy val targetRoot = sourceRoot + "/output/target"
  lazy val packDir = new File(targetRoot + "/pack")
  private var isInit = false
  private var local: Process = null
  private var services: Process = null
  val lines = ArrayBuffer[String]()
  val serviceIO = new ProcessIO(_ => ()
    , stdout => scala.io.Source.fromInputStream(stdout).getLines().foreach({line =>
      lines += line
      println(line)})
    , _ => ())


  def init(): Unit = {
    if (!isInit) {
      Util.buildProject()
      deployGearpump()
      isInit = true
    }
  }

  def deployGearpump(): Unit = {
    println("Deploy Gearpump on Linux local")
    local = Process(s"bin/local", packDir).run()
    services = Process(s"bin/services", packDir).run(serviceIO)
  }

  def stopGearpump(): Unit = {
    println("Stop Gearpump")
    local.destroy()
    services.destroy()
  }

  def getUiAddress: String = {
    val line = lines.head
    val start = line.indexOf("http")
    val end = line.indexOf(" ", start)
    line.substring(start, end)
  }

}
