import { Injectable } from '@angular/core';

/**
 * Servicio compartido para filtrado de contenido adulto (Filtro Atlas)
 * Usado en biblioteca-usuario y estadísticas para mantener consistencia
 */
@Injectable({
  providedIn: 'root'
})
export class ContentFilterService {
  
  /**
   * Detecta si un juego es +18 según criterios del filtro Atlas
   * Incluye whitelist de excepciones y patrones de detección por tags/nombre
   */
  isAdultContent(name: string, tags: string[] = []): boolean {
    const nameLower = (name || '').toLowerCase();
    
    // Whitelist: Excepciones que NUNCA deben filtrarse
    if (nameLower.trim() === 'high on life') return false;
    if (nameLower.trim() === 'blender') return false;
    if (nameLower.trim() === 'wallpaper engine') return false;
    
    const baseName = nameLower.trim();
    if (baseName.startsWith('marvel rivals')) return false;
    if (baseName.startsWith('muse dash')) return false;
    if (baseName.startsWith('battlefield')) return false; // Battlefield 1, 4, V, etc.
    
    const tset = new Set((tags || []).map(t => (t || '').toLowerCase()));
    
    // Criterio 1: Juegos con tags "Hentai" o "NSFW"
    if (tset.has('hentai') || tset.has('nsfw')) {
      return true;
    }
    
    // Criterio 2: Reglas por nombre (palabras clave y variantes)
    const namePatterns: RegExp[] = [
      /69/i,
      /\bsex\b/i,
      /hentai/i,
      /benefit/i,
      /lewd/i,
      /faulty/i,
      /\bdik\b/i,
      /fetish/i,
      /ecchi/i,
      /\bnude\b/i,
      /\badult\b/i,
      /xxx/i,
      /erotic/i,
      /sexy/i,
      /18\+|18plus/i,
      /nsfw\+?|\bnsfw\b/i,
      /waifu/i,
      /\btits?\b/i,
      /\bsucc(ubus|uby)?\b/i,
      /yandere/i,
      /otome/i,
      /girlfriend/i,
      /boyfriend/i,
      /ussy/i,
      /porn/i,
      /fuck/i,
      /cum/i,
      /h3ntai/i,
      /s3x/i,
      /s\W*x/i
    ];
    if (namePatterns.some(rx => rx.test(name))) return true;
    
    return false;
  }
}
