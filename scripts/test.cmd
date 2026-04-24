@echo off
setlocal
set REPO_ROOT=%~dp0..

set MISE=mise
where %MISE% >nul 2>nul
if errorlevel 1 (
    set MISE=%USERPROFILE%\scoop\shims\mise.exe
    if not exist "%USERPROFILE%\scoop\shims\mise.exe" (
        echo mise is not available on PATH. Install mise, then open a fresh terminal.
        exit /b 1
    )
)

if "%~1"=="" (
    "%MISE%" exec -- mvn "-Dmaven.repo.local=%REPO_ROOT%\.m2\repository" -ntp test
) else (
    "%MISE%" exec -- mvn "-Dmaven.repo.local=%REPO_ROOT%\.m2\repository" -ntp %*
)
