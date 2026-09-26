# Workers sugeridos y contratos (contexto para implementaciones)

Este archivo propone una lista de workers/servicios internos que pueden ayudar en la separación de responsabilidades entre capas y servir como referencia para futuros PRs o tareas.

## Propósito

Proponer workers que encapsulan tareas largas o independientes: sincronización periódica, procesamiento asíncrono, enriquecimiento de datos o colas de trabajo. Cada worker tiene una ubicación sugerida y contrato (inputs/outputs).

## Plantilla de contrato de worker

- Nombre: identificador único
- Ubicación sugerida: carpeta del repo
- Tipo: cron / queue / http-trigger
- Entradas: formato del job o payload
- Salidas: artefactos, llamadas a endpoints, DB writes
- Errores: cómo se registran y reintentos

## Workers sugeridos

1) steam-sync-worker
  - Ubicación: `dacs-conector/workers/steam-sync/` (o `dacs-conector/src/main/...` si prefieres Java)
  - Tipo: cron (ej: se ejecuta cada N minutos)
  - Entradas: lista de steamAppIds o un feed (file/queue)
  - Salidas: resultados normalizados (DTOs) hacia un topic o endpoint del backend (POST `/backend/apps/sync`)
  - Errores: retries con backoff, circuit-breaker. Registrar métricas.

2) compare-library-processor
  - Ubicación: `dacs-backend/workers/compare-library/`
  - Tipo: queue / http-trigger
  - Entradas: payload con dos steamIds
  - Salidas: resultado de comparación persistido en DB y notificación al BFF (o endpoint que el BFF consulte)
  - Notas: Este worker debe contener la lógica de negocio y usar servicios del backend.

3) bff-shaper-worker (opcional)
  - Ubicación: `dacs-bff/workers/` (solo si hay procesamiento asíncrono para la UI)
  - Tipo: http-trigger / queue
  - Entradas: request para preparar vistas pesadas (ej. pre-cálculo de dashboards)
  - Salidas: cache/endpoint listo para la SPA
  - Notas: BFF en general debe ser sin estado; este worker es opcional para tareas de preprocesamiento que agilicen la UI.

## Ejemplo de contrato (compare-library-processor)

Input (JSON):

{
  "requestId": "uuid-v4",
  "steamId1": "7656119...",
  "steamId2": "7656119...",
  "requestedBy": "user-id-or-service"
}

Output (persistido):

{
  "requestId": "uuid-v4",
  "usuario1": { /* resumen */ },
  "usuario2": { /* resumen */ },
  "estadisticas": { /* totals, counts */ },
  "juegosComunes": [...]
}

Errors: si la cuenta es privada, marcar status=PRIVATE y dejar campos vacíos con mensaje para UI.

---
Mantener los workers pequeños, con contratos documentados y tests unitarios. Referenciar al `PROMPT_CONTEXT.md` para decidir en qué capa debe vivir la lógica principal.
