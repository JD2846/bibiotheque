import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { ReservationRoutingModule } from './reservation-routing.module';
import { ReservationContainerComponent } from './components/reservation-container/reservation-container.component';
import { ReservationListComponent } from './components/reservation-list/reservation-list.component';
import { ReservationFormComponent } from './components/reservation-form/reservation-form.component';

@NgModule({
  declarations: [
    ReservationContainerComponent,
    ReservationListComponent,
    ReservationFormComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ReservationRoutingModule
  ]
})
export class ReservationModule { }
