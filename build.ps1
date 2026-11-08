$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force build/classes | Out-Null
    $briSources = Get-ChildItem src_client,src_serveur,test -Recurse -Filter *.java | ForEach-Object { $_.FullName }
    & javac --release 17 -Xlint:all -encoding UTF-8 -d build/classes $briSources
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
} finally { Pop-Location }
