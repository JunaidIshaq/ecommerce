// Helper: Convert string to ArrayBuffer
export function strToBuffer(str: string): ArrayBuffer {
  return new TextEncoder().encode(str);
}

// Helper: Convert ArrayBuffer to Base64
export function bufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

// Helper: Convert Base64 to ArrayBuffer
export function base64ToBuffer(base64: string): ArrayBuffer {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

// Helper: Generate random IV (12 bytes)
export function generateIV(): ArrayBuffer {
  return crypto.getRandomValues(new Uint8Array(12)).buffer;
}

// Encrypt password using AES-256-GCM
export async function encryptPassword(
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
