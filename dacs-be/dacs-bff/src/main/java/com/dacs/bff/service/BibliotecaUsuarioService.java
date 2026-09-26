package com.dacs.bff.service;

import com.dacs.bff.dto.BibliotecaUsuarioDto;
import com.dacs.bff.dto.GameAchievementsDto;
import com.dacs.bff.dto.JuegoAleatorioDto;
import com.dacs.bff.dto.RefreshAllGamesResponseDto;
import com.dacs.bff.dto.RefreshJuegoResponseDto;
import com.dacs.bff.dto.SyncBibliotecaResponseDto;

/**
 * Servicio para gestionar la biblioteca de juegos de un usuario
 */
public interface BibliotecaUsuarioService {
    
    /**
     * Obtiene la biblioteca de juegos de un usuario
     * @param steamId Steam ID del usuario
     * @param refresh Si es true, fuerza actualización desde Steam
     * @return Biblioteca del usuario con su lista de juegos
     */
    BibliotecaUsuarioDto getBibliotecaUsuario(String steamId, boolean refresh);
    
    /**
     * Actualiza la información de un juego específico en la base de datos
     * Llama a appdetails de Steam, steamspy y obtiene la imagen de biblioteca
     * @param steamId Steam ID del usuario
     * @param appId App ID del juego
     * @return Respuesta con el resultado del refresh
     */
    RefreshJuegoResponseDto refreshJuego(String steamId, Integer appId);
    
    /**
     * Obtiene un juego aleatorio de la biblioteca del usuario
     * Filtra juegos no jugados o con menos de maxHoras de juego
     * Enriquece la información con AppDetails de Steam
     * @param steamId Steam ID del usuario
     * @param maxHoras Horas máximas jugadas (null para juegos no jugados)
     * @return Juego aleatorio con información completa
     */
    JuegoAleatorioDto getJuegoAleatorio(String steamId, Integer maxHoras, String tags, Long excludeAppId);
    
    /**
     * Obtiene los logros de un juego para un usuario específico
     * Combina información de logros del jugador con el esquema del juego
     * @param steamId Steam ID del usuario
     * @param appId App ID del juego
     * @return Logros del juego con información completa
     */
    GameAchievementsDto getGameAchievements(String steamId, Long appId);
    
    /**
     * Refresca forzadamente los logros de un juego desde Steam API y los guarda en BD
     * @param steamId Steam ID del usuario
     * @param appId App ID del juego
     * @return Logros actualizados del juego
     */
    GameAchievementsDto refreshGameAchievements(String steamId, Long appId);
    
    /**
     * Sincroniza toda la biblioteca del usuario con la base de datos
     * Obtiene los juegos del usuario desde Steam y enriquece aquellos que no estén en DB
     * con llamadas a appdetails y steamspy, esperando 2 segundos entre cada par de llamadas
     * @param steamId Steam ID del usuario
     * @return Resultado de la sincronización con contadores
     */
    SyncBibliotecaResponseDto syncBiblioteca(String steamId);

    /**
     * Actualiza TODOS los juegos de la biblioteca del usuario
     * Llama a refresh para cada juego para actualizar precios, imágenes, tags, logros
     * ADVERTENCIA: Operación larga que puede tardar varias horas con bibliotecas grandes
     * Espera 2 segundos entre cada juego (Steam API + SteamSpy + imagen)
     * @param steamId Steam ID del usuario
     * @return Resultado de la actualización con contadores
     */
    RefreshAllGamesResponseDto refreshAllGames(String steamId);
}

