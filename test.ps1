$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    & ./build.ps1
    foreach ($briTest in @('bri.ProgrammerAccountTest','clientama.AmateurFlowTest','bri.PlatformFlowTest','clientprog.ClientProgInputTest')) {
        & java '-Dfile.encoding=UTF-8' -cp build/classes $briTest
        if ($LASTEXITCODE -ne 0) { throw "Test failed: $briTest" }
    }
} finally { Pop-Location }
