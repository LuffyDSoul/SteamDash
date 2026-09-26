# Funcionalidad: Biblioteca de Usuario

## 📋 Descripción

Nueva funcionalidad que permite visualizar la biblioteca de juegos de un usuario de Steam. Muestra todos los juegos que posee un usuario junto con su información detallada y permite actualizar datos individuales de cada juego.

## ✨ Características

### Vista de Biblioteca
- **Lista de juegos del usuario**: Obtiene todos los juegos mediante `GetOwnedGames` de la API de Steam
- **Header image al 100%**: Muestra la imagen de cabecera de cada juego en tamaño completo
- **Precio del juego**: Muestra el precio actual o si es gratuito
- **Tiempo jugado**: Muestra las horas totales jugadas en cada juego
- **Información del usuario**: Avatar, nombre y total de juegos

### Botón de Refresh
Cada juego tiene un botón 🔄 que al presionarlo:
1. Llama a `AppDetails` de Steam para obtener información detallada
2. Llama a `SteamSpy` API para datos adicionales
3. Obtiene la imagen `library_600x900.jpg` del CDN de Steam
4. Guarda todos estos datos en la base de datos

### Juego Aleatorio 🎲
Nueva funcionalidad que sugiere un juego aleatorio de tu biblioteca:
- **Filtro de juegos no jugados**: Muestra solo juegos con 0 horas
- **Filtro personalizado**: Filtra juegos con menos de X horas jugadas
- **Vista completa del juego**: Muestra descripción, screenshots, tags, desarrolladores
- **Información enriquecida**: Obtiene datos completos desde Steam AppDetails
- **Navegación fácil**: Botones para buscar otro juego o volver a la biblioteca

## 🚀 Uso

### Acceso a la Biblioteca

#### Por URL directa:
```
http://localhost:4200/biblioteca
```

#### Con Steam ID en la URL:
```
http://localhost:4200/biblioteca/76561198037867202
```

### Búsqueda Manual
1. Navega a `/biblioteca`
2. Ingresa un Steam ID en el campo de búsqueda
3. Presiona "Buscar" o Enter

### Actualizar un Juego
1. Encuentra el juego en la lista
2. Haz clic en el botón 🔄 en la esquina superior derecha de la imagen del juego
3. El botón mostrará un spinner mientras se actualiza
4. Los datos del juego se actualizarán automáticamente

### Buscar Juego Aleatorio
1. En la vista de biblioteca, haz clic en el botón "🎲 Juego Aleatorio"
2. Selecciona el filtro deseado:
   - **Solo juegos no jugados**: Para descubrir juegos que nunca has iniciado
   - **Menos de X horas**: Personaliza las horas máximas jugadas
3. El sistema buscará un juego aleatorio y mostrará su información completa
4. Opciones disponibles:
   - **Ver en Steam Store**: Abre la página del juego en Steam
   - **Buscar otro juego**: Obtiene un nuevo juego aleatorio con los mismos criterios
   - **Volver a la biblioteca**: Regresa a la vista de biblioteca

## 🏗️ Arquitectura

### Frontend (Angular)
```
dacs-fe/src/app/
├── core/
│   ├── models/
│   │   └── biblioteca-usuario.model.ts    # Modelos de datos (incluye IJuegoAleatorio)
│   └── services/
│       └── biblioteca-usuario.service.ts   # Servicio HTTP
└── features/
    ├── biblioteca-usuario/
    │   ├── biblioteca-usuario.component.ts    # Lógica del componente
    │   ├── biblioteca-usuario.component.html  # Vista
    │   └── biblioteca-usuario.component.css   # Estilos
    └── juego-aleatorio/
        ├── juego-aleatorio.component.ts       # Lógica del componente de juego aleatorio
        ├── juego-aleatorio.component.html     # Vista de juego aleatorio
        └── juego-aleatorio.component.css      # Estilos de juego aleatorio
```

### Backend (Spring Boot - BFF)
```
dacs-be/dacs-bff/src/main/java/com/dacs/bff/
├── controller/
│   └── UsuarioController.java                    # Endpoints REST
├── service/
│   ├── BibliotecaUsuarioService.java             # Interface del servicio
│   └── BibliotecaUsuarioServiceImpl.java         # Implementación
└── dto/
    ├── BibliotecaUsuarioDto.java                 # DTO de biblioteca
    ├── JuegoUsuarioDto.java                      # DTO de juego
    ├── JuegoAleatorioDto.java                    # DTO de juego aleatorio
    └── RefreshJuegoResponseDto.java              # DTO de respuesta refresh
```

## 📡 API Endpoints

### 1. Obtener Biblioteca del Usuario
```http
GET /usuarios/{steamId}/biblioteca
```

**Parámetros:**
- `steamId` (path): Steam ID del usuario

Nota: ahora `steamId` acepta tanto `steamid64` (17 dígitos) como un vanity URL (por ejemplo, `luzier`). El BFF resuelve automáticamente el vanity usando `ResolveVanityURL`.

**Respuesta:**
```json
{
  "steamId": "76561198037867202",
  "personaName": "NombreUsuario",
  "avatarUrl": "https://...",
  "totalJuegos": 150,
  "juegos": [
    {
      "appId": 730,
      "name": "Counter-Strike: Global Offensive",
      "playtimeForever": 12500,
      "playtime2Weeks": 120,
      "headerImage": "https://cdn.akamai.steamstatic.com/steam/apps/730/header.jpg",
      "price": "Gratis",
      "isFree": true,
      "storeUrl": "https://store.steampowered.com/app/730",
      "libraryImage": null
    }
  ]
}
```

### 2. Actualizar Juego (Refresh)
```http
POST /usuarios/{steamId}/biblioteca/juegos/{appId}/refresh
```

**Parámetros:**
- `steamId` (path): Steam ID del usuario (steamid64 o vanity)
- `appId` (path): App ID del juego

**Respuesta:**
```json
{
  "appId": 730,
  "success": true,
  "message": "Juego actualizado exitosamente",
  "juegoActualizado": {
    "appId": 730,
    "name": "Counter-Strike: Global Offensive",
    "headerImage": "https://...",
    "price": "Gratis",
    "isFree": true,
    "storeUrl": "https://...",
    "libraryImage": "https://cdn.akamai.steamstatic.com/steam/apps/730/library_600x900.jpg"
  }
}
```

### 3. Obtener Juego Aleatorio
```http
GET /usuarios/{steamId}/biblioteca/juego-aleatorio?maxHoras={maxHoras}
```

**Parámetros:**
- `steamId` (path): Steam ID del usuario (steamid64 o vanity)
- `maxHoras` (query, opcional): Máximo de horas jugadas para filtrar (omitir para solo juegos no jugados)

**Respuesta:**
```json
{
  "appId": 250900,
  "name": "The Binding of Isaac: Rebirth",
  "description": "The Binding of Isaac: Rebirth is a randomly generated...",
  "shortDescription": "The Binding of Isaac: Rebirth is a randomly generated action RPG...",
  "headerImage": "https://cdn.akamai.steamstatic.com/steam/apps/250900/header.jpg",
  "libraryImage": "https://cdn.akamai.steamstatic.com/steam/apps/250900/library_600x900.jpg",
  "backgroundImage": "https://cdn.akamai.steamstatic.com/steam/apps/250900/page_bg_generated.jpg",
  "price": "$14.99",
  "isFree": false,
  "storeUrl": "https://store.steampowered.com/app/250900",
  "tags": ["Roguelike", "Indie", "Action", "Difficult"],
  "screenshots": [
    "https://cdn.akamai.steamstatic.com/steam/apps/250900/ss_1.jpg",
    "https://cdn.akamai.steamstatic.com/steam/apps/250900/ss_2.jpg"
  ],
  "developers": ["Edmund McMillen", "Nicalis, Inc."],
  "publishers": ["Nicalis, Inc."],
  "releaseDate": "Nov 4, 2014",
  "genres": ["Action", "Indie"],
  "categories": ["Single-player", "Steam Achievements"],
  "playtimeForever": 0
}
```

## 🔄 Flujo de Datos

### Carga de Biblioteca
```
1. Usuario ingresa Steam ID
2. Frontend → BFF: GET /usuarios/{steamId}/biblioteca
3. BFF → Conector: GetOwnedGames
4. BFF → Conector: GetPlayerSummaries
5. BFF procesa y formatea datos
6. BFF → Frontend: Biblioteca completa
7. Frontend renderiza juegos
```

### Refresh de Juego
```
1. Usuario hace clic en botón 🔄
2. Frontend → BFF: POST /usuarios/{steamId}/biblioteca/juegos/{appId}/refresh
3. BFF → Conector: AppDetails de Steam
4. BFF → Conector: SteamSpy AppDetails
5. BFF → CDN Steam: Verifica library_600x900.jpg
6. BFF → Backend: Guarda datos en BD (TODO)
7. BFF → Frontend: Juego actualizado
8. Frontend actualiza la vista
```

### Juego Aleatorio
```
1. Usuario hace clic en botón 🎲
2. Frontend → Nueva vista: /biblioteca/{steamId}/aleatorio
3. Usuario selecciona filtro (no jugados o personalizado)
4. Frontend muestra: "Buscando juego aleatorio..."
5. Frontend → BFF: GET /usuarios/{steamId}/biblioteca/juego-aleatorio?maxHoras=X
6. BFF → Conector: GetOwnedGames (obtiene biblioteca)
7. BFF filtra juegos según criterio (playtime <= maxHoras)
8. BFF selecciona juego aleatorio del conjunto filtrado
9. BFF → Backend: Obtiene juego completo (enriquecido con AppDetails)
10. BFF → Frontend: Juego aleatorio con información completa
11. Frontend renderiza vista con imagen vertical, descripción, tags, screenshots
12. Usuario puede:
    - Ver en Steam Store (abre navegador)
    - Buscar otro juego (repite proceso)
    - Volver a biblioteca
```

## 🎨 Interfaz de Usuario

### Características de la UI
- **Grid responsivo**: Se adapta a diferentes tamaños de pantalla
- **Cards de juegos**: Cada juego tiene su propia tarjeta con imagen y detalles
- **Hover effects**: Animaciones al pasar el mouse
- **Loading states**: Spinners durante las cargas
- **Error handling**: Mensajes de error claros
- **Botón refresh interactivo**: Animación de rotación y estado de carga

### Colores y Tema
- Inspirado en el tema de Steam
- Colores principales: #1b2838, #66c0f4
- Hover effects con gradientes
- Sombras y elevación en las cards

## 🔧 Configuración

### Variables de Entorno
El servicio usa las configuraciones existentes del BFF:
- URL del BFF: `http://localhost:9001`
- URL del Conector: `http://localhost:9002`

### Rutas Angular
Agregar a `app.routes.ts`:
```typescript
{ 
  path: 'biblioteca', 
  loadComponent: () => import('./features/biblioteca-usuario/biblioteca-usuario.component')
    .then(m => m.BibliotecaUsuarioComponent) 
},
{ 
  path: 'biblioteca/:steamId', 
  loadComponent: () => import('./features/biblioteca-usuario/biblioteca-usuario.component')
    .then(m => m.BibliotecaUsuarioComponent) 
},
{
  path: 'biblioteca/:steamId/aleatorio',
  loadComponent: () => import('./features/juego-aleatorio/juego-aleatorio.component')
    .then(m => m.JuegoAleatorioComponent)
}
```

## 📝 Tareas Pendientes (TODO)

1. **Backend - Persistencia**: 
   - Implementar guardado de juegos en la base de datos
   - Crear entidades y repositorios para juegos actualizados
   - Endpoint en el backend para guardar datos de refresh

2. **Caché**:
   - Implementar caché de imágenes
   - Caché de datos de juegos para evitar llamadas repetidas

3. **Optimizaciones**:
   - Lazy loading de imágenes
   - Paginación de juegos si la biblioteca es muy grande
   - Búsqueda y filtrado de juegos en la biblioteca

4. **Características adicionales**:
   - Ordenar juegos por nombre, tiempo jugado, etc.
   - Estadísticas de la biblioteca (géneros más jugados, etc.)
   - Comparar con otras bibliotecas

## 🧪 Testing

### Ejemplo de Steam IDs para testing:
- `76561198037867202`
- `76561198055558695`

### Endpoints de prueba:
```bash
# Obtener biblioteca
curl http://localhost:9001/usuarios/76561198037867202/biblioteca

# Refresh de un juego (CS:GO)
curl -X POST http://localhost:9001/usuarios/76561198037867202/biblioteca/juegos/730/refresh

# Obtener juego aleatorio (solo no jugados)
curl http://localhost:9001/usuarios/76561198037867202/biblioteca/juego-aleatorio

# Obtener juego aleatorio (menos de 5 horas)
curl "http://localhost:9001/usuarios/76561198037867202/biblioteca/juego-aleatorio?maxHoras=5"
```

## 🐛 Troubleshooting

### Error: "Usuario no encontrado"
- Verificar que el Steam ID sea válido
- Verificar que el perfil de Steam sea público

### Error: "No se pueden cargar los juegos"
- Verificar que el servicio Conector esté corriendo
- Verificar la API Key de Steam en el conector

### Imágenes no cargan
- Las imágenes se cargan desde el CDN de Steam
- Algunos juegos pueden no tener todas las imágenes disponibles
- El componente tiene fallback a placeholder

### Juego aleatorio no encuentra juegos
- Verifica que tengas juegos que cumplan el criterio seleccionado
- Si seleccionaste "no jugados", asegúrate de tener juegos con 0 horas
- Si usas filtro personalizado, aumenta el número de horas máximas

### El juego aleatorio no muestra toda la información
- Algunos juegos pueden no tener toda la información en Steam
- El sistema mostrará la información disponible
- Si falta información, el juego se mostrará con datos básicos

## 📚 Referencias

- [Steam Web API Documentation](https://steamcommunity.com/dev)
- [SteamSpy API](https://steamspy.com/api.php)
- [Steam CDN Images](https://partner.steamgames.com/doc/store/assets)
