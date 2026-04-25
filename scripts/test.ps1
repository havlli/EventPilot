param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $MavenArgs = @("test")
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$LocalRepository = Join-Path $RepoRoot ".m2\repository"

$Mise = (Get-Command mise -ErrorAction SilentlyContinue).Source
if (-not $Mise) {
    $Mise = Join-Path $env:USERPROFILE "scoop\shims\mise.exe"
}

if (-not (Test-Path $Mise)) {
    throw "mise is not available on PATH. Install mise, then open a fresh terminal."
}

& $Mise exec -- mvn "-Dmaven.repo.local=$LocalRepository" -ntp @MavenArgs
