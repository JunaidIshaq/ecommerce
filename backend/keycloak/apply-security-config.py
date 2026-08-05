#!/usr/bin/env python3
"""
Adds the Keycloak resource server to each service's existing SecurityFilterChain.

The services keep their legacy HS256 filter for now: it authenticates old tokens,
and anything it leaves unauthenticated falls through to the resource server, which
validates Keycloak RS256 tokens. That dual behaviour is what makes the cutover
gradual instead of a flag-day where every client must switch at once.

`jwt(Customizer.withDefaults())` is deliberate - the JwtDecoder and
JwtAuthenticationConverter beans come from common-lib, so the role mapping and
audience checks stay identical across services.
"""
import re
import sys
from pathlib import Path

BACKEND = Path(__file__).resolve().parent.parent

OAUTH_SNIPPET = """                // Migration window: the legacy HS256 filter added below authenticates
                // old tokens; requests it leaves anonymous fall through to here and
                // are validated as Keycloak RS256 tokens against the realm JWKS.
                // Once all clients use Keycloak, delete the filter and JwtUtils.
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
"""

IMPORT_LINE = "import org.springframework.security.config.Customizer;\n"


def patch(path: Path) -> bool:
    text = path.read_text()
    if "oauth2ResourceServer" in text:
        return False

    match = re.search(r"^[ \t]*\.addFilterBefore\(", text, re.MULTILINE)
    if not match:
        print(f"  ! no .addFilterBefore(...) in {path.name}; wire it up by hand")
        return False

    text = text[: match.start()] + OAUTH_SNIPPET + text[match.start():]

    if IMPORT_LINE not in text:
        first_import = text.index("import ")
        text = text[:first_import] + IMPORT_LINE + text[first_import:]

    path.write_text(text)
    return True


def main() -> int:
    configs = sorted(BACKEND.glob("*-service/src/main/java/**/config/SecurityConfig.java"))
    if not configs:
        print("no SecurityConfig files found")
        return 1
    for config in configs:
        service = config.relative_to(BACKEND).parts[0]
        print(f"{service}: {'patched' if patch(config) else 'skipped'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
