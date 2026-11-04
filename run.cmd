@echo off
setlocal
pushd "%~dp0"
rem A previous build may predate source/resource changes or a git pull.
rem Inline -Command works with the default PowerShell execution policy too.
powershell.exe -NoProfile -Command "$jar = Get-Item -LiteralPath 'target/terminal-sprite-lab.jar' -ErrorAction SilentlyContinue; if ($null -eq $jar) { exit 1 }; $inputs = @(Get-Item -LiteralPath 'pom.xml') + @(Get-ChildItem -LiteralPath 'src' -Recurse -File); if (@($inputs | Where-Object { $_.LastWriteTimeUtc -gt $jar.LastWriteTimeUtc }).Count -gt 0) { exit 1 }; exit 0"
if errorlevel 1 (
    echo Bronbestanden gewijzigd of nog geen build gevonden. Applicatie wordt gebouwd...
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
