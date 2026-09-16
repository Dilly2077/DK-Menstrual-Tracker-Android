# Lunara development signing

`lunara-development.jks` is a deliberately non-secret development/test signing key used only so GitHub-built APKs from V0.2 onward have a stable certificate and can update one another during sideloaded testing.

It is **not** a production release key and must never be used for Google Play, other stores, or a public production release. The credentials are intentionally stored alongside the test key because this repository configuration is for reproducible development builds, not production signing.

Alias: `lunara-development`
Password: `lunara-development`
Certificate SHA-256: `BD2745001D57B3992FD6D05AD6C0E26C37BB22E74609E5A9C39F4585EF87E3B7`
