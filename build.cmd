@echo off
setlocal
pushd "%~dp0"
if not exist "build\classes" mkdir "build\classes"
if errorlevel 1 goto failed
javac --release 17 -Xlint:all -encoding UTF-8 -d build/classes "@sources.txt"
if errorlevel 1 goto failed
popd
exit /b 0
:failed
popd
exit /b 1
