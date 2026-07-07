import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Book {
  id?: number;
  bookName: string;
  genre: string;
  author: string;
}

export interface Member {
  id?: number;
  name: string;
  email: string;
  age: number;
}

export interface BorrowRecord {
  id: number;
  book: Book;
  member: Member;
  borrowTime: string;
  returnTime: string | null;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private base = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  getBooks(): Observable<Book[]> {
    return this.http.get<Book[]>(`${this.base}/books`);
  }
  getBook(id: number): Observable<Book> {
    return this.http.get<Book>(`${this.base}/books/${id}`);
  }
  createBook(book: Book): Observable<Book> {
    return this.http.post<Book>(`${this.base}/books`, book);
  }

  deleteBook(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/books/${id}`);
  }

  getMembers(): Observable<Member[]> {
    return this.http.get<Member[]>(`${this.base}/members`);
  }
  createMember(member: Member): Observable<Member> {
    return this.http.post<Member>(`${this.base}/members`, member);
  }
  deleteMember(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/members/${id}`);
  }

  borrowBook(bookId: number, memberId: number): Observable<BorrowRecord> {
    return this.http.post<BorrowRecord>(`${this.base}/borrow?bookId=${bookId}&memberId=${memberId}`, {});
  }
  returnBook(borrowId: number): Observable<BorrowRecord> {
    return this.http.put<BorrowRecord>(`${this.base}/borrow/return/${borrowId}`, {});
  }
  getBorrowRecords(): Observable<BorrowRecord[]> {
    return this.http.get<BorrowRecord[]>(`${this.base}/borrow`);
  }
  deleteBorrow(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/borrow/${id}`);
  }

  slowQuery(): Observable<Book[]> {
    return this.http.get<Book[]>(`${this.base}/books/monitor/slow-query`);
  }
  errorQuery(): Observable<any> {
    return this.http.get<any>(`${this.base}/books/monitor/error-test`);
  }
}
