# sing-box mod 1.14.x line, which carries:
#   - EWP/v2.3.1 (sing-ewp v0.3.1): ticket-based 1-RTT resumption on top
#     of v2.3 — servers mint rotating tickets after every handshake and
#     clients with an in-memory store resume in 1 RTT (1.5-RTT to data)
#     instead of the six-stage 3-RTT; rejected/expired tickets silently
#     fall back to the full handshake, and no store means byte-identical
#     v2.3.0 behavior. Every resumption still runs a fresh hybrid
#     X25519+ML-KEM exchange (tickets never feed session keys); no 0-RTT.
#     The sing-box outbound enables it with zero config surface.
#   - EWP/v2.3 (sing-ewp v0.3.0): six-stage authenticated handshake with
#     full transcript binding, Ed25519 server signing identity + signed
#     short-term outer keys (replacing the v2.1 static X25519 server key),
#     hybrid X25519+ML-KEM-768 session keys, stateless cookie + admission
#     control, in-window replay rejection, AnyTLS-style opening-phase
#     refragmentation, and async TCP/UDP handoff fixes (gRPC/xhttp).
#     Config: outbound takes uuid + server_public_key + server_id
#     (+ route_epoch); inbound takes users + signing_private_key + server_id.
#   - xhttp: transport delegated to the standalone sing-xhttp v0.1.7 library
#     (full Xray splithttp parity, REALITY/HTTP3/uTLS, per-mode defaults);
#     in-tree transport/v2rayxhttp is now a thin bridge. v0.1.7 rejects
#     `alpn: ["h3"]` combined with uTLS or REALITY instead of silently falling
#     back to HTTP/2 over TCP: quic-go runs the TLS 1.3 handshake through
#     crypto/tls and exposes no hook for a caller-supplied ClientHello, so
#     HTTP/3 needs a standard TLS client. v0.1.5 completes the
#     Chromium-like H2 client frame adaptation without forking x/net/http2:
#     SETTINGS order/presence, session WINDOW_UPDATE, HEADERS priority,
#     RFC 9218 priority header, and request pseudo-header order are aligned;
#     client-side only, wire format unchanged. v0.1.1 fixes the packet-up/
#     stream-* deadlock through response-buffering middleboxes (Cloudflare) by
#     returning the download GET at connection time.
#   - reality: keep X25519MLKEM768 in ClientHello + correct PQC auth key
#     selection (byte-identical Chrome fingerprint, working REALITY auth)
#   - 1.14 port: NekoBox private layer (conntrack + legacy libbox
#     platform.Interface) re-applied; oomprofile parses /proc/self/maps
#     locally instead of linkname'ing runtime/pprof internals (Go 1.26
#     rejects pull-mode linkname); CI follows the fork's 1.13.x workflows.
export COMMIT_SING_BOX="e21c669bd7783efc10eeefca7c1ad832a50e8fd3"
export COMMIT_LIBNEKO="1c47a3af71990a7b2192e03292b4d246c308ef0b"
