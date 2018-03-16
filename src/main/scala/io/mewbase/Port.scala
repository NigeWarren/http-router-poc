package io.mewbase

trait Port {

  def getPortNumber : Int =  System.getProperty("mewbase.port") match {
    case null => 8080
    case port  => port.toInt
  }

  def getHostName : String = System.getProperty("mewbase.host") match {
    case null => "localhost"
    case host => host
  }


}
