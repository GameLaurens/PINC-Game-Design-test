param([switch]$ForceBuild)
$ErrorActionPreference = 'Stop'

# Both launchers enter the repository first. No execution-policy change is needed:
# run.cmd evaluates this local file with PowerShell's inline -Command mode.
$projectPath = (Get-Location).Path
$jarPath = Join-Path $projectPath 'target/terminal-sprite-lab.jar'
$cachePath = Join-Path $projectPath '.cache/run'
$javaPath = (Get-Command java -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source

function Invoke-LabBuild([switch]$Clean) {
    # Prefer the JDK that supplies the java command over a stale JAVA_HOME.
    $jdkPath = Split-Path -Parent (Split-Path -Parent $javaPath)
    if (Test-Path -LiteralPath (Join-Path $jdkPath 'bin/javac.exe')) { $env:JAVA_HOME = $jdkPath }
    $goals = @('package')
    if ($Clean) {
        $buildPath = [IO.Path]::GetFullPath((Join-Path $projectPath 'target'))
        if ($buildPath -ne ($projectPath.TrimEnd('\') + '\target')) { throw 'Ongeldig bouwpad.' }
        $goals = @('clean', 'package')
    }
    & mvn -B "-Dmaven.repo.local=$projectPath/.cache/maven" @goals
    if ($LASTEXITCODE -ne 0) { throw 'Bouwen mislukt; de game is niet gestart.' }
}

function Get-LabSnapshot {
    New-Item -ItemType Directory -Path $cachePath -Force | Out-Null
    $hash = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash.ToLowerInvariant()
    $snapshot = Join-Path $cachePath "terminal-sprite-lab-$hash.jar"
    if (!(Test-Path -LiteralPath $snapshot) -or (Get-FileHash -LiteralPath $snapshot -Algorithm SHA256).Hash.ToLowerInvariant() -ne $hash) {
        Copy-Item -LiteralPath $jarPath -Destination $snapshot -Force
    }
    if ((Get-FileHash -LiteralPath $snapshot -Algorithm SHA256).Hash.ToLowerInvariant() -ne $hash) {
        throw 'De build wijzigde tijdens het voorbereiden. Start run.cmd opnieuw na het bouwen.'
    }
    return $snapshot
}

function Test-LabSnapshot([string]$Snapshot) {
    $info = New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName = $javaPath
    $info.Arguments = '--enable-native-access=ALL-UNNAMED -jar "' + $Snapshot + '" --check-build'
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $process = [Diagnostics.Process]::Start($info)
    try {
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) {
            $reason = ($stderr.Result -split '\r?\n' | Select-Object -First 1)
            Write-Host "Buildcontrole mislukt: $reason"
            return $false
        }
        return $true
    } finally { $process.Dispose() }
}

$needsBuild = $ForceBuild -or !(Test-Path -LiteralPath $jarPath)
if (!$needsBuild) {
    $jarFile = Get-Item -LiteralPath $jarPath
    $buildInputs = @(Get-Item -LiteralPath 'pom.xml') + @(Get-ChildItem -LiteralPath 'src' -Recurse -File)
    $needsBuild = @($buildInputs | Where-Object { $_.LastWriteTimeUtc -gt $jarFile.LastWriteTimeUtc }).Count -gt 0
}
if ($needsBuild) {
    Write-Host 'Bronbestanden gewijzigd of nog geen build gevonden. Applicatie wordt gebouwd...'
    Invoke-LabBuild
}
$snapshot = Get-LabSnapshot
if (!(Test-LabSnapshot $snapshot)) {
    Write-Host 'Onvolledige of ongeldige build gevonden. De applicatie wordt volledig opnieuw gebouwd...'
    Invoke-LabBuild -Clean
    $snapshot = Get-LabSnapshot
    if (!(Test-LabSnapshot $snapshot)) { throw 'Buildcontrole mislukt; de game is niet gestart.' }
}
# An ASCII relative path also works with cmd.exe's active code page in Unicode user folders.
$relativeSnapshot = Join-Path '.cache/run' ([IO.Path]::GetFileName($snapshot))
[IO.File]::WriteAllText((Join-Path $projectPath '.cache/current-run.txt'), $relativeSnapshot, [Text.UTF8Encoding]::new($false))
