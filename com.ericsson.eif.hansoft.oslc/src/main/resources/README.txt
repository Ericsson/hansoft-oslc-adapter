Instructions for (some of) the files in this directory:

adapter.properties: 
-------------------
A copy should be placed in the $ADAPTER_HOME/<webappname> folder and modified 
for the specific deployment.  


log4j.properties:
-----------------
A copy should be placed in the $ADAPTER_HOME/<webappname> folder and modified 
for the specific deployment. Specifically the following properties must be 
configured:

log4j.appender.file.file - set to point to a webapp specific log folder
*.threshold - set the wanted logging level (INFO, DEBUG, WARN, ERROR)


