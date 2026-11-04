# Cameo MCP Bridge 3.3.2 — 2024x and 2022x

Fixes Environment Options that were shown but never applied. The plugin read its
settings during initialization, before Cameo applies the saved values, so the
listeners always started with the built-in defaults. Builds 3.1.0 to 3.3.1 ignored a
saved native **Listening port**; 3.3.0 and 3.3.1 also ignored the saved browser
port, session duration, allowed-origin list and enabled flag. A browser origin added
under Environment Options → Cameo MCP Bridge → Browser service was refused with
"Origin is not configured in Cameo Environment Options" even after a restart. The
`cameo.mcp.port` system property was unaffected.

The plugin now registers its options group during initialization and starts both
listeners as an after-startup activity, once the saved values are applied. The
browser service's startup log line names its effective port and allowed origins. The
native API starts when Cameo's startup finishes rather than during plugin
initialization; a client that connects in that window sees a refused connection, as
while Cameo is starting, and can connect once startup finishes. On a host without
the after-startup API the plugin reports that it cannot apply saved settings and
starts with the defaults, as before.

## Upgrading

Values saved while they were ignored take effect for the first time. Before
upgrading, review Environment Options → Cameo MCP Bridge:

- A changed **Listening port** moves the native API; set the Python client's
  `CAMEO_BRIDGE_PORT` to the same value.
- One invalid entry in **Allowed browser origins** (a path, a trailing slash, plain
  HTTP for a non-localhost host) keeps the browser service from starting; the GUI log
  says why.
- A cleared **Enable browser service** now turns the browser service off.

The Cameo log records the browser port and origins in effect at startup.

Install only the ZIP matching your Cameo target. Native Python clients and other
exact-version consumers must be updated to expect 3.3.2.

## Validation

- Root cause and fix traced in the 2024x host's startup code: plugin groups receive
  saved values after every plugin's `init()`, and after-startup activities run after
  that, in GUI and command-line starts alike.
- Plugin tests (66, including the new deferred-start and origin-list tests) and SDK
  linkage on Java 17 ran against a 2024x Refresh 3 installation. That release no
  longer has `DiagramPresentationElement.isDiagramWindowOpen/isDiagramWindowActive`
  (used by the Relation Map handler, unchanged here), so this source does not compile
  against it as is; those two calls were stubbed in a scratch copy for the test run.
- Release ZIPs come from the documented 2024x (Refresh 2) and 2022x gates, which were
  not run for this change. The 2022x linkage check of the after-startup API is part of
  that gate.
- Live acceptance (an origin added in Environment Options is accepted after a
  restart) must be checked separately.
