# Feature: Estadísticas de Logros en Biblioteca Steam

## 📋 Descripción

Extensión de la funcionalidad de estadísticas para incluir análisis completo de logros (achievements) de la biblioteca de juegos del usuario. Muestra progreso, juegos completados al 100%, juegos cercanos a completar, y estadísticas basadas en los juegos más jugados.

## ✨ Características Implementadas

### Vista de Estadísticas de Logros
- **Resumen global**: Total de juegos con logros, logros desbloqueados, porcentaje global
- **Juegos al 100%**: Lista de juegos con todos los logros completados
- **Juegos cercanos al 100%**: Juegos con 80-99% de progreso (perfectos para completar)
- **Juegos más jugados con progreso**: Top juegos ordenados por tiempo jugado mostrando su progreso de logros
- **Botón de actualización**: Sincroniza todos los logros de la biblioteca desde Steam

### Optimizaciones
- **Consulta inteligente**: Prioriza juegos con más horas jugadas para mostrar estadísticas relevantes
- **Caché en BD**: Los logros se almacenan en la base de datos para consultas rápidas
- **Sincronización en segundo plano**: Para usuarios con datos existentes, la actualización es asíncrona
- **Mínimas llamadas a API**: Solo se consulta Steam cuando no hay datos en caché

## 🏗️ Arquitectura

```
┌─────────────┐      ┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Frontend  │─────►│     BFF     │─────►│   Backend    │      │  Conector   │
│  (Angular)  │      │  (Puerto    │      │  (Puerto     │      │  (Puerto    │
└─────────────┘      │   9001)     │──────►   9003)      │──────►   9002)     │
                     └─────────────┘      └──────────────┘      └─────────────┘
                            │                     │                      │
                            │                     │                      │
                            └─── Enriquece ───────┘                      │
                                  con datos                              │
                                  biblioteca                             │
                                                                          │
                                                            Steam Web API ┘
```

### Responsabilidades por Capa:

1. **Backend (Puerto 9003)**:
   - `AchievementStatsService`: Calcula estadísticas de logros desde la BD
   - `AchievementStatsController`: Endpoint `/achievement-stats/{steamId}`
   - Algoritmo de clasificación: 100%, cercanos 100%, más jugados
   - Usa repositorios `UserGameAchievementRepository` y `GameAchievementRepository`

2. **BFF (Puerto 9001)**:
   - `AchievementStatsService`: Orquesta llamadas y enriquece datos
   - Obtiene biblioteca del usuario via Conector
   - Llama al Backend con lista de top juegos por playtime
   - Enriquece respuesta con: nombre del juego, imagen header, tiempo jugado
   - Endpoints:
     - `GET /usuarios/{steamId}/estadisticas/logros` - Obtener estadísticas
     - `POST /usuarios/{steamId}/estadisticas/logros/sync` - Sincronizar logros

3. **Frontend (Angular)**:
   - Integrado en `EstadisticasComponent`
   - Carga automática al entrar a `/estadisticas/{steamId}`
   - UI con tarjetas visuales por categoría
   - Botón de sincronización con feedback visual

## 📡 API Endpoints

### 1. Obtener Estadísticas de Logros (BFF)
```http
GET /bff/usuarios/{steamId}/estadisticas/logros
```

**Parámetros:**
- `steamId` (path): Steam ID del usuario (steamid64 o vanity)

**Respuesta:**
```json
{
  "steamId": "76561198037867202",
  "totalJuegosConLogros": 45,
  "totalLogrosDesbloqueados": 523,
  "totalLogrosDisponibles": 1205,
  "porcentajeGlobal": 43.4,
  "juegosCompletos100": [
    {
      "appId": 730,
      "totalAchievements": 167,
      "unlockedAchievements": 167,
      "percentage": 100.0,
      "gameName": "Counter-Strike 2",
      "headerImage": "https://cdn.cloudflare.steamstatic.com/steam/apps/730/header.jpg",
      "playtimeForever": 15420
    }
  ],
  "juegosCercanos100": [
    {
      "appId": 570,
      "totalAchievements": 121,
      "unlockedAchievements": 105,
      "percentage": 86.8,
      "gameName": "Dota 2",
      "headerImage": "https://cdn.cloudflare.steamstatic.com/steam/apps/570/header.jpg",
      "playtimeForever": 25680
    }
  ],
  "juegosMasProgreso": [
    {
      "appId": 440,
      "totalAchievements": 520,
      "unlockedAchievements": 245,
      "percentage": 47.1,
      "gameName": "Team Fortress 2",
      "headerImage": "https://cdn.cloudflare.steamstatic.com/steam/apps/440/header.jpg",
      "playtimeForever": 8940
    }
  ]
}
```

### 2. Sincronizar Logros (BFF)
```http
POST /bff/usuarios/{steamId}/estadisticas/logros/sync
```

**Parámetros:**
- `steamId` (path): Steam ID del usuario (steamid64 o vanity)

**Respuesta:**
```
Sincronización iniciada para 150 juegos. El proceso continuará en segundo plano.
```

### 3. Calcular Estadísticas (Backend - Interno)
```http
POST /backend/achievement-stats/{steamId}
Content-Type: application/json

[730, 570, 440, 252490, 1172470]
```

**Body:** Lista de appIds ordenados por tiempo jugado (opcional)

## 🔄 Flujo de Datos

### Carga de Estadísticas
```
1. Usuario ingresa a /estadisticas/{steamId}
2. Frontend → EstadisticasService.getEstadisticas()
3. Service → GET /bff/usuarios/{steamId}/estadisticas/logros
4. BFF → Conector: GetOwnedGames (obtiene biblioteca)
5. BFF → Extrae top 50 juegos por playtime
6. BFF → Backend: POST /achievement-stats/{steamId} con lista de appIds
7. Backend → Consulta UserGameAchievementRepository y GameAchievementRepository
8. Backend → Calcula estadísticas agrupando por juego
9. Backend → Clasifica en: 100%, cercanos 100%, más jugados
10. Backend → Retorna estadísticas al BFF
11. BFF → Enriquece con datos de biblioteca (nombre, imagen, playtime)
12. BFF → Frontend: Estadísticas completas
13. Frontend → Renderiza UI con todas las categorías
```

### Sincronización de Logros
```
1. Usuario hace clic en "🔄 Actualizar Logros"
2. Frontend → POST /bff/usuarios/{steamId}/estadisticas/logros/sync
3. BFF → Conector: GetOwnedGames (obtiene todos los juegos)
4. BFF → Backend: ingestarLogros(steamId, appIds)
5. Backend → Verifica cuántos juegos tienen logros en BD
6. Si ≥10 juegos: Verificación asíncrona en segundo plano
7. Si <10 juegos: Verificación síncrona
8. Backend → Encuentra juegos faltantes
9. Backend → Para cada juego:
   a. Conector → GetPlayerAchievements (progreso usuario)
   b. Conector → GetSchemaForGame (esquema de logros)
   c. Backend → Guarda en UserGameAchievement y GameAchievement
10. BFF → Retorna mensaje de confirmación
11. Frontend → Muestra mensaje y recarga estadísticas
```

## 💾 Base de Datos

### Tablas Utilizadas

#### `user_game_achievement`
Almacena el progreso de logros de cada usuario por juego.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGINT | ID auto-incremental |
| steam_id | VARCHAR(50) | Steam ID del usuario |
| app_id | BIGINT | ID del juego |
| achievement_name | VARCHAR(255) | Nombre interno del logro |
| achieved | BOOLEAN | Si fue desbloqueado |
| unlock_time | BIGINT | Timestamp de desbloqueo |

**Índices:**
- `idx_user_app_achievement` en (steam_id, app_id, achievement_name)

#### `game_achievement`
Almacena el esquema de logros de cada juego.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGINT | ID auto-incremental |
| app_id | BIGINT | ID del juego |
| achievement_name | VARCHAR(255) | Nombre interno |
| display_name | VARCHAR(255) | Nombre visible |
| description | TEXT | Descripción del logro |
| icon_url | VARCHAR(512) | URL icono desbloqueado |
| icon_gray_url | VARCHAR(512) | URL icono bloqueado |
| hidden | BOOLEAN | Si es logro oculto |

**Índices:**
- `idx_app_achievement` en (app_id, achievement_name)

## 🎨 Interfaz de Usuario

### Características de la UI

#### Resumen Global
Cuatro tarjetas con estadísticas principales:
- 🎮 Juegos con logros
- ✅ Logros desbloqueados
- 📊 Progreso global (%)
- 🎯 Total disponibles

#### Sección: Juegos Completados (100%)
- Grid responsivo de tarjetas
- Imagen header del juego
- Nombre del juego
- Progreso: 🏆 X/X logros (100%)
- Tiempo jugado

#### Sección: Juegos Cercanos al 100% (80-99%)
- Misma estructura que juegos completos
- Icono: 🏅
- Muestra porcentaje exacto
- Ideal para motivar a completar

#### Sección: Juegos Más Jugados con Progreso
- Ordenados por tiempo jugado (descendente)
- Barra de progreso visual
- Icono: 🎖️
- Incluye todos los juegos con logros

### Botón de Actualización
- Ubicación: Esquina superior derecha de la sección
- Color: Gradiente naranja-rojo
- Estados:
  - Normal: "🔄 Actualizar Logros"
  - Cargando: Spinner animado
  - Deshabilitado durante sincronización

### Colores y Tema
Integrado con el tema existente de estadísticas:
- Fondo: Gradiente oscuro `#0a0e1a` → `#1a1f35`
- Accent: `#8b5cf6` (violeta), `#22d3ee` (cyan)
- Juegos 100%: Borde dorado `#fbbf24`
- Juegos cercanos: Borde cyan `#22d3ee`

## 🧪 Testing

### Casos de Prueba

1. **Usuario sin logros en BD**
   - Primera carga debe mostrar 0 estadísticas
   - Botón de sincronización debe funcionar
   - Después de sync, debe mostrar datos

2. **Usuario con muchos logros**
   - Verificar que se muestren todas las categorías
   - Juegos al 100% deben aparecer primero
   - Progreso global debe ser correcto

3. **Juegos cercanos al 100%**
   - Solo debe mostrar juegos entre 80-99%
   - Límite de 10 juegos
   - Ordenados por porcentaje descendente

4. **Sincronización**
   - Mensaje de confirmación
   - Recarga automática de estadísticas
   - Manejo de errores

### Steam IDs de Prueba
- `76561198037867202`
- `76561198055558695`

### Endpoints de prueba
```bash
# Obtener estadísticas
curl http://localhost:9001/bff/usuarios/76561198037867202/estadisticas/logros

# Sincronizar logros
curl -X POST http://localhost:9001/bff/usuarios/76561198037867202/estadisticas/logros/sync
```

## 📊 Estadísticas Calculadas

### 1. Juegos Completados al 100%
- **Criterio**: `percentage >= 100.0`
- **Ordenamiento**: Por porcentaje descendente (todos son 100%)
- **Límite**: Sin límite
- **Uso**: Mostrar logros del usuario

### 2. Juegos Cercanos al 100%
- **Criterio**: `80.0 <= percentage < 100.0`
- **Ordenamiento**: Por porcentaje descendente
- **Límite**: Top 10
- **Uso**: Motivar a completar juegos casi terminados

### 3. Juegos Más Jugados con Progreso
- **Criterio**: Todos los juegos con logros
- **Ordenamiento**: Según tiempo jugado (proporcionado por biblioteca)
- **Límite**: Top 10
- **Uso**: Mostrar progreso en juegos favoritos

### 4. Porcentaje Global
- **Fórmula**: `(totalLogrosDesbloqueados / totalLogrosDisponibles) * 100`
- **Uso**: Métrica general de completitud

## ⚡ Optimizaciones Implementadas

1. **Priorización por playtime**:
   - Se envían top 50 juegos más jugados al backend
   - El backend los prioriza en la sección "Más jugados"
   - Reduce consultas innecesarias a juegos poco jugados

2. **Caché en base de datos**:
   - Logros almacenados en BD (UserGameAchievement)
   - Esquemas de juegos cacheados (GameAchievement)
   - Consultas rápidas sin llamar a Steam API

3. **Sincronización inteligente**:
   - Usuarios nuevos: Verificación síncrona (bloqueante)
   - Usuarios con ≥10 juegos: Verificación asíncrona (segundo plano)
   - Solo se sincronizan juegos faltantes

4. **Enriquecimiento en BFF**:
   - Backend solo calcula estadísticas
   - BFF agrega datos de presentación (nombre, imagen, playtime)
   - Separación de responsabilidades

5. **Carga paralela en frontend**:
   - Estadísticas de logros se cargan junto con otras estadísticas
   - Si falla, no bloquea el resto de la vista
   - Manejo graceful de errores

## 📝 Notas de Diseño

### Edge Cases Manejados

1. **Juegos sin logros**:
   - No aparecen en estadísticas
   - Backend filtra por `totalAchievements > 0`

2. **Usuario sin biblioteca**:
   - Retorna estadísticas vacías
   - No genera errores

3. **Perfil privado**:
   - Steam API retorna error
   - Se maneja gracefully, estadísticas en 0

4. **Timeout de sincronización**:
   - Proceso continúa en segundo plano
   - Usuario recibe confirmación inmediata

### Futuras Mejoras Sugeridas

1. **Filtros adicionales**:
   - Por género de juego
   - Por rango de dificultad
   - Por fecha de último logro

2. **Comparación con amigos**:
   - Ver quién tiene más logros
   - Juegos en común con progreso

3. **Logros raros**:
   - Identificar logros con bajo % global
   - Destacar logros únicos del usuario

4. **Timeline de logros**:
   - Gráfico de logros por mes/año
   - Rachas de logros

5. **Exportar estadísticas**:
   - PDF con resumen
   - CSV para análisis externo

## 🔐 Seguridad

⚠️ **Importante**: Actualmente los endpoints están sin autenticación. En producción se debe:
- Implementar autenticación JWT
- Validar permisos de acceso
- Rate limiting para prevenir abuso
- CORS configurado correctamente

## 🚀 Cómo Usar

### Desde la Aplicación

1. Navega a la vista de estadísticas:
   ```
   http://localhost:4200/estadisticas/{steamId}
   ```

2. Las estadísticas de logros se cargan automáticamente junto con otras estadísticas

3. Para actualizar logros:
   - Click en botón "🔄 Actualizar Logros"
   - Esperar confirmación
   - Las estadísticas se recargan automáticamente

### Desde API Directa

```bash
# Ver estadísticas
curl http://localhost:9001/bff/usuarios/luzier/estadisticas/logros

# Sincronizar
curl -X POST http://localhost:9001/bff/usuarios/luzier/estadisticas/logros/sync
```

## 📚 Referencias

- [Steam Web API Documentation](https://developer.valvesoftware.com/wiki/Steam_Web_API)
- [ISteamUserStats/GetPlayerAchievements](https://developer.valvesoftware.com/wiki/Steam_Web_API#GetPlayerAchievements_.28v0001.29)
- [ISteamUserStats/GetSchemaForGame](https://developer.valvesoftware.com/wiki/Steam_Web_API#GetSchemaForGame_.28v2.29)
- Documentación de arquitectura: `PROMPT_CONTEXT.md`
- Documentación de biblioteca: `README-BIBLIOTECA-USUARIO.md`

---

**Última actualización:** 30 de noviembre de 2025  
**Versión:** 1.0.0  
**Autor:** Sistema DACS
