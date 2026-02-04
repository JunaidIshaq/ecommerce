import { Component } from '@angular/core';
import { CommonModule, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../services/admin-api.service';
import { AdminCardComponent } from '../../shared/admin-card/admin-card.component';

@Component({
  selector: 'app-reviews-list',
  standalone: true,
  imports: [CommonModule, FormsModule, NgFor, NgIf, AdminCardComponent],
  templateUrl: './reviews-list.component.html',
  styleUrls: ['./reviews-list.component.css']
})
export class ReviewsListComponent {

  reviews: any[] = [];
  statusFilter = '';

  constructor(private adminApi: AdminApiService) {}

  ngOnInit() {
    this.loadReviews();
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
