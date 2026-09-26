@echo off
setlocal
cd /d "%~dp0"
call gradlew.bat build
set "TASK_EXIT=%ERRORLEVEL%"
pause
exit /b %TASK_EXIT%
