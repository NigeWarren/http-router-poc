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
already installed on your machine.)

`$> sbt assembly`

This will pack all the source code and dependencies into a jar file named  http-router-POC-assembly-X.X.X.jar see the 
output from the 'sbt assembly' command for the location of the file.


### Running The Server

To run the server you will need a [JDK]( http://www.oracle.com/technetwork/java/javase/downloads/index.html ) installed.

As an example to start the serve on port 32080 type

`$> java -Dmewbase.port=32080 -cp {path to jar}/http-router-POC-assembly-0.0.1.jar io.mewbase.Server`

This will output

`Server Started on port 32080`

It is possible to ping the server with an HTTP GET command from any browser or client - e.g.

`http://localhost:32080/ping`

In a browser local to the server will result in the 'pong' text being displayed.

If you dont specify a port the systems properties the server will start on 8080 by default.


### Running The Client

The jar also contains a test client that can be run to test publish and subscribe protocols to the server - e.g.

`java -Dmewbase.host=127.0.0.1 -Dmewbase.port=32080 -cp ./http-router-POC-assembly-0.0.1.jar io.mewbase.Client`

Will start the test client which tries to connect to the server on the given host and port. If it connects
this will produce output reflecting replies from the server for various calls and the server will report publish
and subscribe details.

