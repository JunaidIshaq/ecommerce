export const isBrowser = typeof window !== 'undefined';

export function safeLocalStorageGet(key: string): string | null {
  if (isBrowser && window.localStorage) {
    return localStorage.getItem(key);
  }
  return null;
}

export function safeLocalStorageSet(key: string, value: string) {
  if (isBrowser && window.localStorage) {
    localStorage.setItem(key, value);
  }
}

export function safeLocalStorageRemove(key: string) {
  if (isBrowser && window.localStorage) {
    localStorage.removeItem(key);
  }
}
