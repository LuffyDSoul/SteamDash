@echo off
cd /d %~dp0
"C:\Program Files\OpenLogic\jdk-21.0.8.9-hotspot\bin\java.exe" -Duser.timezone=UTC -jar target\dacs-backend-0.0.1-SNAPSHOT.jar
