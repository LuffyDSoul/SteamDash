# API de Comparación de Bibliotecas de Steam

## 📋 Descripción

Este módulo permite comparar las bibliotecas de juegos de Steam entre dos usuarios, mostrando:
- Juegos que ambos usuarios tienen en común
- Juegos únicos del primer usuario
- Juegos únicos del segundo usuario
- Tiempos de juego de cada usuario para juegos comunes
- Total de horas jugadas por cada usuario

## 🏗️ Arquitectura

```
┌─────────────┐      ┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Frontend  │─────►│     BFF     │─────►│   Backend    │      │  Conector   │
│  (Angular)  │      │  (Puerto    │      │  (Puerto     │      │  (Puerto    │
└─────────────┘      │   9001)     │──────►   9003)      │      │   9002)     │
                     └─────────────┘      └──────────────┘      └─────────────┘
                            │                                            │
                            └────────────────────────────────────────────┘
                                         Llamadas a Steam API
```

### Responsabilidades por Capa:

1. **BFF (Puerto 9001)**: 
   - Recibe peticiones del frontend
   - Orquesta llamadas al Conector y Backend
   - Transforma datos simples (ej: precio null → "0 USD")

2. **Backend (Puerto 9003)**:
   - Contiene lógica de negocio pura
   - Compara bibliotecas
   - Filtra juegos válidos (type="game" o "dlc")
   - Calcula totales de horas jugadas

3. **Conector (Puerto 9002)**:
   - Solo realiza llamadas a APIs externas de Steam
   - Retorna datos sin procesar

## 🚀 Uso desde Frontend/Cliente

### Endpoint Principal (BFF)

```
GET http://localhost:9001/bff/usuarios/comparar/{steamId1}/con/{steamId2}
```
Nota: `{steamIdX}` puede ser un `steamid64` (17 dígitos) o un vanity (`gaben`, `luzier`, etc.). El BFF resuelve automáticamente a `steamid64` usando `ResolveVanityURL`.

### Ejemplo con cURL

```bash
curl -X GET "http://localhost:9001/bff/usuarios/comparar/76561198037867202/con/76561198055558695"
```

### Ejemplo con JavaScript (fetch)

```javascript
const steamId1 = '76561198037867202';
const steamId2 = '76561198055558695';

fetch(`http://localhost:9001/bff/usuarios/comparar/${steamId1}/con/${steamId2}`)
  .then(response => {
    if (!response.ok) {
      throw new Error('Error en la comparación');
    }
    return response.json();
  })
  .then(data => {
    console.log('Usuario 1:', data.usuario1);
    console.log('Usuario 2:', data.usuario2);
    console.log('Juegos comunes:', data.juegosComunes.length);
    console.log('Juegos únicos usuario 1:', data.juegosUnicosUsuario1.length);
    console.log('Juegos únicos usuario 2:', data.juegosUnicosUsuario2.length);
  })
  .catch(error => console.error('Error:', error));
```

### Ejemplo con Angular (HttpClient)

```typescript
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export class BibliotecaService {
  private bffUrl = 'http://localhost:9001/bff';

  constructor(private http: HttpClient) {}

  compararBibliotecas(steamId1: string, steamId2: string): Observable<BibliotecaComparacionDto> {
    return this.http.get<BibliotecaComparacionDto>(
      `${this.bffUrl}/usuarios/comparar/${steamId1}/con/${steamId2}`
    );
  }
}

// En el componente:
this.bibliotecaService.compararBibliotecas('76561198037867202', '76561198055558695')
  .subscribe({
    next: (resultado) => {
      console.log('Comparación exitosa:', resultado);
      this.juegosComunes = resultado.juegosComunes;
      this.juegosUsuario1 = resultado.juegosUnicosUsuario1;
      this.juegosUsuario2 = resultado.juegosUnicosUsuario2;
    },
    error: (error) => console.error('Error:', error)
  });
```

## 📤 Respuesta Esperada

```json
{
  "usuario1": {
    "steamId": "76561198037867202",
    "personaName": "Usuario1",
    "avatarFull": "https://avatars.steamstatic.com/..._full.jpg",
    "profileUrl": "https://steamcommunity.com/profiles/76561198037867202/",
    "localCountryCode": "AR",
    "timeCreated": 1234567890,
    "totalHorasJugadas": 1500
  },
  "usuario2": {
    "steamId": "76561198055558695",
    "personaName": "Usuario2",
    "avatarFull": "https://avatars.steamstatic.com/..._full.jpg",
    "profileUrl": "https://steamcommunity.com/profiles/76561198055558695/",
    "localCountryCode": "US",
    "timeCreated": 1345678901,
    "totalHorasJugadas": 2300
  },
  "juegosComunes": [
    {
      "appId": 730,
      "name": "Counter-Strike 2",
      "headerImage": "https://cdn.akamai.steamstatic.com/steam/apps/730/header.jpg",
      "isFree": true,
      "price": "0 USD",
      "type": "game",
      "tiempoJugadoUsuario1": 15000,
      "tiempoJugadoUsuario2": 8000
    },
    {
      "appId": 570,
      "name": "Dota 2",
      "headerImage": "https://cdn.akamai.steamstatic.com/steam/apps/570/header.jpg",
      "isFree": true,
      "price": "0 USD",
      "type": "game",
      "tiempoJugadoUsuario1": 30000,
      "tiempoJugadoUsuario2": 45000
    }
  ],
  "juegosUnicosUsuario1": [
    {
      "appId": 271590,
      "name": "Grand Theft Auto V",
      "headerImage": "https://cdn.akamai.steamstatic.com/steam/apps/271590/header.jpg",
      "isFree": false,
      "price": "$19.99",
      "type": "game",
      "tiempoJugadoUsuario1": 12000,
      "tiempoJugadoUsuario2": 0
    }
  ],
  "juegosUnicosUsuario2": [
    {
      "appId": 1091500,
      "name": "Cyberpunk 2077",
      "headerImage": "https://cdn.akamai.steamstatic.com/steam/apps/1091500/header.jpg",
      "isFree": false,
      "price": "$59.99",
      "type": "game",
      "tiempoJugadoUsuario1": 0,
      "tiempoJugadoUsuario2": 5000
    }
  ]
}
```

## 🔧 Uso Interno (Backend)

El Backend expone un endpoint para uso interno del BFF:

```
POST http://localhost:9003/biblioteca-comparacion/comparar
Content-Type: application/json
```

**Body:**
```json
{
  "usuario1": {
    "steamId": "76561198037867202",
    "playerInfo": {
      "personaName": "Usuario1",
      "avatarFull": "https://...",
      "profileUrl": "https://...",
      "localCountryCode": "AR",
      "timeCreated": 1234567890
    },
    "games": [
      {
        "appId": 730,
        "name": "Counter-Strike 2",
        "playtimeForever": 15000,
        "type": "game",
        "headerImage": "https://...",
        "isFree": true,
        "price": "0 USD"
      }
    ]
  },
  "usuario2": { ... }
}
```

**⚠️ Nota**: Este endpoint es solo para uso interno. Los usuarios finales deben usar el endpoint del BFF.

## 📊 Modelo de Datos

### BibliotecaComparacionDto (Respuesta)

```typescript
interface BibliotecaComparacionDto {
  usuario1: UsuarioComparacionDto;
  usuario2: UsuarioComparacionDto;
  juegosComunes: JuegoComparacionDto[];
  juegosUnicosUsuario1: JuegoComparacionDto[];
  juegosUnicosUsuario2: JuegoComparacionDto[];
}

interface UsuarioComparacionDto {
  steamId: string;
  personaName: string;
  avatarFull: string;
  profileUrl: string;
  localCountryCode: string;
  timeCreated: number;
  totalHorasJugadas: number; // Total de horas jugadas en todos los juegos
}

interface JuegoComparacionDto {
  appId: number;
  name: string;
  headerImage: string;
  isFree: boolean;
  price: string;
  type: string; // "game" o "dlc"
  tiempoJugadoUsuario1: number; // Minutos jugados por usuario 1
  tiempoJugadoUsuario2: number; // Minutos jugados por usuario 2
}
```

## 🧪 Pruebas

### Con Postman

1. Crear una request GET
2. URL: `http://localhost:9001/bff/usuarios/comparar/76561198037867202/con/76561198055558695`
3. Enviar

### Con Thunder Client (VS Code)

```
GET http://localhost:9001/bff/usuarios/comparar/76561198037867202/con/76561198055558695
```
```
GET http://localhost:9001/bff/usuarios/comparar/76561198037867202/con/luzier
```
```
GET http://localhost:9001/bff/usuarios/comparar/76561198037867202/con/76561198055558695
```

## ⚙️ Configuración

### BFF (application.properties)

```properties
# URL del Backend
backend.url=http://localhost:9003
```

### Backend (application.yml)

```yaml
server:
  port: 9003
```

## 🐛 Manejo de Errores

### Códigos de Estado HTTP

- `200 OK`: Comparación exitosa
- `404 NOT FOUND`: Usuario no encontrado
- `500 INTERNAL SERVER ERROR`: Error interno del servidor

### Ejemplo de Error

```json
{
  "timestamp": "2025-10-16T08:45:10.123",
  "status": 404,
  "error": "Not Found",
  "message": "Usuario no encontrado: 76561198037867202",
  "path": "/bff/usuarios/comparar/76561198037867202/con/76561198055558695"
}
```

## 📝 Notas Técnicas

1. **Filtrado de Juegos**: Solo se incluyen juegos de tipo "game" o "dlc". Otros tipos (software, demo, etc.) son excluidos.

2. **Tiempo de Juego**: Los tiempos se almacenan en minutos (`playtimeForever` de Steam API).

3. **Precio**: Si un juego no tiene precio disponible, se muestra como "0 USD".

4. **Performance**: La comparación es eficiente usando estructuras Map para búsquedas O(1).

5. **Asincronía**: El servicio de orquestación en el BFF puede hacer múltiples llamadas al Conector en paralelo para mejorar el rendimiento.

## 🔐 Seguridad

⚠️ **Importante**: Actualmente los endpoints están sin autenticación. En producción se debe:
- Implementar autenticación JWT
- Validar permisos de acceso
- Rate limiting para prevenir abuso
- CORS configurado correctamente

## 📚 Referencias

- [Steam Web API Documentation](https://developer.valvesoftware.com/wiki/Steam_Web_API)
- Documentación interna: `README-BACKEND.md`
- Arquitectura del proyecto: `PROJECT_ARCHITECTURE_ANALYSIS.md`
