Param(
    [string]$Server = "127.0.0.1",
    [string]$FileName = "test.txt"
)

$scriptRoot = Split-Path -Parent $PSScriptRoot
$projectRoot = Resolve-Path -Path (Join-Path $scriptRoot "..")

Push-Location $projectRoot
try {
    $cp = "out;libs\mysql-connector-j-8.0.33.jar"
    Write-Host "Running client to download $FileName from $Server..."
    java -cp $cp tools.TestClient $Server $FileName
} finally {
    Pop-Location
}
