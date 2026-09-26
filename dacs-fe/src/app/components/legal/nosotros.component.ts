import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-nosotros',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="legal-container">
      <div class="legal-content">       
        
        <h1>Sobre Nosotros</h1>
        
        <section class="intro">
          <p>
            SteamDash es un proyecto desarrollado por estudiantes de la 
            <strong>Universidad Tecnológica Nacional</strong> para la asignatura 
            <strong>Desarrollo de Aplicaciones Cliente-Servidor (DACS) – 2025</strong>.
          </p>
          <p>
            Nuestro objetivo es crear una plataforma moderna y funcional que permita a los 
            usuarios de Steam gestionar y visualizar su biblioteca de juegos, comparar estadísticas 
            con amigos y mantenerse al día con las últimas noticias.
          </p>
        </section>

        <h2>El Equipo</h2>
        
        <div class="team-grid">
          <div class="team-member">
            <div class="member-avatar">-</div>
            <h3>Leal, Pablo</h3>
            <p class="role">Estudiante de Ing. en Sistemas</p>
            <a href="mailto:pablo.leal224@gmail.com" class="email">📧 pablo.leal224@gmail.com</a>
          </div>

          <div class="team-member">
            <div class="member-avatar">-</div>
            <h3>Guiffrey, Leandro</h3>
            <p class="role">Estudiante de Ing. en Sistemas</p>
            <a href="mailto:lguiffrey@gmail.com" class="email">📧 lguiffrey@gmail.com</a>
          </div>

          <div class="team-member">
            <div class="member-avatar">-</div>
            <h3>Cárcamo Tommasi, Lucía</h3>
            <p class="role">Estudiante de Ing. en Sistemas</p>
            <a href="mailto:luciactommasi@gmail.com" class="email">📧 luciactommasi@gmail.com</a>
          </div>
        </div>
        
        <section class="tech-stack">
          <h2>Tecnologías Utilizadas</h2>
          <div class="tech-list">
            <span class="tech-badge">Angular 20</span>
            <span class="tech-badge">TypeScript</span>   
            <span class="tech-badge">Java 21</span>
            <span class="tech-badge">Spring Boot</span>
            <span class="tech-badge">Keycloak</span>
            <span class="tech-badge">Nginx</span>
            <span class="tech-badge">PostgreSQL</span>
            <span class="tech-badge">Steam Web API</span>  
            <span class="tech-badge">SteamSPY API</span>                     
          </div>
        </section>

        <button class="back-button" routerLink="/">
          ← Volver al inicio
        </button>

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
      max-width: 900px;
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
      display: flex;
      margin-left: auto;
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

    h2 {
      color: #ffffff;
      font-size: 1.8rem;
      margin: 2.5rem 0 1.5rem 0;
      padding-bottom: 0.5rem;
      border-bottom: 2px solid rgba(59, 130, 246, 0.3);
    }

    h3 {
      color: #93c5fd;
      font-size: 1.2rem;
      margin: 0.5rem 0;
    }

    section {
      margin-bottom: 2rem;
    }

    .intro p {
      color: #d1d5db;
      line-height: 1.8;
      font-size: 1rem;
      margin-bottom: 1rem;
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

    .team-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 1.5rem;
      margin-top: 2rem;
    }

    .team-member {
      background: rgba(31, 41, 55, 0.5);
      border: 1px solid rgba(59, 130, 246, 0.2);
      border-radius: 12px;
      padding: 2rem;
      text-align: center;
      transition: all 0.3s;
    }

    .team-member:hover {
      transform: translateY(-4px);
      border-color: rgba(59, 130, 246, 0.4);
      box-shadow: 0 8px 24px rgba(59, 130, 246, 0.2);
    }

    .member-avatar {
      font-size: 4rem;
      margin-bottom: 1rem;
      filter: grayscale(0.3);
    }

    .role {
      color: #9ca3af;
      font-size: 0.9rem;
      font-style: italic;
      margin: 0.5rem 0 1rem 0;
    }

    .email {
      display: inline-block;
      color: #60a5fa;
      text-decoration: none;
      font-size: 0.9rem;
      padding: 0.5rem 1rem;
      background: rgba(59, 130, 246, 0.1);
      border-radius: 6px;
      transition: all 0.3s;
      word-break: break-word;
      max-width: 100%;
    }

    .email:hover {
      background: rgba(59, 130, 246, 0.2);
      transform: scale(1.05);
    }

    .tech-stack {
      margin-top: 3rem;
    }

    /* Breakout full-width section for tech stack */
    .tech-stack-full {
      width: 100vw;
      position: relative;
      left: 50%;
      margin-left: -50vw;
      padding: 3rem 4rem;
      background: linear-gradient(135deg, rgba(25,35,70,0.6) 0%, rgba(45,55,95,0.5) 100%);
      border-top: 1px solid rgba(59,130,246,0.25);
      border-bottom: 1px solid rgba(139,92,246,0.25);
      backdrop-filter: blur(6px);
    }

    .tech-list {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
      gap: 1rem;
      margin-top: 1.25rem;
      width: 100%;
      align-items: stretch;
    }

    .tech-badge {
      background: linear-gradient(135deg, rgba(139, 92, 246, 0.2) 0%, rgba(59, 130, 246, 0.2) 100%);
      border: 1px solid rgba(139, 92, 246, 0.3);
      color: #c4b5fd;
      padding: 0.75rem 0.75rem;
      border-radius: 14px;
      font-size: 0.85rem;
      font-weight: 600;
      display: flex;
      justify-content: center;
      align-items: center;
      letter-spacing: .5px;
      text-transform: uppercase;
      box-shadow: inset 0 0 0 1px rgba(139,92,246,0.25);
      transition: all 0.25s ease;
    }

    .tech-badge:hover {
      transform: translateY(-3px) scale(1.04);
      box-shadow: 0 6px 16px rgba(139, 92, 246, 0.35);
      border-color: rgba(139, 92, 246, 0.5);
    }

   
    @media (max-width: 768px) {
      .legal-container {
        padding: 1rem;
        padding-top: 2rem;
      }

      .legal-content {
        padding: 2rem;
      }

      .tech-stack-full {
        padding: 2rem 1.25rem;
      }

      h1 {
        font-size: 2rem;
      }

      h2 {
        font-size: 1.5rem;
      }

      .team-grid {
        grid-template-columns: 1fr;
      }

      .contact-email {
        font-size: 1rem;
        padding: 0.75rem 1.5rem;
      }
    }
  `]
})
export class NosotrosComponent {}
