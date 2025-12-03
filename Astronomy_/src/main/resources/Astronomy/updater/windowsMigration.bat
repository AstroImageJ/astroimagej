@echo off
setlocal enabledelayedexpansion

REM wait for starting process to exit
powershell -NoProfile -Command "Wait-Process -Id %~1"

REM remove existing version
if exist "%~3\unins000.exe" (
    start "" /WAIT /B "%~3\unins000.exe"
    REM wait a bit to give time for the UAC to show correctly
    TIMEOUT /T 3 /NOBREAK
) else (
    REM todo
)

REM install update
msiexec /i "%~2" /passive /norestart INSTALLDIR="%~3"

REM open AIJ
start "" /B "%~3\AstroImageJ.exe"

endlocal