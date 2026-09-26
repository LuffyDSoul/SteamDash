# DACS Backend - Steam API Backend Service

## Descripción

Servicio backend completo para la gestión de aplicaciones Steam, usuarios y logros. Proporciona una API REST completa para operaciones CRUD en todas las entidades del dominio Steam.

## Arquitectura

- **Spring Boot 3.5.5**
- **Spring Data JPA** - Persistencia y repositorios
- **Spring Security** - Configuración de seguridad moderna
- **Maven** - Gestión de dependencias
- **Lombok** - Reducción de código boilerplate
- **PostgreSQL/H2** - Base de datos (configurable)

## Estructura del Proyecto

```
src/main/java/com/dacs/backend/
├── config/                     # Configuraciones
│   ├── ModernSecurityConfig.java    # Seguridad moderna Spring Boot 3.x
├── controller/                 # Controladores REST
│   ├── AplicacionController.java    # /api/apps
│   ├── UsuarioController.java       # /api/users
│   ├── LogroObtenidoController.java # /api/achievements
│   ├── PrecioController.java        # /api/prices
│   ├── ImagenController.java        # /api/images
│   ├── VideoController.java         # /api/videos
│   ├── NoticiaController.java       # /api/news
│   ├── GeneroController.java        # /api/genres
│   ├── CategoriaController.java     # /api/categories
│   ├── LogroController.java         # /api/achievements-def
│   └── RequisitoController.java     # /api/requirements
├── exception/                  # Manejo de excepciones
│   └── GlobalExceptionHandler.java  # Manejador global
├── model/
│   ├── entity/                # Entidades JPA
│   │   ├── Aplicacion.java          # Aplicaciones Steam
│   │   ├── Usuario.java             # Usuarios Steam
│   │   ├── Logro.java               # Definiciones de logros
│   │   ├── LogroObtenido.java       # Estado de logros por usuario
│   │   ├── Precio.java              # Precios de aplicaciones
│   │   ├── Imagen.java              # Screenshots de juegos
│   │   ├── Video.java               # Videos/trailers
│   │   ├── Noticia.java             # Noticias de juegos
│   │   ├── Genero.java              # Géneros de juegos
│   │   ├── Categoria.java           # Categorías de juegos
│   │   ├── Plataforma.java          # Plataformas soportadas
│   │   └── Requisito.java           # Requisitos del sistema
│   └── enums/                 # Enumeraciones
│       ├── TGenero.java             # Tipos de género
│       ├── TCategoria.java          # Tipos de categoría
│       ├── TMoneda.java             # Tipos de moneda
│       ├── TSO.java                 # Sistemas operativos
│       └── TApp.java                # Tipos de aplicación
├── repository/                # Repositorios JPA
├── service/                   # Servicios de negocio
│   ├── impl/                       # Implementaciones
└── BackendApplication.java    # Clase principal
```

## Entidades Principales

### Aplicacion
- **Campos**: idJuego, titulo, desarrolladora, publisher, fechaLanzamiento, descripcion, etc.
- **Relaciones**: OneToOne con Precio, OneToMany con Imagen/Video/Noticia/Logro
- **Endpoints**: GET/POST/PUT/DELETE `/api/apps`

### Usuario  
- **Campos**: steamId, nickname, avatar, pais, apikey, estadoOnline
- **Relaciones**: OneToMany con LogroObtenido
- **Endpoints**: GET/POST/PUT/DELETE `/api/users`

### LogroObtenido
- **Campos**: usuario, logro, obtenido, fechaObtencion, progreso
- **Relaciones**: ManyToOne con Usuario y Logro
- **Endpoints**: GET/POST/PUT/DELETE `/api/achievements`

## API Endpoints

### Aplicaciones (`/api/apps`)
- `GET /api/apps` - Listar aplicaciones (con filtros por título, desarrolladora, género, etc.)
- `GET /api/apps/{id}` - Obtener aplicación por ID
- `GET /api/apps/steam/{steamAppId}` - Obtener por Steam App ID
- `POST /api/apps` - Crear nueva aplicación
- `PUT /api/apps/{id}` - Actualizar aplicación
- `DELETE /api/apps/{id}` - Eliminar aplicación
- `GET /api/apps/gratuitas` - Aplicaciones gratuitas
- `HEAD /api/apps/steam/{steamAppId}` - Verificar existencia

### Usuarios (`/api/users`)
- `GET /api/users` - Listar usuarios (con filtros por nickname, país)
- `GET /api/users/{id}` - Obtener usuario por ID
- `GET /api/users/steam/{steamId}` - Obtener por Steam ID
- `POST /api/users` - Crear nuevo usuario
- `PUT /api/users/{id}` - Actualizar usuario
- `DELETE /api/users/{id}` - Eliminar usuario

### Logros Obtenidos (`/api/achievements`)
- `GET /api/achievements` - Listar logros obtenidos (con filtros)
- `GET /api/achievements/user/{usuarioId}` - Logros por usuario
- `GET /api/achievements/app/{aplicacionId}` - Logros por aplicación
- `GET /api/achievements/user/{usuarioId}/app/{aplicacionId}` - Logros específicos
- `POST /api/achievements` - Registrar logro obtenido
- `PUT /api/achievements/{id}` - Actualizar logro obtenido
- `DELETE /api/achievements/{id}` - Eliminar registro

### Precios (`/api/prices`)
- `GET /api/prices` - Listar precios (con filtros por moneda, descuentos)
- `GET /api/prices/{aplicacionId}` - Obtener precio por aplicación
- `GET /api/prices/currency/{moneda}` - Precios por moneda
- `GET /api/prices/discounts` - Precios con descuento
- `POST /api/prices` - Crear precio
- `PUT /api/prices/{aplicacionId}` - Actualizar precio
- `DELETE /api/prices/{aplicacionId}` - Eliminar precio

### Imágenes (`/api/images`)
- `GET /api/images` - Listar imágenes
- `GET /api/images/app/{aplicacionId}` - Imágenes por aplicación
- `POST /api/images` - Crear imagen
- `PUT /api/images/{id}` - Actualizar imagen
- `DELETE /api/images/{id}` - Eliminar imagen

### Videos (`/api/videos`)
- `GET /api/videos` - Listar videos
- `GET /api/videos/app/{aplicacionId}` - Videos por aplicación
- `POST /api/videos` - Crear video
- `PUT /api/videos/{id}` - Actualizar video
- `DELETE /api/videos/{id}` - Eliminar video

### Biblioteca (comparación y juego único)
- `POST /biblioteca-comparacion/comparar-multiples` - Comparar bibliotecas entre 2 a 6 usuarios (body: lista de SteamUserGamesInput)
- `GET /biblioteca-juego/{appId}` - Obtener juego enriquecido y persistido por appId (guarda en DB si no existe)

Ejemplo respuesta `GET /biblioteca-juego/730`:

```json
{
  "appId": 730,
  "name": "Counter-Strike 2",
  "headerImage": "https://.../header.jpg",
  "imgVertical": "https://cdn.akamai.steamstatic.com/steam/apps/730/library_600x900.jpg",
  "imgIconUrl": null,
  "isFree": true,
  "price": "$0 USD",
  "storeUrl": "https://store.steampowered.com/app/730",
  "tags": [ {"tag": "Action"}, {"tag": "Multiplayer"} ]
}
```

### Noticias (`/api/news`)
- `GET /api/news` - Listar noticias (con filtros por fechas)
- `GET /api/news/app/{aplicacionId}` - Noticias por aplicación
- `GET /api/news/recent` - Noticias recientes (30 días)
- `POST /api/news` - Crear noticia
- `PUT /api/news/{id}` - Actualizar noticia
- `DELETE /api/news/{id}` - Eliminar noticia

## Características Implementadas

✅ **Entidades JPA completas** con relaciones bidireccionales  
✅ **Repositorios Spring Data** con consultas personalizadas  
✅ **Servicios de negocio** con lógica de validación  
✅ **Controladores REST** con operaciones CRUD completas  
✅ **Manejo global de excepciones** con respuestas estructuradas  
✅ **Configuración de seguridad moderna** para Spring Boot 3.x  
✅ **Índices de base de datos** para optimización de consultas  
✅ **Validaciones de integridad** en servicios  
✅ **Filtrado y búsqueda** en endpoints principales  

## Configuración

### Base de Datos
Configurar en `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/dacs_steam
    username: ${DB_USER:dacs_user}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

### Variables de Entorno
- `DB_USER`: Usuario de base de datos
- `DB_PASSWORD`: Contraseña de base de datos
- `DB_URL`: URL de conexión a base de datos

## Ejecución

```bash
# Compilar
mvn clean compile

# Ejecutar tests
mvn test

# Ejecutar aplicación
mvn spring-boot:run

# Generar JAR
mvn clean package
```

## Integración con Steam API

Este backend está diseñado para trabajar junto con el **dacs-conector** que proporciona:
- Integración con Steam Web API
- Obtención de datos de juegos y usuarios
- Sincronización de logros
- Actualización automática de precios

## Documentación API

La API está documentada con ejemplos de uso en cada endpoint. Todos los endpoints soportan:
- Respuestas en formato JSON
- Códigos de estado HTTP apropiados
- Manejo de errores estructurado
- Filtrado por parámetros de consulta

## Estado del Proyecto

🟢 **Completamente funcional** - Todas las entidades, servicios y controladores implementados  
🟢 **Sin errores de compilación** - Proyecto listo para despliegue  
🟢 **Arquitectura escalable** - Preparado para futuras extensiones  

## Próximos Pasos Sugeridos

1. **Configurar base de datos PostgreSQL** en producción
2. **Implementar autenticación JWT** si se requiere seguridad
3. **Agregar documentación Swagger/OpenAPI**
4. **Configurar profiles** para diferentes entornos
5. **Implementar cache Redis** para consultas frecuentes
6. **Agregar métricas y monitoring** con Micrometer/Actuator