/**
 * Mirror of the backend `ProfileDto`.
 *
 * Note there is no `password` field, and there must never be one: the server does
 * not send the hash, and adding it here would only invite someone to "fix" the
 * server to match. `role` and `status` are present but read-only - they are shown,
 * never submitted (see `UpdateProfileRequest`, which cannot carry them).
 */
export interface Profile {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  /** Convenience join of first/last, already trimmed by the server. */
  fullName: string;
  phone: string | null;
  country: string | null;
  role: string | null;
  status: string | null;
  /** ISO-8601 instant of account creation. */
  memberSince: string | null;
}

/**
 * The only fields the user may change. Kept deliberately narrow to match the
 * server-side record; sending anything else is silently ignored, so keeping the
 * shapes aligned avoids edits that appear to work but do nothing.
 */
export interface UpdateProfileRequest {
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
  country: string | null;
}
