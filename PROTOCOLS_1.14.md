# Android protocol options for sing-box 1.14.x

The Android editors and generators target the core revision pinned in
`buildScript/lib/core/get_source_env.sh`. Generic dialer options and advanced
protocol fields can also be supplied through each profile's custom outbound JSON.

The current core integrates upstream `stable@8dbc70be`, a post-`v1.14.4`
maintenance snapshot. libcore uses its scoped lifecycle and resolver-aware ECH
interfaces, together with `sing-tun/lwip@ff0e245` and `sing/dev@9b11dbe`.
The matched Browser-capable Cronet fork archives remain pinned separately;
the core's `release/UPSTREAM_1.14.md` records that native compatibility boundary.

## Snell

Snell is registered in the Android native outbound registry and has its own
profile type, editor, database column, configuration generator and import support.

- Client versions: **4 and 6**. The core uses a version 5 server for the v4 wire
  protocol; a version 5 client is not a separate supported option.
- Common fields: server, port, PSK, optional user key, connection reuse and
  enabled networks.
- Version 4: `obfs_mode` (`none`, `http`, `tls`) and `obfs_host`, as supported by
  the pinned native implementation.
- Version 6: `mode` (`default`, `unshaped`, `unsafe-raw`). The editor keeps the
  settings for each version, while the generator emits only the active variant.
- Import: Clash-style Snell entries, individual sing-box Snell outbounds,
  sing-box outbound lists, and the existing `sn://snell` universal profile format.
  Unknown sing-box dialer fields are retained in custom outbound JSON.
  Clash imports must specify version 4 or 6 explicitly.
- Sharing uses the app's universal profile format, preserving every field.

Room schema 8 adds a nullable Snell blob through a 7→8 automatic migration.
Existing profile type IDs are retained; Snell uses the new ID 24.

## Updated editors and generators

| Protocol | Added or corrected options |
| --- | --- |
| AnyTLS | `idle_session_check_interval`, `idle_session_timeout`, `min_idle_session`, `client_metadata`; ALPN and the new settings survive share links |
| TUIC | `udp_over_stream`, duration-valued `heartbeat`; `zero_rtt_handshake` survives share links; UDP-over-stream suppresses the conflicting `udp_relay_mode` |
| Hysteria2 | Gecko/salamander/disabled obfuscation, Gecko min/max packet sizes, `hop_interval_max`, `bbr_profile`, `disable_chrome_parrot`, `brutal_debug`; receive windows and PMTU controls are available for v2 as well as v1 |
| Hysteria 1/2 | Mixed individual ports and ranges are retained; single entries in `server_ports` become single-point ranges such as `443:443`; the first port in a hyphenated range is preserved when sharing |
| SSH | Host-key algorithms, client version, cipher list, MAC list and key-exchange algorithms |

New fields are appended behind versioned Kryo records. Readers continue to
accept AnyTLS v1, TUIC v3, Hysteria v8 and SSH v0 records and initialize the added
fields with compatible defaults. Hysteria2 always uses the native core even
when a profile retains an old Hysteria1 fake-TCP/plugin transport selection.

Duration units are preserved across formats: Clash AnyTLS numeric session
intervals are seconds, while TUIC `heartbeat-interval` is milliseconds. Native
sing-box duration fields are emitted as strings with units.

Typed sing-box JSON is recognized before legacy JSON heuristics, so Hysteria2
with `up_mbps` or only `server_ports` is preserved as a native custom outbound
instead of being misclassified as a Hysteria1 profile.

## Other surfaces checked

- VLESS/VMess/Trojan/EWP Browser XHTTP options retain their existing ALPN,
  framing, ECH and REALITY integration.
- WireGuard uses the endpoint form required by 1.14.x.
- Naïve profiles currently use the external Naïve plugin's configuration format;
  the native core's Naïve-specific tuning fields are a separate integration.
- Hysteria Realm, server/inbound settings, and platform-specific TLS engines do
  not map directly to these Android outbound editors. Advanced outbound options
  remain representable through custom JSON.

## Verification

`Protocol14Test` checks serialization compatibility, actual subscription import
paths, variant-specific generated JSON, link round trips and duration/port
conversion. Set `SEKAIMOD_PROTOCOL_FIXTURE` to export its generated configurations.
`TestGeneratedProtocol14Configurations` in libcore consumes that file through
`CheckSingBoxConfig`, exercising the real Android protocol registry and core.

`TestSnellAndroidOutboundInterop` exercises v4 (`none`/`http`/`tls`) and all three
v6 modes, both with and without connection reuse, using loopback
TCP and UDP echo traffic. No external endpoint is required.
