@echo off
setlocal enabledelayedexpansion

echo =====================================
echo  Iniciando servicios DACS
echo =====================================
echo.

REM Verificar si Maven está instalado
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven no está instalado o no está en el PATH
    echo Por favor instala Maven y agrega la carpeta bin al PATH
    pause
    exit /b 1
)

REM Verificar si Java está instalado
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java no está instalado o no está en el PATH
    pause
    exit /b 1
)

echo Maven y Java encontrados correctamente
echo.

REM Guardar directorio actual
set "SCRIPT_DIR=%~dp0"

echo [1/3] Iniciando DACS-CONECTOR en puerto 9002...
start "DACS-CONECTOR [Puerto 9002]" cmd /k "cd /d "%SCRIPT_DIR%dacs-be\dacs-conector" && echo Compilando DACS-CONECTOR... && mvn clean package -DskipTests && echo Iniciando DACS-CONECTOR... && mvn spring-boot:run"

timeout /t 5 /nobreak >nul

echo [2/3] Iniciando DACS-BACKEND en puerto 9000...
start "DACS-BACKEND [Puerto 9000]" cmd /k "cd /d "%SCRIPT_DIR%dacs-be\dacs-backend" && echo Compilando DACS-BACKEND... && mvn clean package -DskipTests && echo Iniciando DACS-BACKEND... && mvn spring-boot:run -Dspring-boot.run.jvmArguments=-Duser.timezone=America/Argentina/Buenos_Aires"

timeout /t 5 /nobreak >nul

echo [3/3] Iniciando DACS-BFF en puerto 9001...
start "DACS-BFF [Puerto 9001]" cmd /k "cd /d "%SCRIPT_DIR%dacs-be\dacs-bff" && echo Compilando DACS-BFF... && mvn clean package -DskipTests && echo Iniciando DACS-BFF... && mvn spring-boot:run"

echo.
echo =====================================
echo  Servicios iniciados
echo =====================================
echo.
echo Los servicios estarán disponibles en:
echo - BFF:      http://localhost:9001/bff
echo - Conector: http://localhost:9002/conector
echo - Backend:  http://localhost:9003/biblioteca-comparacion
echo.
echo Se abrieron 3 ventanas separadas para cada servicio.
echo Puedes ver los logs en cada ventana.
echo Para detener un servicio, presiona Ctrl+C en su ventana.
echo.
echo Presiona cualquier tecla para cerrar esta ventana...
pause >nul
