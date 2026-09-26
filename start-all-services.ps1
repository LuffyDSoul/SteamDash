# Script para iniciar todos los microservicios DACS
# Autor: Sistema automatizado
# Fecha: $(Get-Date -Format "yyyy-MM-dd")

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  Iniciando todos los microservicios" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Función para verificar si un puerto está en uso
function Test-Port {
    param([int]$Port)
    try {
        $connection = New-Object System.Net.Sockets.TcpClient
        $connection.Connect("localhost", $Port)
        $connection.Close()
        return $true
    }
    catch {
        return $false
    }
}

# Verificar puertos antes de iniciar
$ports = @{9001="BFF"; 9002="Conector"; 9003="Backend"}
$portsInUse = @()

foreach ($port in $ports.Keys) {
    if (Test-Port $port) {
        $portsInUse += "$port ($($ports[$port]))"
    }
}

if ($portsInUse.Count -gt 0) {
    Write-Host "¡ADVERTENCIA! Los siguientes puertos ya están en uso:" -ForegroundColor Yellow
    foreach ($portInfo in $portsInUse) {
        Write-Host "  - Puerto $portInfo" -ForegroundColor Yellow
    }
    Write-Host ""
    $continue = Read-Host "¿Desea continuar de todos modos? (s/N)"
    if ($continue -ne "s" -and $continue -ne "S") {
        Write-Host "Operación cancelada." -ForegroundColor Red
        exit 1
    }
}

# Array para almacenar los procesos
$global:processes = @()

# Función para iniciar un microservicio
function Start-Microservice {
    param(
        [string]$Name,
        [string]$Path,
        [int]$Port,
        [string]$JvmArgs = ""
    )
    
    Write-Host "[$((Get-Date).ToString("HH:mm:ss"))] Iniciando $Name en puerto $Port..." -ForegroundColor Green
    
    # Construir el comando Maven con argumentos JVM opcionales
    $mavenCommand = "mvn spring-boot:run"
    if (-not [string]::IsNullOrEmpty($JvmArgs)) {
        $mavenCommand += " -Dspring-boot.run.jvmArguments=`"$JvmArgs`""
        Write-Host "  Con argumentos JVM: $JvmArgs" -ForegroundColor Yellow
    }
    
    try {
        $process = Start-Process -FilePath "cmd.exe" `
                                -ArgumentList "/c", "cd /d `"$Path`" && $mavenCommand" `
                                -WindowStyle Normal `
                                -PassThru
        
        $global:processes += @{
            Name = $Name
            Process = $process
            Port = $Port
            Path = $Path
        }
        
        Write-Host "✓ $Name iniciado correctamente (PID: $($process.Id))" -ForegroundColor Green
        return $true
    }
    catch {
        Write-Host "✗ Error al iniciar $Name`: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Obtener la ruta base del script
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path

# Iniciar los microservicios en orden
$services = @(
    @{Name="DACS-Conector"; Path="$scriptPath\dacs-conector"; Port=9002; JvmArgs=""},
    @{Name="DACS-Backend"; Path="$scriptPath\dacs-backend"; Port=9003; JvmArgs="-Duser.timezone=UTC"},
    @{Name="DACS-BFF"; Path="$scriptPath\dacs-bff"; Port=9001; JvmArgs=""}
)

$successCount = 0
foreach ($service in $services) {
    if (Start-Microservice -Name $service.Name -Path $service.Path -Port $service.Port -JvmArgs $service.JvmArgs) {
        $successCount++
        Start-Sleep -Seconds 2  # Pausa entre inicios
    }
}

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  Resumen de inicialización" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Servicios iniciados exitosamente: $successCount de $($services.Count)" -ForegroundColor Green
Write-Host ""

if ($successCount -gt 0) {
    Write-Host "Los servicios estarán disponibles en:" -ForegroundColor Yellow
    Write-Host "- BFF:      http://localhost:9001/bff" -ForegroundColor White
    Write-Host "- Conector: http://localhost:9002/conector" -ForegroundColor White  
    Write-Host "- Backend:  http://localhost:9003/backend" -ForegroundColor White
    Write-Host ""
    
    Write-Host "Comandos disponibles:" -ForegroundColor Yellow
    Write-Host "- Para ver el estado: Get-Process | Where-Object {$_.ProcessName -eq 'java'}" -ForegroundColor White
    Write-Host "- Para detener todos: Stop-Process -Name 'java' -Force" -ForegroundColor White
    Write-Host ""
    
    Write-Host "Presiona Ctrl+C para detener este script (los servicios seguirán ejecutándose)" -ForegroundColor Cyan
    Write-Host "O presiona cualquier tecla para salir..." -ForegroundColor Cyan
    
    # Mantener el script ejecutándose para monitorear
    try {
        while ($true) {
            if ([Console]::KeyAvailable) {
                $key = [Console]::ReadKey($true)
                break
            }
            Start-Sleep -Seconds 1
        }
    }
    catch {
        # Ctrl+C presionado
    }
}

Write-Host ""
Write-Host "Script finalizado. Los servicios continúan ejecutándose en segundo plano." -ForegroundColor Green