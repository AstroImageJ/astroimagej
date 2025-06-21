@echo off

REM wait for starting process to exit
powershell -NoProfile -Command "Wait-Process -Id %~1"

REM install update
msiexec /i "%~2" /passive /norestart

REM cleanup temp file

REM open AIJ