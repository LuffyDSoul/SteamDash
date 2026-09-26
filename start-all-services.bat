@echo off
echo =====================================
echo  Iniciando todos los microservicios
echo =====================================
echo.

echo Iniciando microservicios en paralelo...
echo.

echo [1/4] Iniciando dacs-conector en puerto 9002...
start "DACS-CONECTOR" cmd /c "cd /d %~dp0dacs-be\dacs-conector && mvn spring-boot:run"

echo [2/4] Iniciando dacs-backend en puerto 9003...
start "DACS-BACKEND" cmd /c "cd /d %~dp0dacs-be\dacs-backend && mvn spring-boot:run -Dspring-boot.run.jvmArguments=-Duser.timezone=UTC"

echo [3/4] Iniciando dacs-bff en puerto 9001...
start "DACS-BFF" cmd /c "cd /d %~dp0dacs-be\dacs-bff && mvn spring-boot:run"

echo [4/4] Iniciando interfaz de usuario en puerto 4200...
start "DACS-UI" cmd /c "cd /d %~dp0dacs-ui && ng serve"

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