@echo off
setlocal enabledelayedexpansion

REM wait for starting process to exit
powershell -NoProfile -Command "Wait-Process -Id %~1"

REM install update
msiexec /i "%~2" /passive /norestart INSTALLDIR="%~3"

REM open AIJ
start "" /B "%~3\AstroImageJ.exe"

endlocal