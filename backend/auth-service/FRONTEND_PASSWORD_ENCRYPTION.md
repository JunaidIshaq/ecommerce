# Frontend Password Encryption Specification

## Overview

The `/api/v1/auth/login` endpoint requires the `password` field to be encrypted using **AES-256-GCM** before sending it over the wire. This document specifies the exact encryption scheme the frontend must follow.

---

## Encryption Algorithm

| Property | Value |
|----------|-------|
| **Algorithm** | AES-256-GCM (Advanced Encryption Standard with Galois/Counter Mode) |
| **Key Size** | 256 bits (32 bytes) |
| **IV Size** | 12 bytes (96 bits) — randomly generated per encryption |
| **Auth Tag** | 128 bits (built into GCM) |
| **Encoding** | Base64 |

---

## How It Works

1. **Frontend** encrypts the plaintext password using AES-256-GCM with a shared secret key.
2. **Frontend** concatenates `IV + ciphertext + authTag` into a single byte array.
3. **Frontend** Base64-encodes the concatenated bytes.
4. **Frontend** sends the Base64 string as the `password` field in the login request body.
5. **Backend** Base64-decodes, extracts the IV, decrypts using AES-256-GCM, and then compares the plaintext password against the stored BCrypt hash.

---

## Request Format

```json
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "BASE64_ENCRYPTED_PASSWORD_HERE"
}
```

---

## Frontend Implementation (JavaScript/TypeScript Example)

```typescript
// Helper: Convert string to ArrayBuffer
function strToBuffer(str: string): ArrayBuffer {
  return new TextEncoder().encode(str);
}

// Helper: Convert ArrayBuffer to Base64
function bufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

// Helper: Convert Base64 to ArrayBuffer
function base64ToBuffer(base64: string): ArrayBuffer {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

// Helper: Generate random IV (12 bytes)
function generateIV(): ArrayBuffer {
  return crypto.getRandomValues(new Uint8Array(12)).buffer;
}

// Encrypt password using AES-256-GCM
async function encryptPassword(
  plaintextPassword: string,
  base64Key: string
): Promise<string> {
  // 1. Import the Base64-encoded key
  const keyBytes = base64ToBuffer(base64Key);
  const cryptoKey = await crypto.subtle.importKey(
    'raw',
    keyBytes,
    { name: 'AES-GCM' },
    false,
    ['encrypt']
  );

  // 2. Generate random 12-byte IV
  const iv = generateIV();

  // 3. Encrypt
  const ciphertext = await crypto.subtle.encrypt(
    {
      name: 'AES-GCM',
      iv: new Uint8Array(iv),
    },
    cryptoKey,
    strToBuffer(plaintextPassword)
  );

  // 4. Concatenate IV + ciphertext
  const combined = new Uint8Array(iv.byteLength + ciphertext.byteLength);
  combined.set(new Uint8Array(iv), 0);
  combined.set(new Uint8Array(ciphertext), iv.byteLength);

  // 5. Base64 encode
  return bufferToBase64(combined.buffer);
}

// Usage
const base64Key = 'MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI='; // Get from backend config
const encrypted = await encryptPassword('myPlainPassword', base64Key);

// Send in login request
fetch('/api/v1/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'user@example.com',
    password: encrypted,
  }),
});
```

---

## Encryption Key

The frontend must obtain the **Base64-encoded 256-bit AES key** from the backend configuration. This key is shared between the frontend and backend.

**Default key (for development only):**
```
MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=
```
> This decodes to `12345678901234567890123456789012` (32 bytes).

**Production:** The key must be injected via the environment variable `APP_PASSWORD_ENCRYPTION_KEY` and must be a cryptographically random 32-byte value.

---

## Security Notes

1. **Always use HTTPS/TLS** in addition to this encryption. The AES encryption protects the password at the application layer; TLS protects the entire HTTP request.
2. **Never hardcode the encryption key** in frontend source code or public repositories.
3. **Rotate the encryption key** periodically. When rotating, ensure all active frontend clients are updated before the old key is revoked.
4. **GCM mode** provides both confidentiality and integrity verification (authentication tag). If decryption fails, the backend will reject the request.
5. **IV uniqueness:** A new random IV is generated for each encryption. Never reuse an IV with the same key.

---

## Backend Configuration

The backend reads the encryption key from:

```yaml
app:
  password:
    encryption:
      key: ${APP_PASSWORD_ENCRYPTION_KEY:MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=}
```

Or via environment variable:
```bash
export APP_PASSWORD_ENCRYPTION_KEY="your-base64-encoded-32-byte-key"
```

---

## Troubleshooting

| Error | Cause | Solution |
|-------|-------|----------|
| `Invalid credentials` | Password decryption failed or BCrypt mismatch | Ensure the frontend uses the correct encryption key and algorithm |
| `Failed to decrypt password` | Corrupted Base64 or wrong key | Verify the key matches backend config and Base64 is valid |
| `Invalid encrypted password format` | Data too short or malformed | Ensure IV (12 bytes) + ciphertext are properly concatenated |
