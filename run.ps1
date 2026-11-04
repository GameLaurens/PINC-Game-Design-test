param(
    [string]$Png,
    [ValidateSet(8,16,32)][int]$Size = 16,
    [switch]$Build
)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $needsBuild = $Build -or !(Test-Path -LiteralPath 'target/terminal-sprite-lab.jar')
    if (!$needsBuild) {
        $jarFile = Get-Item -LiteralPath 'target/terminal-sprite-lab.jar'
        $buildInputs = @(Get-Item -LiteralPath 'pom.xml') + @(Get-ChildItem -LiteralPath 'src' -Recurse -File)
        $needsBuild = @($buildInputs | Where-Object { $_.LastWriteTimeUtc -gt $jarFile.LastWriteTimeUtc }).Count -gt 0
    }
    if ($needsBuild) {
        & mvn -B "-Dmaven.repo.local=$PSScriptRoot/.cache/maven" package
        if ($LASTEXITCODE -ne 0) { throw 'Bouwen mislukt.' }
    }
    $appArgs = @('--size', "$Size")
    if ($Png) { $appArgs += @('--png', $Png) }
    & java --enable-native-access=ALL-UNNAMED -jar target/terminal-sprite-lab.jar @appArgs
    if ($LASTEXITCODE -ne 0) { throw "Demo gestopt met exitcode $LASTEXITCODE." }
} finally { Pop-Location }
