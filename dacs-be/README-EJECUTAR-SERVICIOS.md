# Scripts de Ejecución de Microservicios DACS

Este proyecto incluye scripts para facilitar el inicio simultáneo de todos los microservicios DACS.

## Microservicios incluidos

- **DACS-BFF**: Puerto 9001, contexto `/bff`
- **DACS-Conector**: Puerto 9002, contexto `/conector`
- **DACS-Backend**: Puerto 9003, contexto `/backend` (configurado con timezone America/Argentina/Buenos_Aires)

## Archivos de ejecución disponibles

### 1. `start-all-services.bat` (Recomendado para uso básico)

Script batch simple que inicia los 3 microservicios en ventanas separadas.

**Uso:**

```cmd
# Desde el directorio raíz del proyecto
.\start-all-services.bat
```

**Características:**

- Abre una ventana de terminal separada para cada microservicio
- Proporciona URLs de acceso
- Fácil de usar para desarrollo rápido

### 2. `start-all-services.ps1` (Recomendado para uso avanzado)

Script de PowerShell con características avanzadas de control y monitoreo.

**Uso:**

```powershell
# Desde PowerShell en el directorio raíz del proyecto
.\start-all-services.ps1
```

**Características avanzadas:**

- Verificación de puertos antes del inicio
- Control de errores mejorado
- Información detallada de procesos (PID)
- Comandos para monitoreo y gestión de servicios
- Inicio secuencial con pausas entre servicios

## Requisitos previos

1. **Java 17 o superior** instalado y configurado en PATH
2. **Maven** instalado y configurado en PATH
3. **Acceso a Internet** para descargar dependencias (primera ejecución)

## Verificación de requisitos

```cmd
# Verificar Java
java -version

# Verificar Maven
mvn -version
```

## URLs de los servicios

Una vez iniciados, los servicios estarán disponibles en:

- **BFF**: http://localhost:9001/bff
- **Conector**: http://localhost:9002/conector
- **Backend**: http://localhost:9003/backend

## Configuración especial

### Timezone del Backend

El microservicio **DACS-Backend** está configurado automáticamente con el timezone de Argentina:

- Timezone: `America/Argentina/Buenos_Aires`
- Se aplica automáticamente al ejecutar los scripts
- Afecta el manejo de fechas y horas en el backend

### Personalización de argumentos JVM

Si necesitas modificar o agregar argumentos JVM adicionales:

**En el script .bat:**

```cmd
# Editar la línea del backend en start-all-services.bat
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Duser.timezone=America/Argentina/Buenos_Aires -Xmx512m"
```

**En el script .ps1:**

```powershell
# Modificar el array $services en start-all-services.ps1
@{Name="DACS-Backend"; Path="$scriptPath\dacs-backend"; Port=9003; JvmArgs="-Duser.timezone=America/Argentina/Buenos_Aires -Xmx512m"}
```

## Endpoints de salud

Para verificar el estado de cada servicio:

- **BFF Health**: http://localhost:9001/bff/health
- **Conector Health**: http://localhost:9002/conector/health
- **Backend Health**: http://localhost:9003/backend/health

## Gestión de servicios

### Detener todos los servicios

**Método 1 - Cerrar ventanas:**

- Cierra manualmente cada ventana de terminal abierta

**Método 2 - PowerShell (solo si se usó el script .ps1):**

```powershell
# Ver procesos Java ejecutándose
Get-Process | Where-Object {$_.ProcessName -eq 'java'}

# Detener todos los procesos Java (¡CUIDADO! esto detendrá TODOS los procesos Java)
Stop-Process -Name 'java' -Force
```

**Método 3 - Administrador de tareas:**

- Abrir Administrador de tareas
- Buscar procesos "java.exe"
- Finalizar los procesos relacionados con Maven/Spring Boot

### Detener un servicio específico

1. Identificar la ventana del servicio por su título:

   - `DACS-BFF`
   - `DACS-CONECTOR`
   - `DACS-BACKEND`

2. En la ventana correspondiente, presionar `Ctrl+C`

## Solución de problemas comunes

### Error: Puerto ya en uso

**Síntoma:** Error al iniciar indicando que el puerto está ocupado

**Solución:**

1. Verificar qué proceso usa el puerto:

   ```cmd
   netstat -ano | findstr :9001
   netstat -ano | findstr :9002
   netstat -ano | findstr :9003
   ```

2. Detener el proceso usando el PID mostrado:
   ```cmd
   taskkill /PID <numero_pid> /F
   ```

### Error: Maven no encontrado

**Síntoma:** `'mvn' no se reconoce como comando`

**Solución:**

1. Verificar instalación de Maven
2. Agregar Maven al PATH del sistema
3. Reiniciar terminal/PowerShell

### Error: Java no encontrado

**Síntoma:** `'java' no se reconoce como comando`

**Solución:**

1. Verificar instalación de Java 17+
2. Configurar JAVA_HOME
3. Agregar Java al PATH del sistema

### Servicios no responden

**Diagnóstico:**

1. Verificar que los servicios hayan iniciado correctamente en sus ventanas
2. Revisar logs en las ventanas de terminal para errores
3. Verificar conectividad de red local

## Arquitectura de comunicación

```
Cliente/Frontend
       ↓
   DACS-BFF (9001)
       ↓
   DACS-Backend (9003)

   DACS-BFF (9001)
       ↓
   DACS-Conector (9002)
       ↓
   API Externa
```

El BFF actúa como punto de entrada y se comunica con Backend y Conector según sea necesario.

## Desarrollo

Para desarrollo individual de servicios, puedes iniciar cada uno por separado:

```cmd
# Backend
cd dacs-backend
mvn spring-boot:run

# BFF
cd dacs-bff
mvn spring-boot:run

# Conector
cd dacs-conector
mvn spring-boot:run
```

## Notas adicionales

- Los scripts están optimizados para Windows
- Para entornos de producción, considera usar Docker o herramientas de orquestación
- Los logs de cada servicio se muestran en sus respectivas ventanas de terminal
- La primera ejecución puede tardar más debido a la descarga de dependencias Maven
