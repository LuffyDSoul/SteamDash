# Feature de Noticias de Juegos de Usuario - Documentación

## 📋 Descripción General

Esta feature permite a los usuarios ver noticias de los juegos que poseen en Steam, con las siguientes capacidades:

1. **Ver noticias de todos los juegos del usuario**: Ingresa tu Steam ID y obtén noticias de todos tus juegos
2. **Sistema de caché inteligente**: Marca automáticamente las noticias nuevas que no has visto antes
3. **Navegación por lotes**: Los juegos se muestran en grupos de 25 para mejor rendimiento
4. **Vista colapsable**: Las últimas 2 noticias se muestran siempre, las demás en un desplegable
5. **Búsqueda de juego específico**: Busca detalles de cualquier juego por su App ID

## 🏗️ Arquitectura

```
┌─────────────┐      ┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Frontend  │─────►│     BFF     │─────►│   Backend    │      │  Conector   │
│  (Angular)  │      │  (Puerto    │      │  (Puerto     │      │  (Puerto    │
│             │      │   9001)     │──────►   9003)      │      │   9002)     │
└─────────────┘      └─────────────┘      └──────────────┘      └─────────────┘
                            │                                            │
                            └────────────────────────────────────────────┘
                                    Llamadas a Steam API
```

### Flujo de Datos:

1. **Frontend** → Solicita noticias con Steam ID
2. **BFF** → Obtiene juegos del usuario (getOwnedGames) vía Conector
3. **BFF** → Itera por los juegos (lotes de 25) y obtiene noticias (getNewsForApp) vía Conector
4. **BFF** → Envía datos al Backend para procesamiento
5. **Backend** → Filtra noticias nuevas usando caché en base de datos
6. **Backend** → Retorna noticias organizadas (últimas 2 + antiguas)
7. **BFF** → Retorna respuesta formateada al Frontend
8. **Frontend** → Muestra noticias con interfaz colapsable

## 🚀 Uso desde el Frontend

### Endpoint Principal (Usuario)

```
GET http://localhost:9001/bff/user-news/{steamId}?page=0&pageSize=25
```

**Parámetros:**
- `steamId` (path): ID de Steam del usuario (ej: `76561198037867202`) o vanity (ej: `luzier`). El BFF resolverá el vanity a steamid64 automáticamente.
- `page` (query, opcional): Número de página (default: 0)
- `pageSize` (query, opcional): Juegos por página (default: 25)

### Endpoint de Búsqueda de Juego

```
GET http://localhost:9001/bff/user-news/game-search/{appId}
```

**Parámetros:**
- `appId` (path): ID de la aplicación en Steam (ej: `730` para CS2)

### Ejemplo de Respuesta (Noticias de Usuario)

```json
{
  "steamId": "76561198037867202",
  "gamesNews": [
    {
      "appId": 730,
      "gameName": "Counter-Strike 2",
      "gameImageUrl": "https://cdn.akamai.steamstatic.com/steam/apps/730/library_600x900.jpg",
      "latestNews": [
        {
          "gid": "5869834092048567890",
          "title": "Nueva actualización de CS2",
          "url": "https://steamcommunity.com/...",
          "isExternalUrl": false,
          "author": "Valve",
          "contents": "Hemos lanzado una nueva actualización...",
          "date": 1730908800,
          "isNew": true
        },
        {
          "gid": "5869834092048567889",
          "title": "Parche de balance",
          "url": "https://steamcommunity.com/...",
          "isExternalUrl": false,
          "author": "Valve",
          "contents": "Ajustes a las armas...",
          "date": 1730822400,
          "isNew": false
        }
      ],
      "olderNews": [
        {
          "gid": "5869834092048567888",
          "title": "Noticia antigua",
          "url": "https://steamcommunity.com/...",
          "date": 1730736000,
          "isNew": false
        }
      ],
      "totalNewsCount": 3,
      "lastNewsDate": 1730908800
    }
  ],
  "totalGames": 150,
  "page": 0,
  "pageSize": 25,
  "hasMore": true
}
```

## 💾 Base de Datos (Backend)

### Tabla: `user_news_cache`

Esta tabla almacena el registro de noticias vistas por cada usuario:

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGINT | ID auto-incremental |
| steam_id | VARCHAR(50) | Steam ID del usuario |
| app_id | BIGINT | ID del juego |
| news_gid | VARCHAR(100) | ID único de la noticia |
| news_date | BIGINT | Timestamp de la noticia |
| first_seen_at | TIMESTAMP | Primera vez que se vio |
| last_checked_at | TIMESTAMP | Última verificación |

**Índices:**
- `idx_user_app` en (steam_id, app_id)
- `idx_news_gid` en (news_gid)

### Lógica de Caché

1. Cuando se procesan noticias, se verifica si el `gid` existe en caché para ese usuario
2. Si no existe → Se marca como `isNew: true` y se guarda en caché
3. Si existe → Se actualiza `last_checked_at` y se marca como `isNew: false`
4. Las entradas antiguas (>30 días sin revisar) se pueden limpiar automáticamente

## 🎨 Componentes del Frontend

### UserNewsService

**Ubicación:** `src/app/core/services/user-news.service.ts`

**Métodos principales:**
- `getUserGamesNews(steamId, page, pageSize)`: Obtiene noticias de juegos del usuario
- `searchGameDetails(appId)`: Busca detalles de un juego específico
- `formatNewsDate(timestamp)`: Formatea fechas de forma amigable
- `truncateText(text, maxLength)`: Trunca texto largo

### NewsHomeComponent

**Ubicación:** `src/app/features/news-home/`

**Funcionalidades:**
- Input para Steam ID del usuario
- Lista de juegos con noticias (paginada de 25 en 25)
- Cada juego muestra:
  - Imagen del juego (library_600x900.jpg)
  - Nombre y App ID
  - Últimas 2 noticias (siempre visibles)
  - Badge "NUEVA" en noticias no vistas
  - Botón para mostrar/ocultar noticias antiguas
- Paginación (Anterior/Siguiente)
- Búsqueda de juego por App ID
- Vista de detalles del juego buscado

## 📝 Endpoints del Backend (Internos)

### Procesar Noticias de Usuario

```
POST http://localhost:9003/api/user-news/process
Content-Type: application/json
```

**Body:**
```json
{
  "steamId": "76561198037867202",
  "gamesWithNews": [
    {
      "appId": 730,
      "gameName": "Counter-Strike 2",
      "news": [
        {
          "gid": "5869834092048567890",
          "title": "Nueva actualización",
          "url": "https://...",
          "date": 1730908800,
          "contents": "...",
          "author": "Valve"
        }
      ]
    }
  ],
  "page": 0,
  "pageSize": 25
}
```

### Marcar Noticias como Vistas

```
POST http://localhost:9003/api/user-news/mark-seen
Content-Type: application/json
```

**Body:**
```json
{
  "steamId": "76561198037867202",
  "news": [
    {
      "gid": "5869834092048567890",
      "appId": 730,
      "date": 1730908800
    }
  ]
}
```

## 🔧 Configuración

### Variables de Entorno

**BFF (application.properties):**
```properties
# URL del Backend
feign.client.config.apiBackendClient.url=http://localhost:9003

# URL del Conector
feign.client.config.apiconectorclient.url=http://localhost:9002
```

**Backend (application.yml):**
```yaml
server:
  port: 9003

spring:
  datasource:
    url: jdbc:h2:mem:dacsdb
  jpa:
    hibernate:
      ddl-auto: update
```

## 🎯 Características Especiales

### 1. Rate Limiting
El BFF implementa un delay de 200ms entre llamadas a Steam API para evitar rate limiting.

### 2. Procesamiento Concurrente
Las noticias de múltiples juegos se obtienen en paralelo (máximo 5 requests concurrentes).

### 3. Paginación en el BFF
Los juegos se procesan en lotes de 25 en el BFF, no en el backend, para mejor distribución de carga.

### 4. Caché Persistente
El backend mantiene un registro permanente de noticias vistas, permitiendo:
- Identificar noticias nuevas entre sesiones
- Mostrar badge "NUEVA" solo para contenido no visto
- Limpieza automática de registros antiguos

### 5. Imágenes de Alta Calidad
Se utiliza `library_600x900.jpg` de Steam CDN para mostrar imágenes de alta calidad de los juegos.

## 🐛 Manejo de Errores

### Errores Comunes

1. **Steam ID inválido o privado**
   - HTTP 404: Usuario no encontrado
   - Mensaje: "No se encontraron juegos para este usuario"

2. **Rate Limiting de Steam**
   - El sistema implementa delays automáticos
   - Si persiste: Reducir `pageSize` o aumentar delays

3. **Juego sin noticias**
   - Se muestra el juego pero sin noticias
   - No se considera error

4. **Timeout en requests**
   - Timeout configurado en 30 segundos por juego
   - Se continúa con los siguientes juegos

## 📊 Rendimiento

### Tiempos Estimados

Para un usuario con 100 juegos y `pageSize=25`:

- **Primera carga (página 1)**: ~15-20 segundos
  - getOwnedGames: ~2 segundos
  - getNewsForApp (25 juegos): ~10-12 segundos
  - Procesamiento en backend: ~2-3 segundos

- **Páginas siguientes**: ~12-15 segundos
  - Caché ya poblada, solo nuevas noticias

### Optimizaciones Implementadas

1. ✅ Procesamiento concurrente (5 requests paralelos)
2. ✅ Paginación en BFF (solo procesa 25 juegos a la vez)
3. ✅ Caché en backend (evita reprocesar)
4. ✅ Delays entre requests (evita rate limiting)

## 🧪 Pruebas

### Casos de Prueba

1. **Usuario con muchos juegos (>100)**
   - Verificar paginación
   - Verificar performance

2. **Usuario con juegos sin noticias**
   - Verificar que se muestra juego sin noticias
   - No debe crashear

3. **Noticias nuevas**
   - Primera vez: Todas marcadas como nuevas
   - Segunda vez: Solo nuevas desde última visita

4. **Búsqueda de juego**
   - App ID válido: Muestra detalles
   - App ID inválido: Mensaje de error

### Steam IDs de Prueba

Puedes usar estos Steam IDs públicos para pruebas:
- `76561198037867202`
- `76561198055558695`
- `76561197960435530` (GabeN - creador de Steam)

## 📚 Referencias

- [Steam Web API Documentation](https://developer.valvesoftware.com/wiki/Steam_Web_API)
- Documentación de arquitectura: `PROMPT_CONTEXT.md`
- Documentación de comparación de bibliotecas: `BIBLIOTECA_COMPARACION_API.md`

---

**Última actualización:** 6 de noviembre de 2025  
**Versión:** 1.0.0
