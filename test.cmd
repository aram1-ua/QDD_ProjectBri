@echo off
setlocal
call "%~dp0build.cmd"
if errorlevel 1 exit /b 1
pushd "%~dp0"
for %%T in (bri.ProgrammerAccountTest clientama.AmateurFlowTest bri.PlatformFlowTest clientprog.ClientProgInputTest) do (
    java -Dfile.encoding=UTF-8 -cp build/classes %%T
    if errorlevel 1 goto failed
)
popd
exit /b 0
:failed
popd
exit /b 1
