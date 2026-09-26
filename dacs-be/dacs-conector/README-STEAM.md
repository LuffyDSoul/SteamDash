# Conector Steam API

Este módulo proporciona conectividad con la API de Steam Store para obtener información de juegos.

## Configuración

### Variables de Entorno

El conector utiliza las siguientes variables de entorno:

- `STEAM_API_URL`: URL base de la API de Steam (por defecto: https://store.steampowered.com)
- `STEAM_API_KEY`: Clave de API de Steam (opcional para API pública)

### Configuración en Windows (PowerShell)

```powershell
$env:STEAM_API_URL = "https://store.steampowered.com"
$env:STEAM_API_KEY = "tu_clave_api_aqui"
```

### Configuración en Linux/Mac

```bash
export STEAM_API_URL="https://store.steampowered.com"
export STEAM_API_KEY="tu_clave_api_aqui"
```

## API Endpoints

### Obtener detalles de un juego

```
GET /conector/steam/game/{appId}
```

**Ejemplo:**
```
GET /conector/steam/game/250900
```

Esto devuelve la información del juego con el ID 250900 (The Binding of Isaac: Rebirth).

### Respuesta de ejemplo

```json
{
  "type": "game",
  "name": "The Binding of Isaac: Rebirth",
  "steamAppId": 250900,
  "requiredAge": 0,
  "isFree": false,
  "shortDescription": "The Binding of Isaac: Rebirth is a randomly generated action RPG shooter...",
  "detailedDescription": "<h1>Nuevo DLC disponible</h1><p>...",
  "headerImage": "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/250900/header.jpg",
  "developers": ["Nicalis, Inc.", "Edmund McMillen"],
  "publishers": ["Nicalis, Inc."],
  "platforms": {
    "windows": true,
    "mac": true,
    "linux": true
  },
  "categories": [
    {
      "id": 2,
      "description": "Un jugador"
    }
  ],
  "genres": [
    {
      "id": "1",
      "description": "Acción"
    }
  ],
  "releaseDate": {
    "comingSoon": false,
    "date": "4 NOV 2014"
  },
  "priceOverview": {
    "currency": "USD",
    "initial": 1499,
    "finalPrice": 1124,
    "discountPercent": 25,
    "initialFormatted": "$14.99",
    "finalFormatted": "$11.24 USD"
  }
}
```

## Ejecución

Para ejecutar el conector:

```bash
mvn spring-boot:run
```

El servicio estará disponible en http://localhost:9002/conector

## Notas sobre la API de Steam

- La API pública de Steam Store no requiere API key para la mayoría de endpoints
- Algunos endpoints pueden requerir autenticación con API key
- Puedes obtener una API key en: https://steamcommunity.com/dev/apikey
- La API tiene límites de velocidad, úsala responsablemente

## Estructura del Proyecto

- `SteamApiClient`: Cliente Feign para comunicarse con la API de Steam
- `SteamApiService`: Servicio de negocio para manejo de datos de Steam
- `SteamController`: Controlador REST para exponer los endpoints
- `SteamGameDto`: DTO principal para representar información de juegos
- `SteamApiConfig`: Configuración para manejo de propiedades