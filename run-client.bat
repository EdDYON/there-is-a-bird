@echo off
setlocal
cd /d "%~dp0"
call gradlew.bat runClient
set "TASK_EXIT=%ERRORLEVEL%"
pause
exit /b %TASK_EXIT%
