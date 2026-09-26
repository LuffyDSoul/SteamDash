@echo off
echo =====================================
echo  Iniciando todos los microservicios
echo =====================================
echo.

echo Iniciando microservicios en paralelo...
echo.

echo [1/3] Iniciando dacs-conector en puerto 9002...
start "DACS-CONECTOR" cmd /c "cd /d %~dp0dacs-conector && mvn spring-boot:run"

echo [2/3] Iniciando dacs-backend en puerto 9003...
start "DACS-BACKEND" cmd /c "cd /d %~dp0dacs-backend && mvn spring-boot:run -Dspring-boot.run.jvmArguments=-Duser.timezone=America/Argentina/Buenos_Aires"

echo [3/3] Iniciando dacs-bff en puerto 9001...
start "DACS-BFF" cmd /c "cd /d %~dp0dacs-bff && mvn spring-boot:run"

echo.
echo =====================================
echo  Todos los servicios estan iniciando
echo =====================================
echo.
echo Los servicios estarán disponibles en:
echo - BFF:      http://localhost:9001/bff
echo - Conector: http://localhost:9002/conector  
echo - Backend:  http://localhost:9003/backend
echo.
echo Se han abierto 3 ventanas de terminal separadas.
echo Para detener todos los servicios, cierra todas las ventanas
echo o presiona Ctrl+C en cada una de ellas.
echo.
echo Presiona cualquier tecla para cerrar esta ventana...
pause >nul