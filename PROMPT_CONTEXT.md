# Contexto y reglas para futuros prompts

Este archivo documenta las reglas y responsabilidades por carpeta del monorepo DACS (dacs-conector, dacs-bff, dacs-backend) y sirve como referencia para pedir cambios futuros. Está pensado tanto para humanos como para agentes automatizados que reciben instrucciones sobre dónde se puede y no se puede tocar código.

## Resumen ejecutivo (convenio)

- `dacs-conector`: frontera con APIs externas. Aquí vive TODO lo relacionado con llamadas externas, normalización en DTOs, gestión de tokens/keys, rate limiting, retries y backoff. No se deben implementar reglas de negocio ni persistencia aquí.
- `dacs-bff`: orquestación para la UI. Aquí solo se hacen transformaciones de forma/shape de respuesta (paginación, resúmenes, formateo para la SPA). No ejecutar reglas de negocio complejas (por ejemplo, no calcular similitudes o intersecciones). Puede adaptar valores por conveniencia de UI (ej. asignar precio "0 USD" si un juego es gratis).
- `dacs-backend`: dominio y persistencia. Aquí va toda la lógica de negocio, modelos de dominio, sincronización y reglas específicas. Cualquier cálculo de negocio, agregación compleja o persistencia debe implementarse aquí.

Si se requiere un cambio que cruza capas, favor de proponer la modificación en la capa correspondiente según la regla anterior.

## Contrato corto (inputs / outputs / errores esperables)

- Inputs: requests entrantes (HTTP, mensajes), DTOs desde otras capas. Siempre proveer ejemplos de payloads cuando pidas cambios.
- Outputs: DTOs listos para la UI (BFF), entidades persistidas (backend), DTOs normalizados (conector).
- Errores: respuestas HTTP con códigos adecuados (400/404/500). Para errores externos en conector, mapear a 5xx en BFF cuando corresponde y proveer mensajes amigables para UI.

## Reglas por carpeta (detalladas)

1) `dacs-conector` (ubicación: `dacs-conector/`)
  - Qué sí: llamadas a APIs externas, clientes Feign/HTTP, normalización a DTOs internos, manejo de tokens/keys, retries, backoff y rate limiting. Documentar timeouts y fallbacks.
  - Qué no: no componer vistas UI, no cambios de business logic (p. ej. decidir qué juegos son "similares"). No persistir estado de negocio.
  - Ejemplo aceptable: `SteamApiClient` convierte respuesta de Steam a `SteamGameDto`.

2) `dacs-bff` (ubicación: `dacs-bff/`)
  - Qué sí: orquestar llamadas al backend y conector, combinar respuestas, transformar shapes para la SPA (paginación, fields renaming, agregar campos de presentación), formatear precios (ej. convertir null -> "0 USD"), reducir payloads a lo necesario por la UI.
  - Qué no: no implementar reglas de negocio que requieran acceso a datos internos o cálculos complejos (por ejemplo, no implementar el algoritmo de comparación de bibliotecas). En vez de eso, pedir al backend que devuelva la info necesaria.
  - Ejemplo aceptable: si una app no tiene price info proveniente del backend, BFF puede devolver price: "0 USD" para la UI.

3) `dacs-backend` (ubicación: `dacs-backend/`)
  - Qué sí: modelos de dominio, persistencia (JPA/repo), lógica de negocio, cálculos complejos, sincronización entre diferentes fuentes. Aquí se crean entidades que combinan datos de distintas APIs y se guarda el estado.
  - Qué no: no realizar llamadas directamente a APIs de terceros (eso es responsabilidad del conector). Evitar duplicar la lógica de normalización del conector.

## Ejemplos prácticos (peticiones frecuentes y dónde implementarlas)

- "Agregar precio '0 USD' cuando no exista": Implementarlo en el BFF (porque es un ajuste de presentación). Documentar en el BFF code path y tests.
- "Retry/backoff para Steam API": Implementarlo en dacs-conector, no en BFF ni Backend.
- "Comparar bibliotecas y calcular similitud": Implementarlo en dacs-backend (lógica de negocio), exponer endpoint que el BFF consuma.

## Convención de nombres y archivos añadidos

- Si necesitas crear un cliente al conector, usar `*Client` o `*ApiClient` dentro de `dacs-conector/src/...`.
- En BFF, colocar adaptadores/transformers en `dacs-bff/src/main/java/.../adapters` o `mappers`.
- En Backend, usar paquetes `service/`, `repository/`, `model/` y crear tests unitarios para la lógica de negocio.

## Cómo usar este archivo en futuros prompts

1. Referir el cambio proponiendo la carpeta objetivo (conector | bff | backend).
2. Indicar si el cambio es de presentación (BFF), integración (conector) o negocio (backend).
3. Incluir un ejemplo de request y response esperado.

Ejemplo de prompt minimal correcto:
  - "En la carpeta `dacs-bff` quiero que, cuando el backend devuelva price=null, el BFF transforme a price: '0 USD' y lo haga de forma que el front reciba priceFormatted y priceValue (entero en cents). Aquí va ejemplo de payload: ..."

## Notas de diseño y edge-cases

- Perfil privado en Steam: el conector debe propagar un estado que indique datos incompletos; el backend decide si persistir o marcar como privado.
- Rate limiting: el conector debe exponerse con circuit-breaker y fallbacks.
- Paginación: BFF puede resumir/combinar resultados y proveer cursors o paginación por página.

---
Archivo generado automáticamente para ayudar a coordinar cambios entre capas. Mantener actualizado y referenciar en PRs cuando aplique.
