package com.dacs.backend.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dacs.backend.entity.GameRecord;
import com.dacs.backend.repository.GameRecordRepository;
import com.dacs.backend.dto.SteamUserGamesInput;
import com.dacs.backend.client.ConectorClient;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GameRecordServiceImpl {

    @Autowired
    private GameRecordRepository repo;

    @Autowired
    private ConectorClient conectorClient;

    @Value("${comparison.price.free-format:$0 USD}")
    private String freePriceFormat;

    @Value("${comparison.price.missing-format:N/A}")
    private String missingPriceFormat;

    /**
     * Enrich and persist up to `limit` games from the given list.
     * For each game, call appdetails (Steam API) and SteamSpy, wait 2s between each pair.
     * Si el juego ya existe en DB, usa esos datos sin hacer llamadas y continúa hasta completar el límite de NUEVOS enriquecimientos.
     */
    public java.util.Map<Long, java.util.List<String>> enrichAndStore(List<SteamUserGamesInput.GameInfo> games, int limit) {
        if (games == null || games.isEmpty()) return java.util.Collections.emptyMap();

        java.util.Map<Long, java.util.List<String>> result = new java.util.HashMap<>();

        int processedCount = 0; // Contador de juegos enriquecidos (llamadas nuevas)
        int i = 0;
        
        // Continuar hasta procesar 'limit' juegos nuevos o hasta agotar la lista
        while (processedCount < limit && i < games.size()) {
            SteamUserGamesInput.GameInfo game = games.get(i);
            i++;
            
            Long appId = game.getAppId();
            if (appId == null) continue;

            try {
                // Primero verificar si ya existe en la base de datos
                java.util.Optional<GameRecord> existingRecord = repo.findByAppId(appId);
                
                if (existingRecord.isPresent()) {
                    // Ya existe en DB, usar los datos guardados
                    GameRecord existing = existingRecord.get();
                    log.info("Juego appId={} ya existe en DB, usando datos guardados (sin contar en límite)", appId);
                    
                    // Actualizar los atributos del game con los datos de la DB para que se vean en la respuesta
                    game.setHeaderImage(existing.getHeaderImage());
                    game.setIsFree(existing.getIsFree());
                    game.setPrice(existing.getPrice());
                    // imgIconUrl ya debería venir del getOwnedGames, pero por si acaso:
                    if (game.getImgIconUrl() == null && existing.getImgIconUrl() != null) {
                        game.setImgIconUrl(existing.getImgIconUrl());
                    }
                    
                    // Agregar tags al resultado
                    result.put(appId, existing.getTags() != null ? existing.getTags() : new ArrayList<>());
                    
                    // No incrementamos processedCount, continuamos con el siguiente
                    continue;
                }
                
                // No existe en DB, proceder con las llamadas
                log.info("Enriqueciendo appId={} nombre={} ({}/{})", appId, game.getName(), processedCount + 1, limit);

                // Ejecutar appdetails y steamspy en paralelo
                @SuppressWarnings("unchecked")
                java.util.concurrent.CompletableFuture<java.util.Map<String, Object>> futureAppDetails =
                        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                            try {
                                Object ad = conectorClient.getAppDetails(String.valueOf(appId));
                                if (ad instanceof java.util.Map) return (java.util.Map<String, Object>) ad;
                                return java.util.Collections.emptyMap();
                            } catch (Exception ex) {
                                log.warn("Error appdetails for {}: {}", appId, ex.getMessage());
                                return java.util.Collections.emptyMap();
                            }
                        });

                java.util.concurrent.CompletableFuture<java.util.Map<String, Object>> futureSteamSpy =
                        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                            try {
                                return conectorClient.getSteamSpyAppDetails(String.valueOf(appId));
                            } catch (Exception ex) {
                                log.warn("Error steamspy for {}: {}", appId, ex.getMessage());
                                return java.util.Collections.emptyMap();
                            }
                        });

                java.util.concurrent.CompletableFuture.allOf(futureAppDetails, futureSteamSpy).join();

                java.util.Map<String, Object> appDetails = futureAppDetails.get();
                java.util.Map<String, Object> steamSpy = futureSteamSpy.get();

                // Detectar si appdetails falló (success: false)
                Boolean appdetailsFailed = false;
                if (appDetails != null && appDetails.containsKey("success")) {
                    Object successObj = appDetails.get("success");
                    if (successObj instanceof Boolean && !(Boolean) successObj) {
                        appdetailsFailed = true;
                        log.info("appdetails devolvió success=false para appId={}", appId);
                    }
                }

                // Parsear tags/genres de steamSpy
                List<String> tags = new ArrayList<>();
                if (steamSpy != null) {
                    Object tagObj = steamSpy.get("tags");
                    if (tagObj instanceof java.util.Map) {
                        java.util.Map<?,?> tmap = (java.util.Map<?,?>) tagObj;
                        for (Object k : tmap.keySet()) tags.add(String.valueOf(k));
                    }
                    // Parsear genre: puede venir como string con múltiples géneros separados por coma
                    Object genreObj = steamSpy.get("genre");
                    if (genreObj != null) {
                        String genreStr = String.valueOf(genreObj);
                        // Separar por comas y agregar cada género como tag individual
                        String[] genres = genreStr.split(",");
                        for (String genre : genres) {
                            String trimmedGenre = genre.trim();
                            if (!trimmedGenre.isEmpty()) {
                                tags.add(trimmedGenre);
                            }
                        }
                    }
                }

                // Parsear price, is_free, header_image desde appDetails (estructura depende del conector)
                Boolean isFree = null;
                String price = null;
                String headerImage = null;
                String detectedName = null;
                if (appDetails != null && !appDetails.isEmpty()) {
                    // El conector devuelve un map con structure similar a SteamAppDetailsResponseDto
                    // Intentamos encontrar keys comunes
                    java.util.Map<String, Object> data = null;
                    if (appDetails.containsKey("data") && appDetails.get("data") instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> tmp = (java.util.Map<String, Object>) appDetails.get("data");
                        data = tmp;
                    } else if (appDetails.containsKey(String.valueOf(appId)) && appDetails.get(String.valueOf(appId)) instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> wrapper = (java.util.Map<String, Object>) appDetails.get(String.valueOf(appId));
                        if (wrapper.get("data") instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> tmp2 = (java.util.Map<String, Object>) wrapper.get("data");
                            data = tmp2;
                        }
                    } else {
                        data = appDetails; // fallback
                    }

                    if (data != null) {
                        // name
                        Object nameObj = data.get("name");
                        if (nameObj != null) detectedName = String.valueOf(nameObj);

                        // header image
                        Object header = data.get("header_image");
                        if (header != null) headerImage = String.valueOf(header);

                        // price overview puede venir en price_overview
                        Object priceOverview = data.get("price_overview");
                        if (priceOverview instanceof java.util.Map) {
                            java.util.Map<?,?> pov = (java.util.Map<?,?>) priceOverview;
                            Object finalPrice = pov.get("final_formatted");
                            if (finalPrice != null) price = String.valueOf(finalPrice);
                        }

                        // is_free flag
                        Object isFreeObj = data.get("is_free");
                        if (isFreeObj instanceof Boolean) isFree = (Boolean) isFreeObj;
                        else if (isFreeObj != null) isFree = Boolean.valueOf(String.valueOf(isFreeObj));
                    }
                }
                // Fallback: si no se detectó nombre en appdetails, intentar con steamspy
                if (detectedName == null && steamSpy != null && steamSpy.get("name") != null) {
                    detectedName = String.valueOf(steamSpy.get("name"));
                }

                // Normalizar precio:
                // - Si is_free == true → usar freePriceFormat ("$0 USD")
                // - Si is_free == false y price existe → mantener el precio parseado y asegurar " USD" al final
                // - Si is_free == false y price es null → usar missingPriceFormat ("N/A")
                // - Si is_free == null → usar missingPriceFormat ("N/A")
                if (isFree == null) {
                    price = missingPriceFormat;
                } else if (isFree) {
                    price = freePriceFormat;
                } else {
                    // is_free == false (juego de pago)
                    if (price == null || price.trim().isEmpty()) {
                        price = missingPriceFormat;
                    } else {
                        // Asegurar que el precio tenga " USD" al final
                        if (!price.toUpperCase().contains("USD")) {
                            price = price + " USD";
                        }
                    }
                }

                // add to result map (store tags only here; other fields saved below)
                result.put(appId, tags);

                // Determinar el header_image final (preferir el del API, fallback al del game)
                String finalHeaderImage = headerImage != null ? headerImage : game.getHeaderImage();
                // Determinar el nombre final (preferir el detectado, fallback al del game)
                String finalName = (game.getName() != null && !game.getName().trim().isEmpty()) ? game.getName() : detectedName;

                // Solo guardar en DB si tiene header_image O si falló appdetails (para recordar que falló)
                if (finalHeaderImage != null && !finalHeaderImage.trim().isEmpty()) {
            GameRecord record = GameRecord.builder()
                            .appId(appId)
                .name(finalName)
                .headerImage(finalHeaderImage)
                .imgVertical("https://cdn.akamai.steamstatic.com/steam/apps/" + appId + "/library_600x900.jpg")
                            .imgIconUrl(game.getImgIconUrl())
                            .isFree(isFree)
                            .price(price)
                            .appdetailsFailed(appdetailsFailed)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                            .tags(tags)
                            .build();

                    // Guardar nuevo registro (sabemos que no existe porque ya lo verificamos)
                    repo.save(record);
                    
                    log.info("Juego appId={} guardado en DB con header_image", appId);
                } else if (appdetailsFailed) {
                    // Guardar también si falló para cachear que el juego no está disponible
                    GameRecord failedRecord = GameRecord.builder()
                            .appId(appId)
                            .name(finalName)
                            .imgIconUrl(game.getImgIconUrl())
                            .appdetailsFailed(true)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    repo.save(failedRecord);
                    log.info("Juego appId={} guardado en DB como FALLIDO (appdetails success=false)", appId);
                } else {
                    log.info("Juego appId={} NO guardado en DB (sin header_image y sin fallo detectado)", appId);
                }

                // Incrementar contador de procesados (independientemente de si se guardó o no)
                processedCount++;

                // esperamos 2 segundos después de completar el par de llamadas
                Thread.sleep(2000);

            } catch (Exception e) {
                log.warn("No se pudo enriquecer appId={}: {}", appId, e.getMessage());
                // Incrementar contador incluso si hubo error, para no quedarnos en loop infinito
                processedCount++;
            }
        }

        return result;
    }

    /**
     * Fuerza el refresh de un juego (llamadas a appdetails y steamspy) y hace upsert.
     * Si existe, actualiza campos y tags; si no, inserta (sólo si hay header_image).
     * Devuelve el registro actualizado/insertado o null si no pudo persistirse (por ejemplo, sin header_image).
     */
    public GameRecord refreshAndUpsert(Long appId, String nameHint, String imgIconHint) {
        if (appId == null) throw new IllegalArgumentException("appId requerido");

        try {
            // Ejecutar appdetails y steamspy en paralelo
            @SuppressWarnings("unchecked")
            java.util.concurrent.CompletableFuture<java.util.Map<String, Object>> futureAppDetails =
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        try {
                            Object ad = conectorClient.getAppDetails(String.valueOf(appId));
                            if (ad instanceof java.util.Map) return (java.util.Map<String, Object>) ad;
                            return java.util.Collections.emptyMap();
                        } catch (Exception ex) {
                            log.warn("Error appdetails for {}: {}", appId, ex.getMessage());
                            return java.util.Collections.emptyMap();
                        }
                    });

            java.util.concurrent.CompletableFuture<java.util.Map<String, Object>> futureSteamSpy =
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        try {
                            return conectorClient.getSteamSpyAppDetails(String.valueOf(appId));
                        } catch (Exception ex) {
                            log.warn("Error steamspy for {}: {}", appId, ex.getMessage());
                            return java.util.Collections.emptyMap();
                        }
                    });

            java.util.concurrent.CompletableFuture.allOf(futureAppDetails, futureSteamSpy).join();

            java.util.Map<String, Object> appDetails = futureAppDetails.get();
            java.util.Map<String, Object> steamSpy = futureSteamSpy.get();

            // Parsear tags/genres
            java.util.List<String> tags = new java.util.ArrayList<>();
            if (steamSpy != null) {
                Object tagObj = steamSpy.get("tags");
                if (tagObj instanceof java.util.Map) {
                    java.util.Map<?,?> tmap = (java.util.Map<?,?>) tagObj;
                    for (Object k : tmap.keySet()) tags.add(String.valueOf(k));
                }
                Object genreObj = steamSpy.get("genre");
                if (genreObj != null) {
                    String genreStr = String.valueOf(genreObj);
                    String[] genres = genreStr.split(",");
                    for (String genre : genres) {
                        String trimmedGenre = genre.trim();
                        if (!trimmedGenre.isEmpty()) tags.add(trimmedGenre);
                    }
                }
            }

            // Parsear datos principales desde appdetails
            Boolean isFree = null;
            String price = null;
            String headerImage = null;
            String detectedName = null;
            java.util.Map<String, Object> data = null;
            if (appDetails != null && !appDetails.isEmpty()) {
                if (appDetails.containsKey("data") && appDetails.get("data") instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> tmp = (java.util.Map<String, Object>) appDetails.get("data");
                    data = tmp;
                } else if (appDetails.containsKey(String.valueOf(appId)) && appDetails.get(String.valueOf(appId)) instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> wrapper = (java.util.Map<String, Object>) appDetails.get(String.valueOf(appId));
                    if (wrapper.get("data") instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> tmp2 = (java.util.Map<String, Object>) wrapper.get("data");
                        data = tmp2;
                    }
                } else {
                    data = appDetails; // fallback
                }
            }

            if (data != null) {
                Object nameObj = data.get("name");
                if (nameObj != null) detectedName = String.valueOf(nameObj);

                Object header = data.get("header_image");
                if (header != null) headerImage = String.valueOf(header);

                Object priceOverview = data.get("price_overview");
                if (priceOverview instanceof java.util.Map) {
                    java.util.Map<?,?> pov = (java.util.Map<?,?>) priceOverview;
                    Object finalPrice = pov.get("final_formatted");
                    if (finalPrice != null) price = String.valueOf(finalPrice);
                }

                Object isFreeObj = data.get("is_free");
                if (isFreeObj instanceof Boolean) isFree = (Boolean) isFreeObj;
                else if (isFreeObj != null) isFree = Boolean.valueOf(String.valueOf(isFreeObj));
            }

            if (detectedName == null && steamSpy != null && steamSpy.get("name") != null) {
                detectedName = String.valueOf(steamSpy.get("name"));
            }

            // Normalizar precio según flags
            if (isFree == null) {
                price = missingPriceFormat;
            } else if (isFree) {
                price = freePriceFormat;
            } else {
                if (price == null || price.trim().isEmpty()) price = missingPriceFormat;
                else if (!price.toUpperCase().contains("USD")) price = price + " USD";
            }

            // Determinar campos finales
            String finalHeaderImage = headerImage; // no hay hint para header en refresh
            String finalName = detectedName != null ? detectedName : nameHint;

            // Upsert
            java.util.Optional<GameRecord> opt = repo.findByAppId(appId);
            if (opt.isPresent()) {
                GameRecord existing = opt.get();
                // Si no hay header nuevo, usar el existente para no perder dato
                if (finalHeaderImage == null || finalHeaderImage.trim().isEmpty()) {
                    finalHeaderImage = existing.getHeaderImage();
                }
                // Nombre: preferir detectado, sino mantener existente
                if (finalName == null || finalName.trim().isEmpty()) finalName = existing.getName();
                // Icono: mantener existente si no tenemos hint
                String finalIcon = (imgIconHint != null && !imgIconHint.trim().isEmpty()) ? imgIconHint : existing.getImgIconUrl();

                existing.setName(finalName);
                existing.setHeaderImage(finalHeaderImage);
                existing.setImgVertical("https://cdn.akamai.steamstatic.com/steam/apps/" + appId + "/library_600x900.jpg");
                existing.setImgIconUrl(finalIcon);
                existing.setIsFree(isFree);
                // Actualizar precio con el valor obtenido del API (refrescar siempre)
                // Si la llamada no devolvió un precio válido, price contendrá el formato de missingPriceFormat
                existing.setPrice(price);
                existing.setTags(tags);
                existing.setUpdatedAt(java.time.Instant.now());

                return repo.save(existing);
            } else {
                // Insertar sólo si hay header_image
                if (finalHeaderImage == null || finalHeaderImage.trim().isEmpty()) {
                    log.info("Juego appId={} NO guardado en DB (sin header_image)", appId);
                    return null;
                }

                GameRecord record = GameRecord.builder()
                        .appId(appId)
                        .name(finalName)
                        .headerImage(finalHeaderImage)
                        .imgVertical("https://cdn.akamai.steamstatic.com/steam/apps/" + appId + "/library_600x900.jpg")
                        .imgIconUrl(imgIconHint)
                        .isFree(isFree)
                        .price(price)
                        .createdAt(java.time.Instant.now())
                        .updatedAt(java.time.Instant.now())
                        .tags(tags)
                        .build();

                GameRecord saved = repo.save(record);
                // rate limiting gentil similar al flujo batch
                try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                log.info("Juego appId={} guardado/actualizado en DB via refresh", appId);
                return saved;
            }

        } catch (Exception e) {
            log.warn("No se pudo refrescar appId={}: {}", appId, e.getMessage());
            return null;
        }
    }
}
