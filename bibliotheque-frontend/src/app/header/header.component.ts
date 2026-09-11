import { Component, EventEmitter, HostListener, Output, ChangeDetectionStrategy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { UserAuthService } from '../_service/user-auth.service';
import { NgClass } from '@angular/common';

@Component({
    selector: 'app-header',
    templateUrl: './header.component.html',
    styleUrls: ['./header.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [RouterLink, NgClass]
})
export class HeaderComponent {
  @Output() menuToggle = new EventEmitter<void>();

  isDropdownOpen = false;

  constructor(
    private userAuthService: UserAuthService,
    private router: Router
  ) { }

  onMenuToggle(): void {
    this.menuToggle.emit();
  }

  toggleDropdown(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.dropdown')) {
      this.isDropdownOpen = false;
    }
  }

  isLoggedIn(): boolean {
    return !!this.userAuthService.isLoggedIn();
  }

  getUserName(): string {
    return this.userAuthService.getName() || 'Utilisateur';
  }

  getUserInitial(): string {
    const name = this.getUserName();
    return name.charAt(0).toUpperCase();
  }

  logout(): void {
    this.userAuthService.clear();
    this.isDropdownOpen = false;
    this.router.navigate(['/login']);
  }
}
