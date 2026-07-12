@echo off
set "JAVA_HOME=D:\MinecraftModWorkspace\jdks\microsoft-jdk-17\jdk-17.0.19+10"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0"
call gradlew.bat build
pause
