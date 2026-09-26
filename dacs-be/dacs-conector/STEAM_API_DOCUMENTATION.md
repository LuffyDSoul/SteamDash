# Steam API Integration - Documentación

Este documento describe la integración completa con la Steam Web API implementada en el microservicio `dacs-conector`.

## Configuración

### Variables de Entorno
```yaml
STEAM_API_KEY=CHANGE_ME_STEAM_API_KEY  # Tu clave de Steam Web API
STEAM_STORE_API_URL=https://store.steampowered.com  # URL de Steam Store API
STEAM_WEB_API_URL=https://api.steampowered.com      # URL de Steam Web API
```

### Configuración en application.yml
```yaml
steam:
  api:
    key: '${STEAM_API_KEY}'
    store-url: '${STEAM_STORE_API_URL:https://store.steampowered.com}'
    web-url: '${STEAM_WEB_API_URL:https://api.steampowered.com}'
```

## Endpoints Implementados

### 1. Detalles de Juego
**Endpoint:** `GET /conector/steam/game/{appId}`

**Descripción:** Obtiene información detallada de un juego específico desde Steam Store API.

**Ejemplo:**
```bash
GET /conector/steam/game/730  # Counter-Strike 2
```

**Respuesta:**
```json
{
  "name": "Counter-Strike 2",
  "steam_appid": 730,
  "required_age": 0,
  "is_free": true,
  "detailed_description": "...",
  "short_description": "...",
  "header_image": "https://...",
  "developers": ["Valve"],
  "publishers": ["Valve"],
  "price_overview": {...},
  "categories": [...],
  "genres": [...],
  "screenshots": [...]
}
```

### 2. Noticias de Juego
**Endpoint:** `GET /conector/steam/game/{appId}/news`

**Parámetros:**
- `count` (opcional): Número de noticias (default: 10)
- `maxLength` (opcional): Longitud máxima del contenido (default: 300)

**Ejemplo:**
```bash
GET /conector/steam/game/730/news?count=5&maxLength=200
```

### 3. Juegos de Usuario
**Endpoint:** `GET /conector/steam/user/{steamId}/games`

**Parámetros:**
- `includeAppInfo` (opcional): Incluir información de aplicaciones (default: true)
- `includePlayedFreeGames` (opcional): Incluir juegos gratuitos jugados (default: true)

**Ejemplo:**
```bash
GET /conector/steam/user/76561198000000000/games
```

### 4. Lista de Todas las Aplicaciones
**Endpoint:** `GET /conector/steam/apps`

**Descripción:** Obtiene la lista completa de todas las aplicaciones disponibles en Steam.

**Ejemplo:**
```bash
GET /conector/steam/apps
```

### 5. Estadísticas de Usuario para un Juego
**Endpoint:** `GET /conector/steam/user/{steamId}/game/{appId}/stats`

**Ejemplo:**
```bash
GET /conector/steam/user/76561198000000000/game/730/stats
```

### 6. Juegos Más Jugados
**Endpoint:** `GET /conector/steam/most-played`

**Descripción:** Obtiene la lista de juegos más jugados actualmente en Steam.

**Ejemplo:**
```bash
GET /conector/steam/most-played
```

### 7. Logros de Usuario
**Endpoint:** `GET /conector/steam/user/{steamId}/game/{appId}/achievements`

**Parámetros:**
- `language` (opcional): Idioma de los logros (default: english)

**Ejemplo:**
```bash
GET /conector/steam/user/76561198000000000/game/730/achievements?language=spanish
```

### 8. Esquema de Juego
**Endpoint:** `GET /conector/steam/game/{appId}/schema`

**Parámetros:**
- `language` (opcional): Idioma del esquema (default: english)

**Ejemplo:**
```bash
GET /conector/steam/game/730/schema?language=english
```

## Arquitectura de la Integración

### Clientes Feign
- **SteamApiClient**: Para Steam Store API (no requiere autenticación)
- **SteamWebApiClient**: Para Steam Web API (requiere API key)

### DTOs Implementados
- `SteamGameDto`: Detalles completos de juegos
- `SteamNewsResponseDto`: Noticias de juegos
- `SteamOwnedGamesResponseDto`: Juegos de usuario
- `SteamAppListResponseDto`: Lista de aplicaciones
- `SteamUserStatsResponseDto`: Estadísticas de usuario
- `SteamPlayerAchievementsResponseDto`: Logros de jugador
- `SteamGameSchemaResponseDto`: Esquema de juegos
- `SteamMostPlayedGamesResponseDto`: Juegos más jugados

### Servicios
- **SteamApiService**: Interface con 8 métodos
- **SteamApiServiceImpl**: Implementación completa del servicio

### Controlador
- **SteamController**: 8 endpoints REST con manejo de errores

## Notas Importantes

1. **API Key**: Se requiere una clave válida de Steam Web API para los endpoints que acceden a datos de usuario.

2. **Rate Limiting**: Steam tiene límites de velocidad. Se recomienda implementar caché para evitar exceder los límites.

3. **SteamID**: Para obtener el SteamID de un usuario, puedes usar herramientas online o la Steam Web API.

4. **Privacidad**: Algunos datos de usuario pueden no estar disponibles si el perfil es privado.

5. **Timeout**: Los clientes Feign están configurados con timeouts de 60 segundos para manejar respuestas lentas de Steam.

## Testing

Para probar los endpoints localmente, el servicio estará disponible en:
- **Base URL**: `http://localhost:9002/conector/steam`
- **Health Check**: `http://localhost:9002/health`

Ejemplo completo:
```bash
curl -X GET "http://localhost:9002/conector/steam/game/730" \
  -H "accept: application/json"
```