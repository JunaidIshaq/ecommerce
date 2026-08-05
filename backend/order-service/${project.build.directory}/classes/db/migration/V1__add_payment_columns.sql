-- Add payment columns to orders table
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_method VARCHAR(255) NOT NULL DEFAULT 'COD';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_status VARCHAR(255) NOT NULL DEFAULT 'PENDING';

-- Update existing records to have valid enum values
UPDATE orders SET payment_method = 'COD', payment_status = 'PENDING' WHERE payment_method IS NULL OR payment_status IS NULL;
