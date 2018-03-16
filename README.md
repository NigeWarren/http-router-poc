# http-router-poc

Proof of Concept - HTTP Router Server and Test Client for Mewbase

This is protoype Scala code which is intended to be integrated into the Mewbase project repo https://github.com/Tesco/mewbase 
once various protocol and resource management issues have been ironed out.

It produces a runnable jar that contains the server and test client code.

The runnable jar is here ...

https://github.com/NigeWarren/http-router-poc/blob/master/target/scala-2.12/http-router-POC-assembly-0.0.1.jar


### Building 

If you want to create a runnable jar from the command line use ... (it will be necessary to install sbt if not 
already installed on your machine.

$> sbt assembly

This will pack all the source code and dependencies into a jar file named  http-router-POC-assembly-<version>.jar see the 
output from the 'sbt assembly' command for the location of the file.

### Running 

