import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { BorrowRoutingModule } from './borrow-routing.module';
import { BorrowBookComponent } from './components/borrow-book/borrow-book.component';
import { ReturnBookComponent } from './components/return-book/return-book.component';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        RouterModule,
        BorrowRoutingModule,
        BorrowBookComponent,
        ReturnBookComponent
    ]
})
export class BorrowModule { }
