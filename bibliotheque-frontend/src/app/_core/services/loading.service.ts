import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class LoadingService {
  private readonly loadingStates = new Map<string, boolean>();
  private readonly loadingSubject = new BehaviorSubject<boolean>(false);

  startLoading(key: string): void {
    this.loadingStates.set(key, true);
    this.refreshLoadingState();
  }

  stopLoading(key: string): void {
    this.loadingStates.set(key, false);
    this.refreshLoadingState();
  }

  isLoading(): Observable<boolean> {
    return this.loadingSubject.asObservable();
  }

  reset(): void {
    this.loadingStates.clear();
    this.loadingSubject.next(false);
  }

  private refreshLoadingState(): void {
    this.loadingSubject.next(Array.from(this.loadingStates.values()).some(Boolean));
  }
}
