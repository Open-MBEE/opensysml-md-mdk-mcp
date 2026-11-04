# Cameo MCP Bridge 3.3.2 — 2024x and 2022x

Fixes Environment Options that were shown but never applied. Earlier 3.3.x builds
read their settings during plugin initialization, before Cameo applies the saved
values, so the native API and the browser service always started with the built-in
defaults: a changed listening port, browser port, session duration, allowed-origin
list or enabled flag had no effect. A browser origin added under Environment Options
→ Cameo MCP Bridge → Browser service was refused with "Origin is not configured in
Cameo Environment Options" even after a restart.

The plugin now registers its options group during initialization and starts both
listeners as an after-startup activity, once the saved values are applied. The
browser service's startup log line names its effective port and allowed origins. The
native API starts when Cameo's startup finishes rather than during plugin
initialization; clients that connect in that window see a refused connection and
retry as before. Restart Cameo after changing a setting.

Install only the ZIP matching your Cameo target. Native Python clients and other
exact-version consumers must be updated to expect 3.3.2.

## Validation

- 2024x: plugin tests and SDK linkage on Java 17 (including the after-startup
  activity API) against a 2024x Refresh 3 installation; the packaged ZIP passes
  `scripts/verify_plugin_zip.py`.
- 2022x: not built for this change; the linkage probe checks the after-startup
  activity API on that target.
- Live acceptance (an origin added in Environment Options is accepted after a
  restart) must be checked separately.
