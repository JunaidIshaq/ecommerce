#!/usr/bin/env python3
"""
One-off migration helper: wire every service up as an OIDC resource server.

Doing this by hand across 12 services is how you end up with one service quietly
left on the old shared-secret validation. The script is idempotent, so it is safe
to re-run after adding a new service.

It only touches build/config files - the SecurityFilterChain changes are
deliberately left to a human, because each service has its own set of public
endpoints and getting those wrong either breaks traffic or opens a hole.
"""
import re
import sys
from pathlib import Path

BACKEND = Path(__file__).resolve().parent.parent

SERVICES = [
    "user-service", "product-service", "category-service", "cart-service",
    "order-service", "payment-service", "inventory-service", "coupon-service",
    "notification-service", "review-service", "elastic-service", "admin-service",
    "auth-service",
]

POM_DEP = """
        <!-- Validates Keycloak RS256 tokens offline against the realm's JWKS. -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
"""

YML_BLOCK = """
---
# --- Keycloak resource server ------------------------------------------------
# Appended as a second YAML document (note the ---) on purpose: the files already
# have a top-level `spring:` key and a duplicate key in one document makes Spring
# Boot refuse to start. Separate documents are merged instead.
# Tokens are verified offline against the realm's public JWKS, fetched from the
# issuer's discovery document and refreshed automatically on key rotation.
# Nothing here is a secret: the service only ever needs the PUBLIC key.
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://keycloak:8180/realms/shopfast}

shopfast:
  security:
    # Service-to-service credentials. Absent in local runs, in which case the
    # Feign interceptor simply relays the caller's token instead.
    service-client:
      client-id: shopfast-services
      client-secret: ${SHOPFAST_SERVICES_CLIENT_SECRET:}
"""

MARKER = "spring-boot-starter-oauth2-resource-server"
YML_MARKER = "resourceserver"


def patch_pom(path: Path) -> bool:
    text = path.read_text()
    if MARKER in text:
        return False
    anchor = re.search(
        r"[ \t]*<dependency>\s*<groupId>org\.springframework\.boot</groupId>\s*"
        r"<artifactId>spring-boot-starter-security</artifactId>\s*</dependency>",
        text,
    )
    if not anchor:
        print(f"  ! no spring-boot-starter-security in {path}, add the dependency manually")
        return False
    text = text[: anchor.end()] + POM_DEP + text[anchor.end():]
    path.write_text(text)
    return True


def patch_yml(path: Path) -> bool:
    text = path.read_text()
    if YML_MARKER in text:
        return False
    path.write_text(text.rstrip() + "\n" + YML_BLOCK)
    return True


def main() -> int:
    for service in SERVICES:
        root = BACKEND / service
        if not root.is_dir():
            print(f"skip {service} (missing)")
            continue
        print(service)
        pom = root / "pom.xml"
        if pom.is_file() and patch_pom(pom):
            print("  + oauth2 resource server dependency")
        yml = root / "src/main/resources/application.yml"
        if yml.is_file() and patch_yml(yml):
            print("  + issuer-uri config")
    return 0


if __name__ == "__main__":
    sys.exit(main())
