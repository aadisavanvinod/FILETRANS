# Run the ServerApp
# Usage: .\scripts\run-server.ps1

param()

# Determine java
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java' }

# Classpath: compiled classes + libs
$binDir = Join-Path $PSScriptRoot '..\bin' | Resolve-Path -Relative
$classpath = "$binDir;libs/*"

Write-Host "Using java: $java"
Write-Host "Classpath: $classpath"

# Run the server GUI
& $java -cp $classpath server.ServerApp
