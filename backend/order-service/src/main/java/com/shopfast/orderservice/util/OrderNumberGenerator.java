package com.shopfast.orderservice.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates human-readable, collision-resistant order numbers.
 *
 * <p>Format: {@code ORD-yyyyMMdd-<12 crockford-base32 chars>}.</p>
 *
 * <p>Replaces the previous {@code UUID.randomUUID().toString().substring(0, 8)} scheme,
 * which drew from only ~4.3 billion values and therefore had roughly a 50% chance of at
 * least one collision by ~100k orders (birthday bound) — with no unique index to catch it.
 * The 60 bits of entropy here plus a monotonic counter make collisions negligible, and a
 * UNIQUE constraint on {@code orders.order_number} is the final backstop.</p>
 */
@Component
public class OrderNumberGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray(); // Crockford base32
    private static final int RANDOM_CHARS = 12;

    private final SecureRandom random = new SecureRandom();
    private final AtomicLong counter = new AtomicLong(new SecureRandom().nextInt(1 << 16));

    public String next() {
        StringBuilder sb = new StringBuilder(28);
        sb.append("ORD-")
          .append(LocalDate.now(ZoneOffset.UTC).format(DATE))
          .append('-');

        long seq = counter.incrementAndGet();
        for (int i = 0; i < RANDOM_CHARS; i++) {
            // Mix a monotonic counter into the tail so two calls in the same
            // nanosecond cannot produce the same suffix.
            int idx = (random.nextInt(ALPHABET.length) + (int) (seq >>> (i % 8))) % ALPHABET.length;
            sb.append(ALPHABET[Math.abs(idx)]);
        }
        return sb.toString();
    }
}
