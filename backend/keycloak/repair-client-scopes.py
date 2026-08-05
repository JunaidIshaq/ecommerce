#!/usr/bin/env python3
"""
Repair missing built-in client scopes in the `shopfast` Keycloak realm.

WHY THIS EXISTS
---------------
`realm-export.json` declares its own `clientScopes` array containing only the
custom `shopfast-profile` scope. When a realm import supplies that array,
Keycloak treats it as the COMPLETE set of client scopes for the realm and
therefore never creates its built-in scopes (profile, email, roles,
web-origins, acr, basic).

Consequences observed in production:
  * Authorization requests with `scope=openid profile email` were rejected with
    `invalid_scope` -> users could not log in at all.
  * Even if login had worked, tokens would carry no `realm_access.roles` claim,
    so `KeycloakRealmRoleConverter` / `hasRole("ADMIN")` could never match.

This script is idempotent: it creates only the scopes that are missing, then
binds them to the given clients as default (or optional) scopes. Running it
twice is a no-op.

Usage:
    python3 repair-client-scopes.py --base-url http://127.0.0.1:8180/auth \
        --realm shopfast --admin-user admin --admin-password "$PW"
"""

import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request

# Clients that should receive the standard OIDC scopes as *default* scopes.
DEFAULT_SCOPE_CLIENTS = ["shopfast-web", "shopfast-mobile", "shopfast-admin"]

# Confidential service client: it uses client_credentials, so it needs role
# claims but has no end user and therefore no profile/email.
SERVICE_CLIENTS = ["shopfast-services"]


def _scope(name, description, mappers, in_token_scope="true",
           consent_text=None, gui_order=None):
    attributes = {
        "include.in.token.scope": in_token_scope,
        "display.on.consent.screen": "true" if consent_text else "false",
    }
    if consent_text:
        attributes["consent.screen.text"] = consent_text
    if gui_order is not None:
        attributes["gui.order"] = str(gui_order)
    return {
        "name": name,
        "description": description,
        "protocol": "openid-connect",
        "attributes": attributes,
        "protocolMappers": mappers,
    }


def _user_attr(name, user_attribute, claim_name, jsontype="String",
               multivalued=False):
    config = {
        "user.attribute": user_attribute,
        "claim.name": claim_name,
        "jsonType.label": jsontype,
        "id.token.claim": "true",
        "access.token.claim": "true",
        "userinfo.token.claim": "true",
        "introspection.token.claim": "true",
    }
    if multivalued:
        config["multivalued"] = "true"
    return {
        "name": name,
        "protocol": "openid-connect",
        "protocolMapper": "oidc-usermodel-attribute-mapper",
        "consentRequired": False,
        "config": config,
    }


def _user_property(name, user_attribute, claim_name):
    return {
        "name": name,
        "protocol": "openid-connect",
        "protocolMapper": "oidc-usermodel-property-mapper",
        "consentRequired": False,
        "config": {
            "user.attribute": user_attribute,
            "claim.name": claim_name,
            "jsonType.label": "String",
            "id.token.claim": "true",
            "access.token.claim": "true",
            "userinfo.token.claim": "true",
            "introspection.token.claim": "true",
        },
    }


def build_scope_definitions():
    """Mirror Keycloak 26's built-in client scope definitions."""

    profile = _scope(
        "profile",
        "OpenID Connect built-in scope: profile",
        [
            _user_property("username", "username", "preferred_username"),
            _user_property("email", "email", "email"),
            {
                "name": "full name",
                "protocol": "openid-connect",
                "protocolMapper": "oidc-full-name-mapper",
                "consentRequired": False,
                "config": {
                    "id.token.claim": "true",
                    "access.token.claim": "true",
                    "userinfo.token.claim": "true",
                    "introspection.token.claim": "true",
                },
            },
            _user_property("given name", "firstName", "given_name"),
            _user_property("family name", "lastName", "family_name"),
            _user_attr("nickname", "nickname", "nickname"),
            _user_attr("profile", "profile", "profile"),
            _user_attr("picture", "picture", "picture"),
            _user_attr("website", "website", "website"),
            _user_attr("gender", "gender", "gender"),
            _user_attr("birthdate", "birthdate", "birthdate"),
            _user_attr("zoneinfo", "zoneinfo", "zoneinfo"),
            _user_attr("locale", "locale", "locale"),
            _user_attr("updated at", "updatedAt", "updated_at", "long"),
        ],
        consent_text="${profileScopeConsentText}",
        gui_order=1,
    )

    email = _scope(
        "email",
        "OpenID Connect built-in scope: email",
        [
            _user_property("email", "email", "email"),
            {
                "name": "email verified",
                "protocol": "openid-connect",
                "protocolMapper": "oidc-usermodel-property-mapper",
                "consentRequired": False,
                "config": {
                    "user.attribute": "emailVerified",
                    "claim.name": "email_verified",
                    "jsonType.label": "boolean",
                    "id.token.claim": "true",
                    "access.token.claim": "true",
                    "userinfo.token.claim": "true",
                    "introspection.token.claim": "true",
                },
            },
        ],
        consent_text="${emailScopeConsentText}",
        gui_order=2,
    )

    # `roles` is what produces realm_access.roles / resource_access.*.roles.
    # Without it hasRole(...) can never match. It is deliberately excluded
    # from the token `scope` string (include.in.token.scope=false), matching
    # Keycloak's own definition.
    roles = _scope(
        "roles",
        "OpenID Connect scope for add user roles to the access token",
        [
            {
                "name": "realm roles",
                "protocol": "openid-connect",
                "protocolMapper": "oidc-usermodel-realm-role-mapper",
                "consentRequired": False,
                "config": {
                    "user.attribute": "foo",
                    "claim.name": "realm_access.roles",
                    "jsonType.label": "String",
                    "multivalued": "true",
                    "access.token.claim": "true",
                    "introspection.token.claim": "true",
                },
            },
            {
                "name": "client roles",
                "protocol": "openid-connect",
                "protocolMapper": "oidc-usermodel-client-role-mapper",
                "consentRequired": False,
                "config": {
                    "user.attribute": "foo",
                    "claim.name": "resource_access.${client_id}.roles",
                    "jsonType.label": "String",
                    "multivalued": "true",
                    "access.token.claim": "true",
                    "introspection.token.claim": "true",
                },
            },
            {
                "name": "audience resolve",
                "protocol": "openid-connect",
                "protocolMapper": "oidc-audience-resolve-mapper",
                "consentRequired": False,
                "config": {
                    "access.token.claim": "true",
                    "introspection.token.claim": "true",
                },
            },
        ],
        in_token_scope="false",
        consent_text="${rolesScopeConsentText}",
    )

    web_origins = _scope(
        "web-origins",
        "OpenID Connect scope for add allowed web origins to the access token",
        [
            {
                "name": "allowed web origins",
                "protocol": "openid-connect",
                "protocolMapper": "oidc-allowed-origins-mapper",
                "consentRequired": False,
                "config": {"introspection.token.claim": "true"},
            }
        ],
        in_token_scope="false",
    )

    acr = _scope(
        "acr",
        "OpenID Connect scope for add acr (authentication context class reference) to the token",
        [
            {
                "name": "acr loa level",
                "protocol": "openid-connect",
                "protocolMapper": "oidc-acr-mapper",
                "consentRequired": False,
                "config": {
                    "id.token.claim": "true",
                    "access.token.claim": "true",
                    "introspection.token.claim": "true",
                },
            }
        ],
        in_token_scope="false",
    )

    basic = _scope(
        "basic",
        "OpenID Connect scope for add all basic claims to the token",
        [
            {
                "name": "auth_time",
                "protocol": "openid-connect",
                "protocolMapper": "oidc-usersessionmodel-note-mapper",
                "consentRequired": False,
                "config": {
                    "user.session.note": "AUTH_TIME",
                    "claim.name": "auth_time",
                    "jsonType.label": "long",
                    "id.token.claim": "true",
                    "access.token.claim": "true",
                    "introspection.token.claim": "true",
                },
            },
            {
                "name": "sub",
                "protocol": "openid-connect",
                "protocolMapper": "oidc-sub-mapper",
                "consentRequired": False,
                "config": {
                    "access.token.claim": "true",
                    "introspection.token.claim": "true",
                },
            },
        ],
        in_token_scope="false",
    )

    # Custom scope: exposes the legacy internal user id as a `userId` claim so
    # services migrated from the old auth-service keep working.
    shopfast_profile = _scope(
        "shopfast-profile",
        "ShopFast custom claims (internal user id)",
        [
            {
                "name": "shopfast-user-id",
                "protocol": "openid-connect",
                "protocolMapper": "oidc-usermodel-attribute-mapper",
                "consentRequired": False,
                "config": {
                    "user.attribute": "userId",
                    "claim.name": "userId",
                    "jsonType.label": "String",
                    "id.token.claim": "true",
                    "access.token.claim": "true",
                    "userinfo.token.claim": "true",
                    "introspection.token.claim": "true",
                },
            }
        ],
    )

    return [profile, email, roles, web_origins, acr, basic, shopfast_profile]


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


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:8180/auth")
    parser.add_argument("--realm", default="shopfast")
    parser.add_argument("--admin-realm", default="master")
    parser.add_argument("--admin-user", default="admin")
    parser.add_argument("--admin-password", required=True)
    parser.add_argument("--dry-run", action="store_true",
                        help="Report what would change without modifying anything.")
    args = parser.parse_args()

    token = get_admin_token(args.base_url, args.admin_realm,
                            args.admin_user, args.admin_password)
    kc = KeycloakAdmin(args.base_url, args.realm, token)

    existing = {s["name"]: s for s in kc.get("/client-scopes")}
    print("Existing client scopes: %s" % sorted(existing))

    changed = False

    # 1. Create any missing built-in scopes.
    for definition in build_scope_definitions():
        name = definition["name"]
        if name in existing:
            print("  [skip]   scope '%s' already exists" % name)
            continue
        if args.dry_run:
            print("  [DRYRUN] would create scope '%s'" % name)
            continue
        kc.post("/client-scopes", definition)
        print("  [create] scope '%s'" % name)
        changed = True

    if changed or args.dry_run:
        existing = {s["name"]: s for s in kc.get("/client-scopes")}

    # 2. Make them realm-wide defaults so future clients inherit them.
    realm_defaults = {s["name"] for s in kc.get("/default-default-client-scopes")}
    for name in ["profile", "email", "roles", "web-origins", "acr", "basic"]:
        if name in realm_defaults or name not in existing:
            continue
        if args.dry_run:
            print("  [DRYRUN] would add '%s' to realm default scopes" % name)
            continue
        kc.put("/default-default-client-scopes/%s" % existing[name]["id"])
        print("  [realm]  '%s' added to realm default scopes" % name)

    realm_optional = {s["name"] for s in kc.get("/default-optional-client-scopes")}
    if "offline_access" in existing and "offline_access" not in realm_optional:
        if args.dry_run:
            print("  [DRYRUN] would add 'offline_access' to realm optional scopes")
        else:
            kc.put("/default-optional-client-scopes/%s"
                   % existing["offline_access"]["id"])
            print("  [realm]  'offline_access' added to realm optional scopes")

    # 3. Bind scopes to the existing clients.
    for client_id in DEFAULT_SCOPE_CLIENTS + SERVICE_CLIENTS:
        found = kc.get("/clients?clientId=%s" % urllib.parse.quote(client_id))
        if not found:
            print("  [warn]   client '%s' not found, skipping" % client_id)
            continue
        uuid = found[0]["id"]

        if client_id in SERVICE_CLIENTS:
            # Service account tokens need roles/basic, not profile/email.
            wanted_default = ["roles", "basic"]
            wanted_optional = []
        else:
            wanted_default = ["profile", "email", "roles", "web-origins",
                              "acr", "basic", "shopfast-profile"]
            # offline_access stays OPTIONAL: browser SPAs must not request it,
            # but native/mobile clients may.
            wanted_optional = ["offline_access"]

        current_default = {s["name"] for s
                           in kc.get("/clients/%s/default-client-scopes" % uuid)}
        current_optional = {s["name"] for s
                            in kc.get("/clients/%s/optional-client-scopes" % uuid)}

        for name in wanted_default:
            if name in current_default or name not in existing:
                continue
            if args.dry_run:
                print("  [DRYRUN] would bind default '%s' -> %s" % (name, client_id))
                continue
            kc.put("/clients/%s/default-client-scopes/%s"
                   % (uuid, existing[name]["id"]))
            print("  [bind]   default '%s' -> %s" % (name, client_id))

        for name in wanted_optional:
            if name in current_optional or name not in existing:
                continue
            if args.dry_run:
                print("  [DRYRUN] would bind optional '%s' -> %s" % (name, client_id))
                continue
            kc.put("/clients/%s/optional-client-scopes/%s"
                   % (uuid, existing[name]["id"]))
            print("  [bind]   optional '%s' -> %s" % (name, client_id))

    print("Done.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
