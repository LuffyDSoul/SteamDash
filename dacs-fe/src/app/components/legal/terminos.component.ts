import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-terminos',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="legal-container">
      <div class="legal-content">
        <button class="back-button" routerLink="/">
          ← Volver al inicio
        </button>
        
        <h1>Términos y Condiciones</h1>
        
        <section>
          <p>
            SteamDash es un proyecto creado para la cátedra 
            <strong>Desarrollo de Aplicaciones Cliente-Servidor (DACS) – 2025</strong>, 
            y todas sus funcionalidades tienen fines exclusivamente académicos y de investigación. 
            El sitio no constituye un producto comercial ni una plataforma oficial de Steam o Valve.
          </p>
        </section>

        <section>
          <p>
            Toda la información obtenida proviene de la Steam Web API, un servicio público que 
            permite acceder a datos expuestos por Valve. SteamDash no modifica, reproduce ni 
            redistribuye dicho contenido fuera del alcance permitido por la API. Las imágenes, 
            nombres de juegos, marcas registradas y cualquier otro material relacionado a Steam 
            son propiedad exclusiva de Valve Corporation y/o sus respectivos titulares.
          </p>
        </section>

        <section>
          <p>
            El uso de la plataforma es meramente demostrativo. Los usuarios que ingresen su 
            SteamID comprenden que SteamDash accede únicamente a los datos habilitados por la 
            configuración pública del perfil en Steam. No se almacena información privada ni se 
            solicita credenciales de acceso.
          </p>
        </section>

        <section>
          <p>
            Los integrantes del proyecto son responsables únicamente del desarrollo académico y 
            no poseen vínculo comercial o institucional con Valve Corporation. El uso del sistema 
            implica la aceptación de estos términos.
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
export class TerminosComponent {}
