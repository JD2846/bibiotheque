import { ChangeDetectorRef, Component, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { HeaderComponent } from './header/header.component';
import { SidebarComponent } from './sidebar/sidebar.component';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';

const SIDEBAR_COLLAPSED_KEY = 'sidebarCollapsed';
const DESKTOP_BREAKPOINT = 992;

// Routes affichees "plein ecran", sans navbar ni sidebar.
const NO_CHROME_PATHS = ['/login'];

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [HeaderComponent, SidebarComponent, RouterOutlet]
})
export class AppComponent implements OnInit {
  title = 'Library Management System';

  // Tiroir mobile (sidebar en superposition, ferme par defaut)
  sidebarOpen = false;

  // Sidebar reduite/agrandie sur desktop (etat memorise)
  sidebarCollapsed = localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === 'true';

  // Masque navbar/sidebar sur les pages "plein ecran" (ex: connexion)
  hideChrome = false;

  constructor(private router: Router, private cdr: ChangeDetectorRef) {
    // Calcule immediatement depuis l'URL du navigateur (et non router.url,
    // pas encore resolu a ce stade du bootstrap) pour eviter tout aleas de
    // timing avec l'evenement NavigationEnd lors du tout premier rendu.
    this.hideChrome = this.computeHideChrome(window.location.pathname);
  }

  ngOnInit(): void {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event) => {
      this.hideChrome = this.computeHideChrome((event as NavigationEnd).urlAfterRedirects);
      this.cdr.detectChanges();
    });
  }

  private computeHideChrome(url: string): boolean {
    const path = url.split(/[?#]/)[0];
    return NO_CHROME_PATHS.some(p => path === p || path.startsWith(p + '/'));
  }

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
