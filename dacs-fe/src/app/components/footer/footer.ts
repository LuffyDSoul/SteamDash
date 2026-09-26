import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UiBlockService } from '../../core/services/ui-block.service';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './footer.html',
  styleUrl: './footer.css'
})
export class Footer {
  year = new Date().getFullYear();
  loading = false;
  sent = false;
  failed = false;
  form: ReturnType<FormBuilder['group']>;
  uiBlock = inject(UiBlockService);

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      message: ['', [Validators.required, Validators.minLength(10)]],
    });
  }
  

  touched(ctrl: 'email'|'message') {
    const c = this.form.controls[ctrl];
    return c.touched || c.dirty;
    }
  submit() {
    this.sent = this.failed = false;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.loading = true;
    // Simulación de envío — acá conectarías tu endpoint
    setTimeout(() => {
      this.loading = false;
      // demo OK
      this.sent = true;
      this.form.reset();
    }, 700);
  }
}
