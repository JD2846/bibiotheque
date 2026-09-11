import { Component, ChangeDetectionStrategy } from '@angular/core';
import { HeaderComponent } from './header/header.component';
import { SidebarComponent } from './sidebar/sidebar.component';
import { RouterOutlet } from '@angular/router';

const SIDEBAR_COLLAPSED_KEY = 'sidebarCollapsed';
const DESKTOP_BREAKPOINT = 992;

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [HeaderComponent, SidebarComponent, RouterOutlet]
})
export class AppComponent {
  title = 'Library Management System';

  // Tiroir mobile (sidebar en superposition, ferme par defaut)
  sidebarOpen = false;

  // Sidebar reduite/agrandie sur desktop (etat memorise)
  sidebarCollapsed = localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === 'true';

  toggleSidebar(): void {
    if (window.innerWidth >= DESKTOP_BREAKPOINT) {
      this.sidebarCollapsed = !this.sidebarCollapsed;
      localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(this.sidebarCollapsed));
    } else {
      this.sidebarOpen = !this.sidebarOpen;
    }
  }

  closeSidebar(): void {
    this.sidebarOpen = false;
  }
}
