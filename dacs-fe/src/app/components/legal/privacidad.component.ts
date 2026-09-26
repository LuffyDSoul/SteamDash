import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-privacidad',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="legal-container">
      <div class="legal-content">
        <button class="back-button" routerLink="/">
          ← Volver al inicio
        </button>
        
        <h1>Privacidad</h1>
        
        <section>
          <p>
            El presente proyecto fue desarrollado exclusivamente para la cátedra 
            <strong>Desarrollo de Aplicaciones Cliente-Servidor (DACS) – Edición 2025</strong>, 
            con fines académicos, demostrativos e investigativos. SteamDash no recopila datos 
            personales sensibles ni comercializa ningún tipo de información del usuario.
          </p>
        </section>

        <section>
          <p>
            Los datos obtenidos mediante la integración con la API pública de Steam Web API 
            (incluyendo, entre otros: biblioteca de juegos, tiempos de juego, perfiles públicos 
            y noticias) se utilizan únicamente para mostrar funcionalidades demostrativas dentro 
            del proyecto. Toda la información mostrada depende de las configuraciones de privacidad 
            del usuario en Steam y sigue los lineamientos establecidos por Valve Corporation.
          </p>
        </section>

        <section>
          <p>
            SteamDash no almacena ni comparte información proveniente de la API más allá de lo 
            necesario para el correcto funcionamiento del prototipo académico. Las imágenes, 
            logotipos, nombres de juegos y demás materiales asociados a Steam o Valve son propiedad 
            de sus respectivos dueños y se utilizan únicamente con fines educativos, bajo el uso 
            permitido del contenido expuesto por la API pública.
          </p>
        </section>

        <section>
          <p>
            Los integrantes del equipo figuran únicamente como responsables del desarrollo académico 
            del proyecto, sin relación oficial con Valve Corporation.
          </p>
        </section>
      </div>
    </div>
  `,
  styles: [`
    .legal-container {
      min-height: 100vh;
      background: linear-gradient(135deg, #0a0e27 0%, #1a1f3a 100%);
      padding: 2rem;
      display: flex;
      justify-content: center;
      align-items: flex-start;
      padding-top: 4rem;
    }

    .legal-content {
      max-width: 800px;
      width: 100%;
      background: rgba(15, 20, 45, 0.8);
      backdrop-filter: blur(10px);
      border-radius: 16px;
      padding: 3rem;
      border: 1px solid rgba(59, 130, 246, 0.2);
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
    }

    .back-button {
      background: rgba(59, 130, 246, 0.1);
      border: 1px solid rgba(59, 130, 246, 0.3);
      color: #93c5fd;
      padding: 0.5rem 1rem;
      border-radius: 8px;
      cursor: pointer;
      font-size: 0.9rem;
      transition: all 0.3s;
      margin-bottom: 2rem;
      display: inline-block;
      text-decoration: none;
    }

    .back-button:hover {
      background: rgba(59, 130, 246, 0.2);
      border-color: rgba(59, 130, 246, 0.5);
      transform: translateX(-4px);
    }

    h1 {
      color: #ffffff;
      font-size: 2.5rem;
      margin-bottom: 2rem;
      background: linear-gradient(135deg, #60a5fa 0%, #a78bfa 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    section {
      margin-bottom: 1.5rem;
    }

    p {
      color: #d1d5db;
      line-height: 1.8;
      font-size: 1rem;
      margin: 0;
    }

    strong {
      color: #93c5fd;
      font-weight: 600;
    }

    @media (max-width: 768px) {
      .legal-container {
        padding: 1rem;
        padding-top: 2rem;
      }

      .legal-content {
        padding: 2rem;
      }

      h1 {
        font-size: 2rem;
      }
    }
  `]
})
export class PrivacidadComponent {}
