import { Component, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SearchService } from './search.service';

@Component({
  selector: 'app-root',
  imports: [FormsModule, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly searchService = inject(SearchService);

  protected searchQuery = signal('');
  protected results = signal<string[]>([]);
  protected isLoading = signal(false);
  protected errorMessage = signal<string | null>(null);

  protected onSearch(): void {
    const query = this.searchQuery().trim();

    if (!query) {
      this.results.set([]);
      this.errorMessage.set(null);
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.searchService.searchFiles(query).subscribe({
      next: (files) => {
        this.results.set(files);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Search error:', error);
        this.errorMessage.set('Failed to search files');
        this.isLoading.set(false);
        this.results.set([]);
      }
    });
  }

  protected onInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchQuery.set(input.value);
  }
}
