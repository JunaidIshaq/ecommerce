#!/usr/bin/env python3
"""
Fixes placement of the oauth2ResourceServer call.

The commented-out template line sat *after* the statement-terminating semicolon,
so uncommenting it in place produced a dangling expression that does not compile.
This moves the call inside the builder chain, just before .formLogin(...).
"""
import re
import sys
from pathlib import Path

BACKEND = Path(__file__).resolve().parent.parent

STRAY = re.compile(
    r"\n[ \t]*// Validate Keycloak tokens here too\. \"Trust the gateway\" would mean any\n"
    r"[ \t]*// workload that can reach this port is implicitly authenticated\.\n"
    r"[ \t]*\.oauth2ResourceServer\(oauth2 -> oauth2\.jwt\(Customizer\.withDefaults\(\)\)\)\n"
)

INSERT = """                // Validate Keycloak tokens here too. "Trust the gateway" would mean any
                // workload that can reach this port is implicitly authenticated.
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
"""


def patch(path: Path) -> str:
    text = path.read_text()
    if not STRAY.search(text):
        return "ok"

    text = STRAY.sub("\n", text)

    anchor = re.search(r"^[ \t]*\.formLogin\(", text, re.MULTILINE)
    if not anchor:
        return "NEEDS MANUAL WIRING"

    text = text[: anchor.start()] + INSERT + text[anchor.start():]
    path.write_text(text)
    return "fixed"


def main() -> int:
    for config in sorted(BACKEND.glob("*-service/src/main/java/**/config/SecurityConfig.java")):
        print(f"{config.relative_to(BACKEND).parts[0]}: {patch(config)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
