import {Component, OnInit} from '@angular/core';
import { CommonModule, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../services/admin-api.service';
import { AdminCardComponent } from '../../shared/admin-card/admin-card.component';

const MOCK_REVIEWS = [
  {
    id: 1,
    productName: 'iPhone 15 Pro',
    userEmail: 'john.doe@gmail.com',
    rating: 5,
    comment: 'Amazing phone, super fast and great camera!',
    status: 'APPROVED'
  },
  {
    id: 2,
    productName: 'Sony WH-1000XM5',
    userEmail: 'sara.khan@yahoo.com',
    rating: 4,
    comment: 'Sound quality is excellent but a bit expensive.',
    status: 'PENDING'
  },
  {
    id: 3,
    productName: 'Gaming Mechanical Keyboard',
    userEmail: 'alex123@gmail.com',
    rating: 2,
    comment: 'Keys stopped working after 2 weeks.',
    status: 'REPORTED'
  },
  {
    id: 4,
    productName: 'Apple Watch Series 9',
    userEmail: 'mike.ross@mail.com',
    rating: 5,
    comment: 'Love the health tracking features!',
    status: 'APPROVED'
  },
  {
    id: 5,
    productName: 'Samsung Galaxy S24',
    userEmail: 'emma.wilson@mail.com',
    rating: 3,
    comment: 'Battery life could be better.',
    status: 'PENDING'
  }
];


@Component({
  selector: 'app-reviews-list',
  standalone: true,
  imports: [CommonModule, FormsModule, NgFor, NgIf, AdminCardComponent],
  templateUrl: './reviews-list.component.html',
  styleUrls: ['./reviews-list.component.css']
})
export class ReviewsListComponent implements OnInit {

  reviews: any[] = [];
  statusFilter = '';

  constructor(private adminApi: AdminApiService) {}

  ngOnInit() {
    this.reviews = MOCK_REVIEWS; // show mock instantly
    this.loadReviews();          // try real API
  }


  loadReviews() {
    this.adminApi.getReviews().subscribe(data => this.reviews = data);
  }

  filteredReviews() {
    return this.reviews.filter(r =>
      this.statusFilter ? r.status === this.statusFilter : true
    );
  }

  approve(id: number) {
    this.adminApi.approveReview(id).subscribe(() => this.loadReviews());
  }

  deleteReview(id: number) {
    if (confirm('Delete this review?')) {
      this.adminApi.deleteReview(id).subscribe(() => this.loadReviews());
    }
  }
}
