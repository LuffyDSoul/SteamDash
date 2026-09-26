import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Utilidades para validación de formularios
 */
export class CustomValidators {
  /**
   * Valida que el campo no esté vacío (solo espacios)
   */
  static noWhitespace(control: AbstractControl): ValidationErrors | null {
    if (control.value && control.value.trim().length === 0) {
      return { whitespace: true };
    }
    return null;
  }

  /**
   * Valida formato de email
   */
  static email(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    return emailRegex.test(control.value) ? null : { email: true };
  }

  /**
   * Valida formato de teléfono español
   */
  static phoneSpain(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    
    const phoneRegex = /^[6-9]\d{8}$/;
    return phoneRegex.test(control.value.replace(/\s/g, '')) ? null : { phoneSpain: true };
  }

  /**
   * Valida que dos campos sean iguales
   */
  static matchFields(field1: string, field2: string): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value1 = control.get(field1)?.value;
      const value2 = control.get(field2)?.value;
      
      return value1 === value2 ? null : { matchFields: true };
    };
  }

  /**
   * Valida fortaleza de contraseña
   */
  static passwordStrength(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    
    const password = control.value;
    const errors: any = {};
    
    if (password.length < 8) {
      errors.minLength = true;
    }
    
    if (!/[A-Z]/.test(password)) {
      errors.uppercase = true;
    }
    
    if (!/[a-z]/.test(password)) {
      errors.lowercase = true;
    }
    
    if (!/\d/.test(password)) {
      errors.number = true;
    }
    
    if (!/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
      errors.specialChar = true;
    }
    
    return Object.keys(errors).length > 0 ? errors : null;
  }

  /**
   * Valida formato de DNI español
   */
  static dniSpain(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    
    const dni = control.value.toUpperCase().replace(/\s/g, '');
    const dniRegex = /^[0-9]{8}[TRWAGMYFPDXBNJZSQVHLCKE]$/;
    
    if (!dniRegex.test(dni)) {
      return { dniSpain: true };
    }
    
    const numbers = dni.substring(0, 8);
    const letter = dni.substring(8, 9);
    const letters = 'TRWAGMYFPDXBNJZSQVHLCKE';
    const expectedLetter = letters[parseInt(numbers) % 23];
    
    return letter === expectedLetter ? null : { dniSpain: true };
  }

  /**
   * Valida formato de NIE español
   */
  static nieSpain(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    
    const nie = control.value.toUpperCase().replace(/\s/g, '');
    const nieRegex = /^[XYZ][0-9]{7}[TRWAGMYFPDXBNJZSQVHLCKE]$/;
    
    if (!nieRegex.test(nie)) {
      return { nieSpain: true };
    }
    
    const firstLetter = nie.substring(0, 1);
    const numbers = nie.substring(1, 8);
    const letter = nie.substring(8, 9);
    const letters = 'TRWAGMYFPDXBNJZSQVHLCKE';
    
    let numberValue = parseInt(numbers);
    if (firstLetter === 'Y') numberValue += 10000000;
    if (firstLetter === 'Z') numberValue += 20000000;
    
    const expectedLetter = letters[numberValue % 23];
    
    return letter === expectedLetter ? null : { nieSpain: true };
  }

  /**
   * Valida que el valor esté en un rango numérico
   */
  static range(min: number, max: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      
      const value = Number(control.value);
      return (value >= min && value <= max) ? null : { range: { min, max, actual: value } };
    };
  }

  /**
   * Valida que el valor tenga una longitud específica
   */
  static exactLength(length: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      
      return control.value.length === length ? null : { exactLength: { required: length, actual: control.value.length } };
    };
  }

  /**
   * Valida que el valor contenga solo letras
   */
  static onlyLetters(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    
    const lettersRegex = /^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s]+$/;
    return lettersRegex.test(control.value) ? null : { onlyLetters: true };
  }

  /**
   * Valida que el valor contenga solo números
   */
  static onlyNumbers(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    
    const numbersRegex = /^\d+$/;
    return numbersRegex.test(control.value) ? null : { onlyNumbers: true };
  }
}
