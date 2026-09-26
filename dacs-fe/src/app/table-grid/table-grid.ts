import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

interface TableData {
  id: number;
  name: string;
  age: number;
  city: string;
  email: string;
  department: string;
  status: string;
}

@Component({
  selector: 'app-table-grid',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './table-grid.html',
  styleUrls: ['./table-grid.css']
})
export class TableGridComponent implements OnInit {
  viewMode: 'table' | 'grid' = 'table';
  tableData: TableData[] = [];
  filteredData: TableData[] = [];
  searchTerm: string = '';

  ngOnInit() {
    this.loadSampleData();
  }

  loadSampleData() {
    this.tableData = [
      { id: 1, name: 'Juan Pérez', age: 30, city: 'Madrid', email: 'juan@email.com', department: 'IT', status: 'Activo' },
      { id: 2, name: 'María García', age: 25, city: 'Barcelona', email: 'maria@email.com', department: 'HR', status: 'Activo' },
      { id: 3, name: 'Carlos López', age: 35, city: 'Valencia', email: 'carlos@email.com', department: 'Finance', status: 'Inactivo' },
      { id: 4, name: 'Ana Martín', age: 28, city: 'Sevilla', email: 'ana@email.com', department: 'Marketing', status: 'Activo' },
      { id: 5, name: 'David Ruiz', age: 32, city: 'Bilbao', email: 'david@email.com', department: 'IT', status: 'Activo' },
      { id: 6, name: 'Laura Sánchez', age: 27, city: 'Málaga', email: 'laura@email.com', department: 'Sales', status: 'Inactivo' },
      { id: 7, name: 'Miguel Torres', age: 29, city: 'Zaragoza', email: 'miguel@email.com', department: 'Finance', status: 'Activo' },
      { id: 8, name: 'Elena Díaz', age: 31, city: 'Murcia', email: 'elena@email.com', department: 'HR', status: 'Activo' }
    ];
    this.filteredData = [...this.tableData];
  }

  toggleView() {
    this.viewMode = this.viewMode === 'table' ? 'grid' : 'table';
  }

  onSearch() {
    if (!this.searchTerm.trim()) {
      this.filteredData = [...this.tableData];
    } else {
      this.filteredData = this.tableData.filter(item =>
        item.name.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        item.email.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        item.city.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        item.department.toLowerCase().includes(this.searchTerm.toLowerCase())
      );
    }
  }

  clearSearch() {
    this.searchTerm = '';
    this.filteredData = [...this.tableData];
  }

  getStatusClass(status: string): string {
    return status === 'Activo' ? 'status-active' : 'status-inactive';
  }
}
