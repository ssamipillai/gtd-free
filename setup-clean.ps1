# GTD-Free Environment Setup Script
Write-Host "Setting up GTD-Free development environment..." -ForegroundColor Green

# Find Java installation
$javaHome = $null
$possibleJavaPaths = @(
    "${env:ProgramFiles}\Java\jdk*",
    "${env:ProgramFiles}\Eclipse Adoptium\jdk*",
    "${env:ProgramFiles}\Microsoft\jdk*",
    "${env:ProgramFiles(x86)}\Java\jdk*"
)

foreach ($path in $possibleJavaPaths) {
    $found = Get-ChildItem -Path $path -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
    if ($found) {
        $javaHome = $found.FullName
        break
    }
}

if (-not $javaHome) {
    try {
        $javaExe = (Get-Command java -ErrorAction Stop).Source
        $javaHome = Split-Path (Split-Path $javaExe)
    } catch {
        Write-Warning "Java installation not found automatically."
        $javaHome = Read-Host "Enter JAVA_HOME path"
    }
}

# Set environment variables
$env:JAVA_HOME = $javaHome
$env:ANT_HOME = "C:\Tools\apache-ant-1.10.15"
$env:MAVEN_HOME = "C:\Tools\apache-maven-3.9.11"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANT_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"

# Display settings
Write-Host "`nEnvironment Variables Set:" -ForegroundColor Yellow
Write-Host "JAVA_HOME  = $env:JAVA_HOME" -ForegroundColor Cyan
Write-Host "ANT_HOME   = $env:ANT_HOME" -ForegroundColor Cyan  
Write-Host "MAVEN_HOME = $env:MAVEN_HOME" -ForegroundColor Cyan

# Verify installations
Write-Host "`nVerifying installations..." -ForegroundColor Yellow

try {
    $javaVersion = & java -version 2>&1 | Select-Object -First 1
    Write-Host "Java: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "Java: Not found or not working" -ForegroundColor Red
}

try {
    $antVersion = & ant -version 2>&1 | Select-Object -First 1
    Write-Host "Ant: $antVersion" -ForegroundColor Green
} catch {
    Write-Host "Ant: Not found or not working" -ForegroundColor Red
}

try {
    $mavenVersion = & mvn -version 2>&1 | Select-Object -First 1
    Write-Host "Maven: $mavenVersion" -ForegroundColor Green
} catch {
    Write-Host "Maven: Not found or not working" -ForegroundColor Red
}

Write-Host "`nSetup complete!" -ForegroundColor Green