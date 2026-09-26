package com.dacs.backend.service;

import com.dacs.backend.dto.BibliotecaComparacionDto;
import com.dacs.backend.dto.SteamUserGamesInput;
import java.util.List;

/**
 * Servicio para comparación de bibliotecas de Steam entre usuarios
 */
public interface BibliotecaComparacionService {
    
    /**
     * Compara las bibliotecas de juegos de múltiples usuarios de Steam (2-6 usuarios)
     * 
     * @param usuarios Lista de usuarios (mínimo 2, máximo 6)
     * @return DTO con la comparación de bibliotecas
     */
    BibliotecaComparacionDto compararBibliotecas(List<SteamUserGamesInput> usuarios);
}
