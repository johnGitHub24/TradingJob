# TradingJob — verification entry (portable; Windows)
# Thin: load env then gradlew check. Do not add demo/batch logic here.

. "$PSScriptRoot\env.ps1"
Set-Location (Split-Path $PSScriptRoot -Parent)
if (Test-Path '.\gradlew.bat') {
    .\gradlew.bat check
} elseif (Test-Path '.\gradlew') {
    .\gradlew check
} else {
    Write-Error 'gradlew not found'
}
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'TradingJob check OK' -ForegroundColor Green
