param(
    [ValidateSet("dev", "preview")]
    [string]$Mode = "dev",
    [switch]$SkipBackend,
    [switch]$NoInstall,
    [switch]$Detached,
    [int]$FrontendPort = 5173,
    [int]$BackendPort = 8080
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$FrontendDir = Join-Path $ProjectRoot "frontend"
$BackendDir = Join-Path $ProjectRoot "backend"
$ScriptsDir = Join-Path $ProjectRoot "scripts"
$LogsDir = Join-Path $ProjectRoot "logs"

New-Item -ItemType Directory -Force -Path $LogsDir | Out-Null

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Write-Warn {
    param([string]$Message)
    Write-Host "WARN: $Message" -ForegroundColor Yellow
}

function Test-Command {
    param([string]$Name)
    return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Test-Port {
    param([int]$Port)
    return $null -ne (Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue)
}

function Start-LoggedProcess {
    param(
        [string]$Name,
        [string]$FilePath,
        [string[]]$ArgumentList,
        [string]$WorkingDirectory,
        [string]$OutFile,
        [string]$ErrFile
    )

    Write-Step "Starting $Name"
    Start-Process `
        -FilePath $FilePath `
        -ArgumentList $ArgumentList `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $OutFile `
        -RedirectStandardError $ErrFile `
        -WindowStyle Hidden
}

Write-Host "ParkVision startup" -ForegroundColor Green
Write-Host "Project: $ProjectRoot"

if (-not (Test-Path $FrontendDir)) {
    throw "frontend directory not found: $FrontendDir"
}

if (-not (Test-Command "node")) {
    throw "Node.js is required. Please install Node.js first."
}

if (-not (Test-Command "npm")) {
    throw "npm is required. Please install npm first."
}

if (-not $NoInstall -and -not (Test-Path (Join-Path $FrontendDir "node_modules"))) {
    Write-Step "Installing frontend dependencies"
    Push-Location $FrontendDir
    npm install
    Pop-Location
}

if (-not $SkipBackend) {
    if (Test-Port $BackendPort) {
        Write-Warn "Backend port $BackendPort is already in use. Skip backend startup."
    } elseif (Test-Command "mvn") {
        $backendOut = Join-Path $LogsDir "backend.out.log"
        $backendErr = Join-Path $LogsDir "backend.err.log"
        Start-LoggedProcess `
            -Name "Spring Boot backend" `
            -FilePath "mvn.cmd" `
            -ArgumentList @("spring-boot:run") `
            -WorkingDirectory $BackendDir `
            -OutFile $backendOut `
            -ErrFile $backendErr
    } else {
        Write-Warn "Maven was not found. Backend will not start. Frontend will use mock fallback data."
        Write-Warn "Install Maven or open backend/pom.xml in IDEA, then run Spring Boot manually."
    }
}

if (Test-Port $FrontendPort) {
    Write-Warn "Frontend port $FrontendPort is already in use. Skip frontend startup."
} else {
    Write-Host ""
    Write-Host "Frontend: http://localhost:$FrontendPort" -ForegroundColor Green
    Write-Host "Keep this terminal open while developing." -ForegroundColor DarkGray

    if ($Mode -eq "dev") {
        if ($Detached) {
            $frontendOut = Join-Path $LogsDir "frontend.out.log"
            $frontendErr = Join-Path $LogsDir "frontend.err.log"
            Start-LoggedProcess `
                -Name "Vue frontend dev server" `
                -FilePath "npm.cmd" `
                -ArgumentList @("run", "dev") `
                -WorkingDirectory $FrontendDir `
                -OutFile $frontendOut `
                -ErrFile $frontendErr
        } else {
            Push-Location $FrontendDir
            npm run dev
            Pop-Location
        }
    } else {
        Write-Step "Building frontend"
        Push-Location $FrontendDir
        npm run build
        Pop-Location

        if ($Detached) {
            $previewOut = Join-Path $LogsDir "frontend-preview.out.log"
            $previewErr = Join-Path $LogsDir "frontend-preview.err.log"
            Start-LoggedProcess `
                -Name "Vue frontend preview server" `
                -FilePath "node.exe" `
                -ArgumentList @((Join-Path $ScriptsDir "serve-frontend.mjs")) `
                -WorkingDirectory $ProjectRoot `
                -OutFile $previewOut `
                -ErrFile $previewErr
        } else {
            Push-Location $ProjectRoot
            node .\scripts\serve-frontend.mjs
            Pop-Location
        }
    }
}

Start-Sleep -Seconds 3

Write-Host ""
Write-Host "ParkVision is starting." -ForegroundColor Green
Write-Host "Frontend: http://localhost:$FrontendPort"
Write-Host "Backend:  http://localhost:$BackendPort"
Write-Host "Logs:     $LogsDir"
Write-Host ""
Write-Host "Useful commands:"
Write-Host "  .\start.ps1                 Start frontend + backend when Maven exists"
Write-Host "  .\start.ps1 -SkipBackend    Start frontend only"
Write-Host "  .\start.ps1 -Mode preview   Build frontend and serve dist"
Write-Host "  .\start.ps1 -Detached       Start frontend in background and write logs"
Write-Host "  .\stop.ps1                  Stop services started on common ports"
