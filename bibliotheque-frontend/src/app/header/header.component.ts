import { Component, HostListener, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { UserAuthService } from '../_service/user-auth.service';

@Component({
    selector: 'app-header',
    templateUrl: './header.component.html',
    styleUrls: ['./header.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class HeaderComponent {
  isCollapsed = true;
  isDropdownOpen = false;

  constructor(
    private userAuthService: UserAuthService,
    private router: Router
  ) { }

  toggleMenu(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  closeMenu(): void {
    this.isCollapsed = true;
    this.isDropdownOpen = false;
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

  isAdmin(): boolean {
    const roles: any[] = this.userAuthService.getRoles() || [];
    return roles.some(role => role?.roleName === 'Admin' || role === 'Admin');
  }

  getUserName(): string {
    return this.userAuthService.getName() || 'Utilisateur';
  }

  logout(): void {
    this.userAuthService.clear();
    this.closeMenu();
    this.router.navigate(['/login']);
  }
}
