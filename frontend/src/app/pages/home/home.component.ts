import {ChangeDetectorRef, Component, ElementRef, Inject, NgZone, OnInit, PLATFORM_ID, ViewChild} from '@angular/core';
import {DecimalPipe, isPlatformBrowser, NgClass, NgForOf, NgIf} from '@angular/common';
import {ProductService} from '../../services/product.service';
import {CartService} from '../../services/cart.service';
import {Router, RouterLink} from '@angular/router';
import {debounceTime, distinctUntilChanged} from 'rxjs';
import {SearchService} from '../../services/search.service';

interface FloatingItem {
  emoji: string;
  label: string;
  top?: string;
  left?: string;
  right?: string;
  bottom?: string;
  delay: number;
  colorClass: string;
}

interface StatItem {
  number: string;
  label: string;
  target: number;
  suffix: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [DecimalPipe, NgClass, NgIf, NgForOf, RouterLink],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  @ViewChild('productsSection') productsSection!: ElementRef
  products: any[] = [];
  currentPage = 1;
  pageSize = 12;
  totalPages = 0;
  totalItems = 0;
  visiblePages: number[] = [];
  maxVisible = 3;
  loading = true;
  errorMessage = '';
  private searchKeyword: string | undefined;

  // Dynamic floating items for the banner
  floatingItems: FloatingItem[] = [
    { emoji: '🍎', label: 'Fresh Fruits', top: '15%', left: '5%', delay: 0, colorClass: 'color-fresh' },
    { emoji: '🥕', label: 'Vegetables', top: '25%', right: '8%', delay: 1, colorClass: 'color-organic' },
    { emoji: '🥛', label: 'Dairy', top: '60%', left: '3%', delay: 2, colorClass: 'color-dairy' },
    { emoji: '🍞', label: 'Bakery', bottom: '20%', right: '5%', delay: 0.5, colorClass: 'color-bakery' },
    { emoji: '🥚', label: 'Groceries', top: '45%', left: '8%', delay: 1.5, colorClass: 'color-grocer' },
    { emoji: '📱', label: 'Electronics', top: '20%', right: '15%', delay: 2.5, colorClass: 'color-fresh' },
    { emoji: '👕', label: 'Fashion', bottom: '25%', left: '10%', delay: 3, colorClass: 'color-snacks' },
    { emoji: '🧴', label: 'Beauty', top: '55%', right: '3%', delay: 1.2, colorClass: 'color-beverages' },
    { emoji: '🍫', label: 'Snacks', bottom: '15%', right: '12%', delay: 2.2, colorClass: 'color-snacks' },
    { emoji: '🥤', label: 'Beverages', top: '70%', left: '6%', delay: 3.5, colorClass: 'color-beverages' },
    { emoji: '🧊', label: 'Frozen', bottom: '30%', left: '15%', delay: 0.8, colorClass: 'color-frozen' },
    { emoji: '🍯', label: 'Organic', top: '35%', right: '6%', delay: 2.8, colorClass: 'color-organic' },
  ];

  // Typing effect for hero title
  titleTexts: string[] = [
    'Fresh Groceries & Everything',
    'Quality Products Daily',
    'Fast Delivery to Your Door',
    'Best Deals Online',
    'Shop Smart, Save More'
  ];
  currentTitleIndex = 0;
  currentTitleText = '';
  isDeleting = false;
  typingSpeed = 100;
  private typingInterval: any;

  // Stats with counter animation
  stats: StatItem[] = [
    { number: '10K+', label: 'Products', target: 10000, suffix: '+' },
    { number: '500+', label: 'Brands', target: 500, suffix: '+' },
    { number: '50K+', label: 'Happy Customers', target: 50000, suffix: '+' }
  ];
  animatedStats: { value: number; suffix: string }[] = [
    { value: 0, suffix: '+' },
    { value: 0, suffix: '+' },
    { value: 0, suffix: '+' }
  ];
  private statsAnimated = false;

  // Mouse parallax
  mouseX = 0;
  mouseY = 0;
  private parallaxEnabled = false;

  constructor(
    private productService: ProductService,
    private searchService: SearchService,
    private cart: CartService,
    private ngZone: NgZone,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    this.loadProducts(this.currentPage);
    this.startTypingEffect();

    this.searchService.search$
      .pipe(
        debounceTime(100),
        distinctUntilChanged()
      )
      .subscribe(keyword => {
        this.searchKeyword = keyword;
        this.currentPage = 1;
        this.loadProducts(1);
      });
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.setupParallax();
      this.setupIntersectionObserver();
    }
  }

  ngOnDestroy(): void {
    if (this.typingInterval) {
      clearInterval(this.typingInterval);
    }
  }

  /**
   * 🔹 Load paginated products from backend
   */
  loadProducts(page: number): void {
    this.loading = true;

    this.productService.getAllProducts(page, this.pageSize, this.searchKeyword).subscribe({
      next: (response: any) => {
        // Expecting backend JSON structure: { items, totalItems, totalPages, page, size }
        this.ngZone.run(() => {
          this.products = response.items || [];
          this.totalItems = response.totalItems || 0;
          this.totalPages = response.totalPages || 0;
          this.currentPage = response.page || 1;
          this.updateVisiblePages();
          this.loading = false;
          this.cdr.detectChanges();
        });
      },
      error: (err: any) => {
        this.ngZone.run(() => {
          this.loading = false;
          this.errorMessage = 'Failed to load products.';
        });
        console.error('❌ Product load error:', err);
      }
    });
  }

  /**
   * 🔹 Handle pagination click
   */
  setPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.loadProducts(page);

    this.scrollToProductsSection();
  }

  scrollToProductsSection(): void {
    if (isPlatformBrowser(this.platformId) && this.productsSection) {
      this.productsSection.nativeElement.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
      });
    }
  }

  /**
   * 🔹 Update visible pagination numbers
   */
  updateVisiblePages(): void {
    const half = Math.floor(this.maxVisible / 2);
    let startPage = Math.max(this.currentPage - half, 1);
    let endPage = startPage + this.maxVisible - 1;

    if (endPage > this.totalPages) {
      endPage = this.totalPages;
      startPage = Math.max(endPage - this.maxVisible + 1, 1);
    }

    this.visiblePages = [];
    for (let i = startPage; i <= endPage; i++) {
      this.visiblePages.push(i);
    }
  }

  /**
   * 🛒 Add product to cart
   */
  addToCart(product: any): void {
    this.cart.addToCart(product.id, 1).subscribe();
  }

  /**
   * 🔹 Navigate to product details page
   */
  goToProductDetail(id: string): void {
    this.router.navigate(['/product', id]);
  }

  /**
   * Typing effect for hero title
   */
  private startTypingEffect(): void {
    this.currentTitleText = '';

    this.typingInterval = setInterval(() => {
      const fullText = this.titleTexts[this.currentTitleIndex];

      if (this.isDeleting) {
        this.currentTitleText = fullText.substring(0, this.currentTitleText.length - 1);
        this.typingSpeed = 50;
      } else {
        this.currentTitleText = fullText.substring(0, this.currentTitleText.length + 1);
        this.typingSpeed = 100;
      }

      if (!this.isDeleting && this.currentTitleText === fullText) {
        this.typingSpeed = 2000; // Pause at end
        this.isDeleting = true;
      } else if (this.isDeleting && this.currentTitleText === '') {
        this.isDeleting = false;
        this.currentTitleIndex = (this.currentTitleIndex + 1) % this.titleTexts.length;
        this.typingSpeed = 500; // Pause before next word
      }

      this.cdr.detectChanges();
    }, this.typingSpeed);
  }

  /**
   * Mouse parallax effect
   */
  private setupParallax(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const hero = document.querySelector('.hero');
    if (!hero) return;

    hero.addEventListener('mousemove', (e: Event) => {
      const mouseEvent = e as MouseEvent;
      if (!this.parallaxEnabled) return;

      const rect = hero.getBoundingClientRect();
      const x = (mouseEvent.clientX - rect.left) / rect.width - 0.5;
      const y = (mouseEvent.clientY - rect.top) / rect.height - 0.5;

      this.mouseX = x;
      this.mouseY = y;

      this.ngZone.run(() => {
        this.cdr.detectChanges();
      });
    });

    hero.addEventListener('mouseenter', () => {
      this.parallaxEnabled = true;
    });

    hero.addEventListener('mouseleave', () => {
      this.parallaxEnabled = false;
      this.mouseX = 0;
      this.mouseY = 0;
      this.ngZone.run(() => {
        this.cdr.detectChanges();
      });
    });
  }

  /**
   * Intersection Observer for stats counter animation
   */
  private setupIntersectionObserver(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting && !this.statsAnimated) {
          this.statsAnimated = true;
          this.animateStats();
        }
      });
    }, { threshold: 0.5 });

    const statsSection = document.querySelector('.hero-stats');
    if (statsSection) {
      observer.observe(statsSection);
    }
  }

  /**
   * Animate stats counter
   */
  private animateStats(): void {
    const duration = 2000;
    const startTime = Date.now();

    const animate = () => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);

      // Easing function
      const easeOutQuart = 1 - Math.pow(1 - progress, 4);

      this.stats.forEach((stat, index) => {
        const currentValue = Math.floor(stat.target * easeOutQuart);
        this.animatedStats[index] = {
          value: currentValue,
          suffix: stat.suffix
        };
      });

      this.cdr.detectChanges();

      if (progress < 1) {
        requestAnimationFrame(animate);
      }
    };

    requestAnimationFrame(animate);
  }

  /**
   * Get parallax transform for shapes
   */
  getParallaxTransform(intensity: number): string {
    if (!this.parallaxEnabled) return '';
    const x = this.mouseX * intensity;
    const y = this.mouseY * intensity;
    return `translate(${x}px, ${y}px)`;
  }

  /**
   * Get parallax transform for floating items
   */
  getFloatingParallax(intensity: number): string {
    if (!this.parallaxEnabled) return '';
    const x = this.mouseX * intensity;
    const y = this.mouseY * intensity;
    return `translate(${x}px, ${y}px)`;
  }
}
