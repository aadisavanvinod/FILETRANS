# Compile all Java sources into ./bin targeting Java 21
# Usage: .\scripts\compile.ps1

param()

# Determine javac
$javac = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\javac.exe' } else { 'javac' }

Write-Host "Using javac: $javac"

# Ensure bin directory
$binDir = Join-Path $PSScriptRoot '..\bin' | Resolve-Path -Relative
New-Item -ItemType Directory -Force -Path $binDir | Out-Null

# Collect source files from client, common, server
$sources = Get-ChildItem -Path (Join-Path $PSScriptRoot '..\client'), (Join-Path $PSScriptRoot '..\common'), (Join-Path $PSScriptRoot '..\server') -Recurse -Filter *.java -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
if (-not $sources) {
    Write-Error "No Java source files found under client/, common/, server/"
    exit 1
}

# Compile with --release 21 to target Java 21 (works when using JDK 9+)
try {
    & $javac --release 21 -d $binDir -cp "libs/*" @($sources)
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Compilation succeeded. Classes are in: $binDir"
    } else {
        Write-Error "javac exited with code $LASTEXITCODE"
        exit $LASTEXITCODE
    }
} catch {
    Write-Error "Failed to run javac: $_"
    exit 1
}
