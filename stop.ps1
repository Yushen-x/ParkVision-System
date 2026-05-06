param(
    [int[]]$Ports = @(5173, 8080)
)

$ErrorActionPreference = "Continue"

foreach ($Port in $Ports) {
    $lines = netstat -ano | Select-String ":$Port\s"
    $processIds = @()

    foreach ($line in $lines) {
        $parts = ($line.ToString() -split "\s+") | Where-Object { $_ -ne "" }
        if ($parts.Count -ge 5 -and $parts[3] -eq "LISTENING") {
            $processIds += [int]$parts[4]
        }
    }

    $processIds = $processIds | Sort-Object -Unique

    if ($processIds.Count -eq 0) {
        Write-Host "No listening process found on port $Port"
        continue
    }

    foreach ($processId in $processIds) {
        Write-Host "Stopping process $processId on port $Port"
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "Done."
