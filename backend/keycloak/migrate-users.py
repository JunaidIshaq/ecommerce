#!/usr/bin/env python3
"""
Migrate existing ShopFast users from user_db into the Keycloak `shopfast` realm.

Why passwords are not carried over
----------------------------------
The existing hashes are BCrypt. Keycloak's built-in hash providers are PBKDF2
variants; it cannot verify a BCrypt hash without a custom SPI provider deployed
into the server. Writing and maintaining that provider to save users one password
reset is a bad trade, so each migrated account is created *without* credentials and
with an UPDATE_PASSWORD required action. On first login the user is forced to set a
new password, which Keycloak then stores with its own (stronger, tunable) hashing.

The alternative - emailing everyone a temporary password - is worse: it puts a live
credential in plaintext in a mailbox.

Idempotency
-----------
Users already present in the realm (matched by email) are skipped, so the script can
be re-run safely after a partial failure. Every action is written to a JSON journal
so the rollback script knows exactly which users this run created.

Usage
-----
    export KEYCLOAK_BASE_URL=http://localhost:8180
    export KEYCLOAK_REALM=shopfast
    export SHOPFAST_SERVICES_CLIENT_SECRET=...
    export POSTGRES_DSN="host=localhost dbname=user_db user=postgres password=..."

    ./migrate-users.py --dry-run          # report what would happen, change nothing
    ./migrate-users.py                    # perform the migration
    ./migrate-users.py --rollback journal-2026-08-05T09-30-00.json
"""

import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone

BASE_URL = os.environ.get("KEYCLOAK_BASE_URL", "http://localhost:8180").rstrip("/")
REALM = os.environ.get("KEYCLOAK_REALM", "shopfast")
CLIENT_ID = os.environ.get("KEYCLOAK_ADMIN_CLIENT_ID", "shopfast-services")
CLIENT_SECRET = os.environ.get("SHOPFAST_SERVICES_CLIENT_SECRET", "")
POSTGRES_DSN = os.environ.get("POSTGRES_DSN", "")

ROLE_MAP = {
    "USER": "ROLE_USER",
    "ADMIN": "ROLE_ADMIN",
    "SELLER": "ROLE_SELLER",
    "SUPPORT": "ROLE_SUPPORT",
}


# --------------------------------------------------------------------- http

def _request(method, url, token=None, body=None, form=False):
    data = None
    headers = {}
    if body is not None:
        if form:
            data = urllib.parse.urlencode(body).encode()
            headers["Content-Type"] = "application/x-www-form-urlencoded"
        else:
            data = json.dumps(body).encode()
            headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"

    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as response:
            raw = response.read()
            payload = json.loads(raw) if raw else None
            return response.status, payload, dict(response.headers)
    except urllib.error.HTTPError as e:
        # Deliberately not echoing the body for token requests - it can contain the secret.
        return e.code, None, dict(e.headers or {})


def admin_token():
    if not CLIENT_SECRET:
        sys.exit("SHOPFAST_SERVICES_CLIENT_SECRET is not set")
    status, payload, _ = _request(
        "POST",
        f"{BASE_URL}/realms/{REALM}/protocol/openid-connect/token",
        body={
            "grant_type": "client_credentials",
            "client_id": CLIENT_ID,
            "client_secret": CLIENT_SECRET,
        },
        form=True,
    )
    if status != 200 or not payload:
        sys.exit(f"Could not obtain an admin token from Keycloak (HTTP {status})")
    return payload["access_token"]


def admin_url(suffix):
    return f"{BASE_URL}/admin/realms/{REALM}{suffix}"


# --------------------------------------------------------------------- source

def load_users():
    """Reads users straight from user_db. psycopg is imported lazily so --help works without it."""
    if not POSTGRES_DSN:
        sys.exit("POSTGRES_DSN is not set")
    try:
        import psycopg2
        import psycopg2.extras
    except ImportError:
        sys.exit("psycopg2 is required: pip install psycopg2-binary")

    with psycopg2.connect(POSTGRES_DSN) as conn:
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute(
                """
                SELECT id, email, first_name, last_name, role, status
                FROM users
                ORDER BY created_at
                """
            )
            return cur.fetchall()


# --------------------------------------------------------------------- migrate

def find_by_email(token, email):
    status, payload, _ = _request(
        "GET",
        admin_url(f"/users?email={urllib.parse.quote(email)}&exact=true"),
        token=token,
    )
    if status != 200 or not payload:
        return None
    return payload[0] if payload else None


def create_user(token, user):
    body = {
        "username": user["email"],
        "email": user["email"],
        "firstName": user.get("first_name") or "",
        "lastName": user.get("last_name") or "",
        # Suspended and deleted accounts must not become live logins just because
        # they were migrated.
        "enabled": (user.get("status") or "ACTIVE") == "ACTIVE",
        # Addresses came from the old system unverified; treat them as verified so
        # existing customers are not locked out, but force a password reset.
        "emailVerified": True,
        "requiredActions": ["UPDATE_PASSWORD"],
        "attributes": {"userId": [str(user["id"])]},
    }
    status, _, headers = _request("POST", admin_url("/users"), token=token, body=body)
    if status != 201:
        return None, status
    location = headers.get("Location", "")
    return location.rsplit("/", 1)[-1], status


def assign_role(token, keycloak_id, role_name):
    status, role, _ = _request("GET", admin_url(f"/roles/{role_name}"), token=token)
    if status != 200 or not role:
        return False
    status, _, _ = _request(
        "POST",
        admin_url(f"/users/{keycloak_id}/role-mappings/realm"),
        token=token,
        body=[{"id": role["id"], "name": role["name"]}],
    )
    return status in (204, 200)


def migrate(dry_run):
    users = load_users()
    print(f"Found {len(users)} users in user_db")

    token = admin_token()
    token_obtained_at = time.time()
    journal = {"startedAt": datetime.now(timezone.utc).isoformat(), "created": [], "skipped": [], "failed": []}

    for index, user in enumerate(users, start=1):
        email = (user["email"] or "").strip().lower()
        if not email:
            journal["failed"].append({"id": str(user["id"]), "reason": "no email"})
            continue

        # A long migration will outlive a 5-minute access token; refresh well before then.
        if time.time() - token_obtained_at > 240:
            token = admin_token()
            token_obtained_at = time.time()

        if find_by_email(token, email):
            journal["skipped"].append(email)
            continue

        if dry_run:
            print(f"[dry-run] would create {email}")
            journal["created"].append({"email": email, "keycloakId": None})
            continue

        keycloak_id, status = create_user(token, user)
        if not keycloak_id:
            print(f"  FAILED {email}: HTTP {status}")
            journal["failed"].append({"email": email, "status": status})
            continue

        role = ROLE_MAP.get((user.get("role") or "USER").upper(), "ROLE_USER")
        if not assign_role(token, keycloak_id, role):
            # The user exists but has no role: record it rather than pretending success,
            # because they will authenticate and then be denied everywhere.
            journal["failed"].append({"email": email, "keycloakId": keycloak_id, "reason": "role assignment failed"})
        journal["created"].append({"email": email, "keycloakId": keycloak_id})

        if index % 100 == 0:
            print(f"  ...{index}/{len(users)}")

    journal["finishedAt"] = datetime.now(timezone.utc).isoformat()
    path = f"journal-{journal['startedAt'].replace(':', '-')}.json"
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(journal, handle, indent=2)

    print(f"\ncreated={len(journal['created'])} skipped={len(journal['skipped'])} failed={len(journal['failed'])}")
    print(f"journal written to {path}")
    if journal["failed"]:
        print("Re-run the script to retry failures; existing users are skipped.")


# --------------------------------------------------------------------- rollback

def rollback(journal_path):
    """
    Deletes exactly the users this run created - never a blanket realm wipe, which
    would take out accounts registered natively after the migration.
    """
    with open(journal_path, encoding="utf-8") as handle:
        journal = json.load(handle)

    token = admin_token()
    removed = 0
    for entry in journal.get("created", []):
        keycloak_id = entry.get("keycloakId")
        if not keycloak_id:
            continue
        status, _, _ = _request("DELETE", admin_url(f"/users/{keycloak_id}"), token=token)
        if status in (204, 404):
            removed += 1
        else:
            print(f"  could not delete {entry.get('email')}: HTTP {status}")
    print(f"Rolled back {removed} users from {journal_path}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dry-run", action="store_true", help="report only, change nothing")
    parser.add_argument("--rollback", metavar="JOURNAL", help="delete the users created by a previous run")
    args = parser.parse_args()

    if args.rollback:
        rollback(args.rollback)
    else:
        migrate(args.dry_run)


if __name__ == "__main__":
    main()
