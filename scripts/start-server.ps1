Param(
    [string]$FilePath,
    [int]$Port = 5000
)

# Resolve project root (one level up from scripts folder)
$scriptRoot = Split-Path -Parent $PSScriptRoot
$projectRoot = Resolve-Path -Path (Join-Path $scriptRoot "..")

if (-not $FilePath) {
    Write-Host "Usage: .\start-server.ps1 -FilePath <path-to-file> [-Port <port>]"
    exit 1
}

$absFile = Resolve-Path -Path $FilePath -ErrorAction Stop

Push-Location $projectRoot
try {
    $cp = "out;libs\mysql-connector-j-8.0.33.jar"
    Write-Host "Starting server as background job serving $absFile on port $Port..."
    Start-Job -ScriptBlock {
        param($cpParam, $fileParam)
        Set-Location $using:projectRoot
        java -cp $cpParam server.ServerApp $fileParam
    } -ArgumentList $cp, $absFile -Name FileServerJob | Out-Null
    Write-Host "Server background job started (Name: FileServerJob). Use Get-Job/Stop-Job to manage."
} finally {
    Pop-Location
}
