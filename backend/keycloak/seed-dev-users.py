#!/usr/bin/env python3
"""
Seed a fixed set of test users into the `shopfast` Keycloak realm.

Idempotent: a user that already exists is left completely untouched, including
its password. Re-running after every deploy therefore only fills in whatever is
missing, and never clobbers a password someone has since changed.

SECURITY
--------
These accounts have a shared, well-known password. Anyone who can reach the
login page can sign in as them. That is fine for a development or demo realm
and unacceptable for one holding real customer data, so the script refuses to
run unless you opt in explicitly:

    SEED_DEV_USERS=true python3 seed-dev-users.py --admin-password "$PW"

or pass --i-understand-this-creates-known-credentials.

Usage:
    SEED_DEV_USERS=true python3 seed-dev-users.py \
        --base-url http://127.0.0.1:8180/auth --realm shopfast \
        --admin-user admin --admin-password "$PW"

    # remove them again
    SEED_DEV_USERS=true python3 seed-dev-users.py --admin-password "$PW" --delete
"""

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

USER_COUNT = 10
EMAIL_TEMPLATE = "alice{n}@yopmail.com"

# Must satisfy the realm password policy, which at time of writing is:
#   length(12) and upperCase(1) and lowerCase(1) and digits(1) and
#   specialChars(1) and notUsername and passwordHistory(3)
# Keycloak rejects the user creation outright if it does not, so keep these in
# step if the policy is ever tightened.
PASSWORD = os.environ.get("SEED_USER_PASSWORD", "Secret123!xyz")

# Realm roles in this project carry the ROLE_ prefix in Keycloak itself
# (KeycloakRealmRoleConverter deliberately does not add a second one).
REALM_ROLE = "ROLE_USER"


class KeycloakAdmin:
    def __init__(self, base_url, realm, token):
        self.base = base_url.rstrip("/")
        self.realm = realm
        self.token = token

    def _request(self, method, path, body=None):
        url = "%s/admin/realms/%s%s" % (self.base, self.realm, path)
        data = json.dumps(body).encode() if body is not None else None
        req = urllib.request.Request(url, data=data, method=method)
        req.add_header("Authorization", "Bearer " + self.token)
        if data:
            req.add_header("Content-Type", "application/json")
        try:
            with urllib.request.urlopen(req) as resp:
                raw = resp.read()
                return json.loads(raw) if raw else None
        except urllib.error.HTTPError as exc:
            detail = exc.read().decode(errors="replace")
            raise RuntimeError("%s %s -> %s %s" % (method, url, exc.code, detail))

    def get(self, path):
        return self._request("GET", path)

    def post(self, path, body):
        return self._request("POST", path, body)

    def put(self, path, body=None):
        return self._request("PUT", path, body)

    def delete(self, path):
        return self._request("DELETE", path)


def get_admin_token(base_url, admin_realm, user, password):
    url = "%s/realms/%s/protocol/openid-connect/token" % (
        base_url.rstrip("/"), admin_realm)
    payload = urllib.parse.urlencode({
        "client_id": "admin-cli",
        "username": user,
        "password": password,
        "grant_type": "password",
    }).encode()
    req = urllib.request.Request(url, data=payload, method="POST")
    req.add_header("Content-Type", "application/x-www-form-urlencoded")
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())["access_token"]


def find_user(kc, email):
    """Locate a seeded user by its email/username.

    Queried by username as well as email: the ?email= filter does not reliably
    return a match on all Keycloak versions, and silently finding nothing would
    make the script try to recreate a user that already exists. Results are
    still compared exactly, because the plain `search` parameter is a substring
    match and alice1@ would otherwise also match alice10@.
    """
    for query in ("username=%s&exact=true", "email=%s&exact=true", "search=%s"):
        try:
            found = kc.get("/users?" + query % urllib.parse.quote(email))
        except RuntimeError:
            continue
        for u in found or []:
            if (u.get("email") or "").lower() == email.lower() \
                    or (u.get("username") or "").lower() == email.lower():
                return u
    return None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:8180/auth")
    parser.add_argument("--realm", default="shopfast")
    parser.add_argument("--admin-realm", default="master")
    parser.add_argument("--admin-user", default="admin")
    parser.add_argument("--admin-password", required=True)
    parser.add_argument("--count", type=int, default=USER_COUNT)
    parser.add_argument("--delete", action="store_true",
                        help="Remove the seeded users instead of creating them.")
    parser.add_argument("--i-understand-this-creates-known-credentials",
                        dest="confirmed", action="store_true")
    args = parser.parse_args()

    if not (args.confirmed or os.environ.get("SEED_DEV_USERS") == "true"):
        print("Refusing to run: these accounts share a well-known password.\n"
              "Set SEED_DEV_USERS=true (or pass "
              "--i-understand-this-creates-known-credentials) to proceed.",
              file=sys.stderr)
        return 2

    token = get_admin_token(args.base_url, args.admin_realm,
                            args.admin_user, args.admin_password)
    kc = KeycloakAdmin(args.base_url, args.realm, token)

    # Resolve the realm role once; skip role assignment if it does not exist
    # rather than failing the whole seed.
    role = None
    try:
        role = kc.get("/roles/%s" % urllib.parse.quote(REALM_ROLE))
    except RuntimeError:
        print("note: realm role '%s' not found, users will be created without it"
              % REALM_ROLE)

    created = skipped = deleted = 0

    for n in range(1, args.count + 1):
        email = EMAIL_TEMPLATE.format(n=n)
        existing = find_user(kc, email)

        if args.delete:
            if existing:
                kc.delete("/users/%s" % existing["id"])
                print("  [delete] %s" % email)
                deleted += 1
            else:
                print("  [absent] %s" % email)
            continue

        if existing:
            print("  [skip]   %s already exists" % email)
            skipped += 1
            continue

        try:
            kc.post("/users", {
                "username": email,
                "email": email,
                "emailVerified": True,
                "enabled": True,
                "firstName": "Alice",
                "lastName": "Test%d" % n,
                "credentials": [{
                    "type": "password",
                    "value": PASSWORD,
                    "temporary": False,
                }],
            })
        except RuntimeError as exc:
            # Belt and braces behind find_user: a 409 means it is already there,
            # which is the outcome we wanted anyway. Anything else is real.
            if "409" not in str(exc):
                raise
            print("  [skip]   %s already exists" % email)
            skipped += 1
            continue

        user = find_user(kc, email)
        if user is None:
            print("  [warn]   %s created but could not be read back" % email)
            continue

        # shopfast-profile maps a `userId` attribute into the token; services
        # migrated from the old auth-service key their rows on it.
        #
        # This has to be a *merge* into the representation we just read back.
        # Keycloak's user update replaces the whole representation, so putting
        # a body of just {"attributes": ...} silently blanks email, firstName
        # and lastName on the account we have only just created.
        updated = dict(user)
        attributes = dict(updated.get("attributes") or {})
        attributes["userId"] = [user["id"]]
        updated["attributes"] = attributes
        kc.put("/users/%s" % user["id"], updated)

        if role:
            kc.post("/users/%s/role-mappings/realm" % user["id"], [{
                "id": role["id"],
                "name": role["name"],
            }])

        print("  [create] %s" % email)
        created += 1

    if args.delete:
        print("Deleted %d user(s)." % deleted)
    else:
        print("Created %d, skipped %d existing." % (created, skipped))
    return 0


if __name__ == "__main__":
    sys.exit(main())
