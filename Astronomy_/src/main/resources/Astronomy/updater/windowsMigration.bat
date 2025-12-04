@echo off
setlocal enabledelayedexpansion

REM wait for starting process to exit
powershell -NoProfile -Command "Wait-Process -Id %~1"

REM remove existing version
if exist "%~3\unins000.exe" (
    start "" /WAIT /B "%~3\unins000.exe"
    REM loop waiting for uninstaller to exit with timeout
    set timeout=0
    :wait_loop
    if exist "%~3\unins000.exe" (
        TIMEOUT /T 1 /NOBREAK >nul
        set /a timeout+=1
        if !timeout! lss 120 goto wait_loop
    )
    TIMEOUT /T 5 /NOBREAK
) else (
    REM todo
)

REM install update
msiexec /i "%~2" /passive /norestart INSTALLDIR="%~3"

REM open AIJ
start "" /B "%~3\AstroImageJ.exe"

endlocal