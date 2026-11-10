@echo off
setlocal
pushd "%~dp0"
rem Validate the packaged demos and freeze this session's archive before launching.
rem Inline -Command also works when running .ps1 files is blocked by execution policy.
powershell.exe -NoProfile -Command "try { & ([scriptblock]::Create([IO.File]::ReadAllText('tools/prepare-run.ps1'))) } catch { Write-Error $_; exit 1 }"
if errorlevel 1 goto :failed
set /p "labJar="<".cache\current-run.txt"
java --enable-native-access=ALL-UNNAMED -jar "%labJar%" %*
set "labExit=%errorlevel%"
popd
exit /b %labExit%
:failed
popd
exit /b 1
