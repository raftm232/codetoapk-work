# Security hardening notes

## Android
- Release builds now enable R8 code shrinking/obfuscation and resource shrinking.
- The existing GitHub Actions workflow builds a **debug** APK. Debug APKs are not protected by the release R8 configuration.
- To produce a hardened release APK, configure a persistent Android signing keystore and set `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` in the build environment, then run `gradle :app:assembleRelease`.
- Keep the keystore and passwords private. Do not commit them to GitHub.
- Test the release APK carefully; reflection-based libraries may need narrowly scoped keep rules.

## Web
- Browser-delivered HTML/JavaScript cannot be kept secret from users. Minification/obfuscation only raises the effort required to read it; it is not encryption.
- Do not put private encryption keys, passwords, or proprietary secrets in `web/index.html`. Move sensitive operations to a server if they must remain secret.
- The web page has not been transformed in this package to avoid breaking its inline JavaScript or changing behavior.

## Limits
No client-side protection can make code impossible to reverse engineer. This package changes build configuration; it does not itself contain a newly built APK.
