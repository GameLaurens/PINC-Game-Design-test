param(
    [string]$Png,
    [ValidateSet(8,16,32)][int]$Size = 16,
    [switch]$Build,
    [switch]$CheckBuild
)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    & ([scriptblock]::Create([IO.File]::ReadAllText('tools/prepare-run.ps1'))) -ForceBuild:$Build
    $launchJar = [IO.File]::ReadAllText((Join-Path $PSScriptRoot '.cache/current-run.txt'))
    $appArgs = @('--size', "$Size")
    if ($Png) { $appArgs += @('--png', $Png) }
    if ($CheckBuild) { $appArgs = @('--check-build') }
    & java --enable-native-access=ALL-UNNAMED -jar $launchJar @appArgs
    if ($LASTEXITCODE -ne 0) { throw "Demo gestopt met exitcode $LASTEXITCODE." }
} finally { Pop-Location }
