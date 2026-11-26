# sing-box mod 1.14.x line, which carries:
#   - Upstream stable@8dbc70be (post-v1.14.4 maintenance), with scoped
#     lifecycle cleanup and resolver-aware ECH for upload/download TLS.
#   - sing-tun lwip@ff0e245 plus sing/dev@9b11dbe; Go remains the default.
#   - Go userspace TUN stack as the default, with optional static-CGO lwIP;
#     system, gVisor and mixed remain selectable. Native dependencies are
#     pinned together with the sing-tun integration in libcore/go.mod.
#   - Cronet Chromium 154.0.8037.49, Chrome Root Store v45 and root-derived
#     Trust Anchor IDs; sing-lwip includes the pinned fragment reassembly fixes.
#   - Cronet wrapper/native c67c5dc12708 and all ffdf7fe53517 include selective
#     async socket scheduling, Naive resolver/reset, C-header initialization,
#     BSD UDP EOF and Windows TCP metrics fixes from the maintained native fork.
#   - Browser XHTTP selects HTTP/3 with tls.alpn=["h3"] and routes UDP through
#     the outbound; explicit h2/http/1.1 offers are preserved for TCP/TLS.
#   - Android Strict ECH retains the configured internal DNS backend, fixing
#     the native VPN-service crash after a successful ECH configuration lookup.
#   - Browser download termination stops its upload; network changes reset the
#     native pool while allowing later dials. Cronet pins include the matching
#     native HTTP/2 pool fixes and smaller initial packet-up buffers.
#   - Browser XHTTP stream-one and opt-in protobuf/gRPC streaming framing;
#     the default raw format stays compatible with stock Xray.
#   - EWP UDP adapters preserve IPv4, IPv6 and domain destinations through
#     both DialContext and ListenPacket.
#   - EWP/v2.3.1 (sing-ewp pseudo-version): ticket resumption skips
#     HelloRetry and currently takes 2 RTT to authenticated application data;
#     rejected/expired tickets silently fall back to the full v2.3 handshake.
#     Every resumption still runs a fresh hybrid X25519+ML-KEM exchange
#     (tickets never feed session keys); no 0-RTT. Capability negotiation
#     requires a v2.3.1 server and is not an automatic v2.3.0 downgrade.
#     The sing-box outbound enables it with zero config surface.
#   - EWP/v2.3 (sing-ewp v0.3.0): six-stage authenticated handshake with
#     full transcript binding, Ed25519 server signing identity + signed
#     short-term outer keys (replacing the v2.1 static X25519 server key),
#     hybrid X25519+ML-KEM-768 session keys, stateless cookie + admission
#     control, in-window replay rejection, AnyTLS-style opening-phase
#     refragmentation, and async TCP/UDP handoff fixes (gRPC/xhttp).
#     Config: outbound takes uuid + server_public_key + server_id
#     (+ route_epoch); inbound takes users + signing_private_key + server_id.
#   - Ordinary XHTTP uses the sing-xhttp revision pinned in libcore/go.mod.
#     The bridge supports custom headers, padding and session placement, plus
#     independent download endpoints, TLS and XMUX settings. It rejects
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
export COMMIT_SING_BOX="287d5e85e8bebe3d986606c4ee98cf924f4293d3"
export COMMIT_LIBNEKO="1c47a3af71990a7b2192e03292b4d246c308ef0b"
