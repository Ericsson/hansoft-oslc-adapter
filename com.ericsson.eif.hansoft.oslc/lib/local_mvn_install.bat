@echo off
rem Install the Hansoft sdk in the local maven repo ($home\.m2)
rem Assume script running from same location as hpmsdk.jar 
rem Assume JAVA_HOME is set 

set mvnExe=C:\oslc_adapter\software\apache-maven-3.1.1\bin\mvn
set groupId=com.ericsson.eif.hansoft.lib
set artifactId=hansoft-lib
set version=1.0

%mvnExe% install:install-file -Dfile=hpmsdk.jar -DgroupId=%groupId% -DartifactId=%artifactId% -Dversion=%version% -Dpackaging=jar