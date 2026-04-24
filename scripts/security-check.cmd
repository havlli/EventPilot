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

set FAIL_BUILD_ON_CVSS=7
if not "%~1"=="" set FAIL_BUILD_ON_CVSS=%~1

"%MISE%" exec -- mvn "-Dmaven.repo.local=%REPO_ROOT%\.m2\repository" -ntp org.owasp:dependency-check-maven:12.2.1:check -Dformat=HTML -Dformat=JSON -DfailBuildOnCVSS=%FAIL_BUILD_ON_CVSS%
