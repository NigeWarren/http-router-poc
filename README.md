# http-router-poc

Proof of Concept - HTTP Router Server and Test Client for Mewbase

This is protoype Scala code which is intended to be integrated into the Mewbase project repo https://github.com/Tesco/mewbase 
once various protocol and resource management issues have been ironed out.

It is licensed under the [mewbase license]( https://github.com/Tesco/mewbase/blob/master/LICENSE.txt )

It produces a runnable jar that contains the server and test client code.

The runnable jar is here ...

https://github.com/NigeWarren/http-router-poc/blob/master/target/scala-2.12/http-router-POC-assembly-0.0.1.jar


### Building 

If you want to create a runnable jar from the command line (it will be necessary to install sbt if not 
already installed on your machine.) ... 

`$> sbt assembly`

This will pack all the source code and dependencies into a jar file named  http-router-POC-assembly-X.X.X.jar see the 
output from the 'sbt assembly' command for the location of the file.

### Running 

To run the server on a machine on which a [JDK]( http://www.oracle.com/technetwork/java/javase/downloads/index.html ) is installed type ...

`$> java -cp {path to jar}/http-router-POC-assembly-0.0.1.jar io.mewbase.Server`

This will output :

`Server Started : 8200`

It is possible to ping the server with an HTTP GET command from any browser or client eg typing

`http://localhost:8200/ping`

In a browser local to the server will result in the 'pong' text being displayed.

The jar also contains a test client that can be run to test publish and subscribe protocols to the server - example

`java -cp ./http-router-POC-assembly-0.0.1.jar io.mewbase.Client`

Will produce output reflecting replies from the server for various calls and the server will report publish 
and subscribe details.

