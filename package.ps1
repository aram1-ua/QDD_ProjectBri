param([string]$GroupName = 'QDD_ProjectBri')
$ErrorActionPreference = 'Stop'
if ($GroupName -notmatch '^[\p{L}\p{N}_-]+$') { throw 'Use group member names without spaces or path characters.' }
Push-Location $PSScriptRoot
try {
    if (-not (Test-Path -LiteralPath output/pdf/Presentation_BRi.pdf)) { throw 'The PDF report is missing.' }
    New-Item -ItemType Directory -Force output/submission | Out-Null
    $briArchive = Join-Path $PSScriptRoot "output/submission/$GroupName.zip"
    $briIncluded = @('src','src_client','src_serveur','test','docs','tools','launch','.settings','README.md','sources.txt','build.cmd','test.cmd','build.ps1','test.ps1','package.ps1','.project','.classpath','output/pdf/Presentation_BRi.pdf')
    Add-Type -AssemblyName System.IO.Compression
    $briStream = [System.IO.File]::Create($briArchive)
    $briZip = [System.IO.Compression.ZipArchive]::new($briStream, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($briItem in $briIncluded) {
            $briFiles = if (Test-Path -LiteralPath $briItem -PathType Container) {
                Get-ChildItem -LiteralPath $briItem -Recurse -File
            } else { Get-Item -LiteralPath $briItem }
            foreach ($briFile in $briFiles) {
                $briEntryName = $briFile.FullName.Substring($PSScriptRoot.Length + 1).Replace('\','/')
                $briEntry = $briZip.CreateEntry($briEntryName, [System.IO.Compression.CompressionLevel]::Optimal)
                $briInput = [System.IO.File]::OpenRead($briFile.FullName)
                $briOutput = $briEntry.Open()
                try { $briInput.CopyTo($briOutput) } finally { $briInput.Dispose(); $briOutput.Dispose() }
            }
        }
    } finally { $briZip.Dispose(); $briStream.Dispose() }
    if ((Get-Item -LiteralPath $briArchive).Length -gt 2000000) { throw 'Archive exceeds the 2 MB assignment limit.' }
    Write-Output "Submission: $briArchive ($((Get-Item -LiteralPath $briArchive).Length) bytes)"
} finally { Pop-Location }
