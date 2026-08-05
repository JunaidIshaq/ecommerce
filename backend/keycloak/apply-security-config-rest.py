#!/usr/bin/env python3
"""
Second pass: the services that the first script could not handle.

Two shapes were left over:
  1. Services carrying a commented-out `.oauth2ResourceServer(...)` line with the
     note "otherwise trust gateway". Trusting the gateway means anything that can
     reach the pod's port bypasses authentication entirely, so those lines get
     uncommented rather than left as a good intention.
  2. Services with no JWT handling at all (every route permitAll). Enabling the
     resource server there does not change who can call what - the route rules are
     untouched - but it does mean a presented token is validated and available for
     method-level checks, which is the prerequisite for locking those routes down.
"""
import re
import sys
from pathlib import Path

BACKEND = Path(__file__).resolve().parent.parent

COMMENTED = re.compile(
    r"[ \t]*//\s*If you want to parse JWT here as resource server; otherwise trust gateway:\s*\n"
    r"[ \t]*//\s*\.oauth2ResourceServer\(oauth2 -> oauth2\.jwt\(Customizer\.withDefaults\(\)\)\);\s*\n"
)

REPLACEMENT = """                // Validate Keycloak tokens here too. "Trust the gateway" would mean any
                // workload that can reach this port is implicitly authenticated.
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
"""

INSERT_BEFORE_FORMLOGIN = """                // Validate Keycloak RS256 tokens against the realm JWKS so that a
                // presented identity is trustworthy and usable by method security.
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
"""

IMPORT_LINE = "import org.springframework.security.config.Customizer;\n"


def ensure_import(text: str) -> str:
    if IMPORT_LINE in text:
        return text
    first_import = text.index("import ")
    return text[:first_import] + IMPORT_LINE + text[first_import:]


def patch(path: Path) -> str:
    text = path.read_text()

    if COMMENTED.search(text):
        text = COMMENTED.sub(REPLACEMENT, text)
        path.write_text(ensure_import(text))
        return "uncommented"

    if ".oauth2ResourceServer" in text:
        return "already done"

    match = re.search(r"^[ \t]*\.formLogin\(", text, re.MULTILINE)
    if not match:
        return "NEEDS MANUAL WIRING"

    text = text[: match.start()] + INSERT_BEFORE_FORMLOGIN + text[match.start():]
    path.write_text(ensure_import(text))
    return "inserted"


def main() -> int:
    for config in sorted(BACKEND.glob("*-service/src/main/java/**/config/SecurityConfig.java")):
        service = config.relative_to(BACKEND).parts[0]
        print(f"{service}: {patch(config)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
