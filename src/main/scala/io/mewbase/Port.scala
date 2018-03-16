package io.mewbase

trait Port {

  def getPortNumber : Int =  System.getProperty("mewbase.port") match {
    case null => 8080
    case port  => port.toInt
  }


}
