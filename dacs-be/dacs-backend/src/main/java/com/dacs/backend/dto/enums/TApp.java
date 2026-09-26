package com.dacs.backend.dto.enums;

/**
 * Enum para los tipos de aplicación
 */
public enum TApp {
    JUEGO("Juego"),
    MUSICA("Música"),
    DLC("Downloadable Content"),
    CORTOMETRAJE("Cortometraje");
    
    private final String descripcion;
    
    TApp(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
}