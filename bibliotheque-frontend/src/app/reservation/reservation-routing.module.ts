import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ReservationContainerComponent } from './components/reservation-container/reservation-container.component';

const routes: Routes = [
  { path: '', component: ReservationContainerComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ReservationRoutingModule { }
