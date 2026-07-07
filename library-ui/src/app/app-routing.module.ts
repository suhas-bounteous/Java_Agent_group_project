import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BooksComponent } from './components/books/books.component';
import { MembersComponent } from './components/members/members.component';
import { BorrowComponent } from './components/borrow/borrow.component';
import { MonitorComponent } from './components/monitor/monitor.component';

const routes: Routes = [
  { path: '', redirectTo: 'books', pathMatch: 'full' },
  { path: 'books', component: BooksComponent },
  { path: 'members', component: MembersComponent },
  { path: 'borrow', component: BorrowComponent },
  { path: 'monitor', component: MonitorComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
