[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$mavenWrapper = Join-Path $projectRoot 'mvnw.cmd'
$installedMaven = Get-Command 'mvn.cmd' -ErrorAction SilentlyContinue

if ($null -ne $installedMaven) {
    $mavenCommand = $installedMaven.Source
} elseif (Test-Path -LiteralPath $mavenWrapper) {
    $mavenCommand = $mavenWrapper
} else {
    throw 'Neither mvn.cmd nor the Maven wrapper is available.'
}

if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME)) {
    $env:DB_USERNAME = 'root'
}

Write-Host 'Running the isolated CodeMate Agent demonstration...'
Write-Host 'The test uses DB_USERNAME/DB_PASSWORD, calls no paid model, and removes all demo rows.'

Push-Location $projectRoot
try {
    & $mavenCommand -pl paicoding-web -am '-Dtest=CodeMateDemoScenarioTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
    if ($LASTEXITCODE -ne 0) {
        throw "CodeMate demonstration failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

Write-Host 'CodeMate Agent demonstration passed.' -ForegroundColor Green
