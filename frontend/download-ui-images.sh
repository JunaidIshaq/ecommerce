#!/usr/bin/env bash

set -e

ASSETS_DIR="src/assets/images"

# --------------------------
#  Create folder structure
# --------------------------
echo "📁 Creating folder structure..."
mkdir -p "$ASSETS_DIR"/{logo,banners,categories,ui,footer}

# --------------------------
#  Logo (placeholder)
# --------------------------
echo "🧾 Downloading logo..."
curl -s -L "https://dummyimage.com/200x60/000/fff.png&text=MyShop+Logo" -o "$ASSETS_DIR/logo/logo.png"

# --------------------------
#  Banners (Unsplash realistic lifestyle images)
# --------------------------
echo "🌄 Downloading banners..."
curl -s -L "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=1600" -o "$ASSETS_DIR/banners/hero-banner-1.jpg"
curl -s -L "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=1600" -o "$ASSETS_DIR/banners/hero-banner-2.jpg"
curl -s -L "https://images.unsplash.com/photo-1585386959984-a41552231693?w=1600" -o "$ASSETS_DIR/banners/sale-banner.jpg"

# Category banners
curl -s -L "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=1600" -o "$ASSETS_DIR/banners/electronics-banner.jpg"
curl -s -L "https://images.unsplash.com/photo-1512436991641-6745cdb1723f?w=1600" -o "$ASSETS_DIR/banners/fashion-banner.jpg"
curl -s -L "https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=1600" -o "$ASSETS_DIR/banners/home-banner.jpg"
curl -s -L "https://images.unsplash.com/photo-1558611848-73f7eb4001b7?w=1600" -o "$ASSETS_DIR/banners/fitness-banner.jpg"
curl -s -L "https://images.unsplash.com/photo-1606813902912-91f50a72e2b6?w=1600" -o "$ASSETS_DIR/banners/accessories-banner.jpg"

# --------------------------
#  Category thumbnails (smaller, realistic)
# --------------------------
echo "🧭 Downloading category images..."
curl -s -L "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=400" -o "$ASSETS_DIR/categories/electronics.jpg"
curl -s -L "https://images.unsplash.com/photo-1512436991641-6745cdb1723f?w=400" -o "$ASSETS_DIR/categories/fashion.jpg"
curl -s -L "https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=400" -o "$ASSETS_DIR/categories/home.jpg"
curl -s -L "https://images.unsplash.com/photo-1558611848-73f7eb4001b7?w=400" -o "$ASSETS_DIR/categories/fitness.jpg"
curl -s -L "https://images.unsplash.com/photo-1606813902912-91f50a72e2b6?w=400" -o "$ASSETS_DIR/categories/accessories.jpg"

# --------------------------
#  UI Icons (SVGRepo)
# --------------------------
echo "🧰 Downloading UI icons..."
curl -s -L "https://www.svgrepo.com/download/13695/search.svg" -o "$ASSETS_DIR/ui/search-icon.svg"
curl -s -L "https://www.svgrepo.com/download/521384/shopping-cart.svg" -o "$ASSETS_DIR/ui/cart-icon.svg"
curl -s -L "https://www.svgrepo.com/download/521507/user.svg" -o "$ASSETS_DIR/ui/profile-icon.svg"
curl -s -L "https://www.svgrepo.com/download/475676/empty-cart.svg" -o "$ASSETS_DIR/ui/empty-cart.svg"
curl -s -L "https://dummyimage.com/400x400/e0e0e0/aaa.png&text=Image+Placeholder" -o "$ASSETS_DIR/ui/placeholder-image.png"

# --------------------------
#  Footer Logos (payment + social)
# --------------------------
echo "💳 Downloading footer logos..."
curl -s -L "https://upload.wikimedia.org/wikipedia/commons/4/41/Visa_Logo.png" -o "$ASSETS_DIR/footer/payment-visa.png"
curl -s -L "https://upload.wikimedia.org/wikipedia/commons/0/04/Mastercard-logo.png" -o "$ASSETS_DIR/footer/payment-mastercard.png"
curl -s -L "https://upload.wikimedia.org/wikipedia/commons/b/b5/PayPal.svg" -o "$ASSETS_DIR/footer/payment-paypal.png"
curl -s -L "https://www.svgrepo.com/show/503338/social-media.svg" -o "$ASSETS_DIR/footer/social-icons.png"

echo "✅ All UI images downloaded successfully to: $ASSETS_DIR"

