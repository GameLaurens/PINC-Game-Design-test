@echo off
setlocal
pushd "%~dp0"
if not exist "target\terminal-sprite-lab.jar" (
    call mvn -B "-Dmaven.repo.local=%~dp0.cache\maven" package
    if errorlevel 1 goto :failed
)
java --enable-native-access=ALL-UNNAMED -jar "target\terminal-sprite-lab.jar" %*
set "labExit=%errorlevel%"
popd
exit /b %labExit%
:failed
popd
exit /b 1
