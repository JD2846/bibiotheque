import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { UserAuthService } from '../_service/user-auth.service';

@Component({
    selector: 'app-sidebar',
    templateUrl: './sidebar.component.html',
    styleUrls: ['./sidebar.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [RouterLink, RouterLinkActive]
})
export class SidebarComponent {
  @Input() isOpen = false;
  @Output() navigate = new EventEmitter<void>();

  constructor(private userAuthService: UserAuthService) { }

  isLoggedIn(): boolean {
    return !!this.userAuthService.isLoggedIn();
  }

  isAdmin(): boolean {
    const roles: any[] = this.userAuthService.getRoles() || [];
    return roles.some(role => role?.roleName === 'Admin' || role === 'Admin');
  }

  onNavigate(): void {
    this.navigate.emit();
  }
}
