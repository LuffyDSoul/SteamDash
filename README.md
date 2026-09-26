DACS 2025 - Steam API
======================

Proyecto académico para consultar y mostrar información de Steam mediante una arquitectura de microservicios.

El sistema permite integrar un frontend Angular con servicios backend encargados de consultar la API de Steam, administrar la comunicación interna y trabajar con una base de datos PostgreSQL.


ARQUITECTURA
------------

- dacs-fe: frontend desarrollado con Angular.
- dacs-bff: Backend For Frontend que centraliza las solicitudes del frontend.
- dacs-conector: servicio que se comunica con Steam, SteamSpy y otras APIs externas.
- dacs-backend: servicio de negocio y persistencia con PostgreSQL.
- Keycloak: autenticación y autorización de usuarios.

Flujo principal:

    Angular -> BFF -> Backend
                  -> Conector -> Steam / SteamSpy


PUERTOS Y URLS LOCALES
----------------------

- Frontend Angular: http://localhost:4200
- BFF: http://localhost:9001/bff
- Conector: http://localhost:9002/conector
- Backend: http://localhost:9003/backend
- Keycloak: http://localhost:8080
- PostgreSQL: localhost:5432

Endpoints de salud:

- http://localhost:9001/bff/health
- http://localhost:9002/conector/health
- http://localhost:9003/backend/health


REQUISITOS
----------

- Java 17 o superior.
- Maven.
- Node.js y npm.
- Angular CLI.
- PostgreSQL.
- Keycloak.
- Una clave de Steam Web API.
- Acceso a Internet para descargar dependencias y consultar las APIs externas.


CONFIGURACION
-------------

1. Copiar example.env como .env.
2. Reemplazar los valores de ejemplo con la configuración local.
3. Configurar la clave de Steam Web API desde:

   https://steamcommunity.com/dev/apikey

4. Crear la base de datos PostgreSQL indicada por DB_URL, DB_USER y DB_PASSWORD.
5. Configurar Keycloak siguiendo KEYCLOAK_SETUP.md dentro de dacs-fe.
6. No publicar nunca el archivo .env real ni credenciales en GitHub.

El frontend utiliza archivos de entorno Angular ubicados en:

    dacs-fe/src/environments/


EJECUCION RAPIDA
----------------

Desde la raíz del repositorio, en Windows:

    .\start-all-services.bat

También se puede utilizar el script avanzado de PowerShell:

    .\start-all-services.ps1

Para iniciar el frontend por separado:

    cd dacs-fe
    npm install
    ng serve --proxy-config proxy.conf.json

Los servicios Java también pueden ejecutarse individualmente desde sus directorios:

    cd dacs-be/dacs-backend
    mvn spring-boot:run

    cd dacs-be/dacs-bff
    mvn spring-boot:run

    cd dacs-be/dacs-conector
    mvn spring-boot:run


ESTRUCTURA DEL REPOSITORIO
--------------------------

    dacs-be/
      dacs-backend/
      dacs-bff/
      dacs-conector/

    dacs-fe/

    start-all-services.bat
    start-all-services.ps1
    example.env


DOCUMENTACION ADICIONAL
-----------------------

- README.md: información general del repositorio.
- README-EJECUTAR-SERVICIOS.md: ejecución y monitoreo de microservicios.
- README-ESTADISTICAS-LOGROS.md: estadísticas y logros de Steam.
- README-NEWS-FEATURE.md: funcionalidad de noticias.
- README-BIBLIOTECA-USUARIO.md: biblioteca del usuario.
- dacs-be/dacs-conector/README-STEAM.md: integración con Steam.
- dacs-fe/KEYCLOAK_SETUP.md: configuración de Keycloak.


NOTAS DE SEGURIDAD
------------------

- No subir .env, contraseñas, tokens ni claves privadas al repositorio.
- Revocar y regenerar cualquier clave que haya sido publicada accidentalmente.
- Usar example.env únicamente como plantilla de configuración.


LICENCIA
--------

Proyecto académico DACS 2025.
