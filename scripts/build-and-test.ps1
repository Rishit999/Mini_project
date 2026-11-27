# EVChargeFlow — Build & Verify Backend + Frontend
$ErrorActionPreference = "Stop"

Write-Host "=== 1. Running Backend Tests (H2 Test Profile) ===" -ForegroundColor Cyan
Push-Location "$PSScriptRoot\..\backend"
mvn test "-Dspring.profiles.active=test"
Pop-Location

Write-Host "=== 2. Running Frontend Biome Lint & Vite Build ===" -ForegroundColor Cyan
Push-Location "$PSScriptRoot\..\frontend"
npm run lint
npm run build
Pop-Location

Write-Host "=== All builds and tests succeeded! ===" -ForegroundColor Green
