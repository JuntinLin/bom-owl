rem C:\Program Files\Java\jdk-21.0.5\bin
rem "C:\Program Files\Java\jdk-21.0.5\bin\java" -jar target\owl-0.0.1-SNAPSHOT.jar
@echo off
REM Create heap dumps directory if it doesn't exist
if not exist "heap_dumps" mkdir heap_dumps

"C:\Program Files\Java\jdk-21.0.5\bin\java"  ^
	-Xms2048m ^
	-Xmx8196m ^
	-XX:MaxMetaspaceSize=256m ^
	-XX:+UseG1GC ^
	-XX:MaxGCPauseMillis=200 ^
  	-XX:+HeapDumpOnOutOfMemoryError ^
  	-XX:HeapDumpPath=./heap_dumps/ ^
  	-Djena.mem.dataset.table=50 ^
  	-jar target\owl-0.0.1-SNAPSHOT.jar