module libcore

go 1.25.5

require (
	github.com/matsuridayo/libneko v1.0.0 // replaced
	github.com/miekg/dns v1.1.72
	github.com/oschwald/maxminddb-golang v1.13.1
	github.com/sagernet/quic-go v0.61.0-sing-box-mod.15
	github.com/sagernet/sing v0.9.7-0.20261125122036-9b11dbe28a7d
	github.com/sagernet/sing-box v1.0.0 // replaced
	github.com/sagernet/sing-tun v0.9.7-0.20261126024553-adc31e26d330
	github.com/ulikunitz/xz v0.5.15
	golang.org/x/mobile v0.0.0-20231108233038-35478a0c49da
	golang.org/x/sys v0.47.0
)

require golang.org/x/net v0.58.0

require (
	github.com/ajg/form v1.5.1 // indirect
	github.com/andybalholm/brotli v1.1.0 // indirect
	github.com/anytls/sing-anytls v0.0.11 // indirect
	github.com/caddyserver/certmagic v0.25.3-0.20260421143802-60d9d8b415d6 // indirect
	github.com/caddyserver/zerossl v0.1.5 // indirect
	github.com/cretz/bine v0.2.0 // indirect
	github.com/database64128/netx-go v0.1.1 // indirect
	github.com/database64128/tfo-go/v2 v2.3.3 // indirect
	github.com/ebitengine/purego v0.10.0 // indirect
	github.com/florianl/go-nfqueue/v2 v2.1.0 // indirect
	github.com/fsnotify/fsnotify v1.9.0 // indirect
	github.com/go-chi/chi/v5 v5.2.5 // indirect
	github.com/go-chi/render v1.0.3 // indirect
	github.com/go-ole/go-ole v1.3.0 // indirect
	github.com/gobwas/httphead v0.1.0 // indirect
	github.com/gobwas/pool v0.2.1 // indirect
	github.com/godbus/dbus/v5 v5.2.2 // indirect
	github.com/gofrs/uuid/v5 v5.5.1 // indirect
	github.com/google/btree v1.1.3 // indirect
	github.com/google/go-cmp v0.7.0 // indirect
	github.com/google/gopacket v1.1.19 // indirect
	github.com/hashicorp/yamux v0.1.2 // indirect
	github.com/huin/goupnp v1.3.0 // indirect
	github.com/jackpal/go-nat-pmp v1.0.2 // indirect
	github.com/jsimonetti/rtnetlink v1.4.1 // indirect
	github.com/justinwoo280/sing-ewp v0.3.2-0.20261023015301-0b89075e7185 // indirect
	github.com/justinwoo280/sing-lwip v0.0.0-20261110151649-0dca048bf681 // indirect
	github.com/justinwoo280/sing-lwip/lib/android_386 v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/android_amd64 v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/android_arm v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/android_arm64 v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/darwin_amd64 v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/darwin_arm64 v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/freebsd_amd64 v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/freebsd_arm64 v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/ios_amd64_simulator v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/ios_arm64 v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/ios_arm64_simulator v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_386_gnu v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_386_musl v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_amd64_gnu v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_amd64_musl v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_arm64_gnu v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_arm64_musl v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_arm_gnu v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_arm_musl v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_loong64_gnu v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_loong64_musl v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_mips64le_gnu v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_mipsle_gnu v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_mipsle_musl v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_ppc64le_gnu v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_ppc64le_musl v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_riscv64_gnu v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_riscv64_musl v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_s390x_gnu v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/linux_s390x_musl v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/tvos_amd64_simulator v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/tvos_arm64 v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/tvos_arm64_simulator v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/windows_386 v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/windows_amd64 v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-lwip/lib/windows_arm64 v0.0.0-20261110151646-c447d5af480c // indirect
	github.com/justinwoo280/sing-xhttp v0.1.7-0.20261123151222-4d9b1bf62aa1 // indirect
	github.com/klauspost/compress v1.19.1 // indirect
	github.com/klauspost/cpuid/v2 v2.3.0 // indirect
	github.com/koron/go-ssdp v0.0.4 // indirect
	github.com/libdns/acmedns v0.5.0 // indirect
	github.com/libdns/alidns v1.0.6 // indirect
	github.com/libdns/cloudflare v0.2.2 // indirect
	github.com/libdns/libdns v1.1.1 // indirect
	github.com/libp2p/go-nat v1.0.1-0.20250821073202-01afc089f138 // indirect
	github.com/libp2p/go-netroute v0.2.1 // indirect
	github.com/logrusorgru/aurora v2.0.3+incompatible // indirect
	github.com/mdlayher/netlink v1.11.2 // indirect
	github.com/mdlayher/socket v0.6.0 // indirect
	github.com/metacubex/utls v1.9.0-alpha.1 // indirect
	github.com/mholt/acmez/v3 v3.1.6 // indirect
	github.com/quic-go/qpack v0.6.0 // indirect
	github.com/sagernet/bbolt v0.0.0-20261124060517-2ab7bd6d2bff // indirect
	github.com/sagernet/cors v1.2.1 // indirect
	github.com/sagernet/cronet-go v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/all v0.0.0-20261126220912-ffdf7fe53517 // indirect
	github.com/sagernet/cronet-go/lib/android_386 v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/android_amd64 v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/android_arm v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/android_arm64 v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/darwin_amd64 v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/darwin_arm64 v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/ios_amd64_simulator v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/ios_arm64 v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/ios_arm64_simulator v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/linux_386 v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/linux_386_musl v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/linux_amd64 v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/linux_amd64_musl v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/linux_arm v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/linux_arm64 v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/linux_arm64_musl v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/linux_arm_musl v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/linux_loong64 v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/linux_loong64_musl v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/linux_mips64le v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/linux_mipsle v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/linux_mipsle_musl v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/linux_riscv64 v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/linux_riscv64_musl v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/tvos_amd64_simulator v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/tvos_arm64 v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/tvos_arm64_simulator v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/windows_amd64 v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/cronet-go/lib/windows_arm64 v0.0.0-20261126220904-c67c5dc12708 // indirect
	github.com/sagernet/fswatch v0.1.2 // indirect
	github.com/sagernet/gvisor v0.0.0-20260727.0-sing-box-mod.1 // indirect
	github.com/sagernet/netlink v0.0.0-20261125120807-b006b90aa077 // indirect
	github.com/sagernet/nftables v0.3.0-mod.4.0.20261114074246-0b310c3e010b // indirect
	github.com/sagernet/sing-mux v0.3.10-0.20261125130604-6c5b46f55eeb // indirect
	github.com/sagernet/sing-quic v0.7.2-0.20261126024618-6fa4bb84d0bd // indirect
	github.com/sagernet/sing-shadowsocks v0.2.9-0.20260929204512-65740e0f0e3e // indirect
	github.com/sagernet/sing-shadowsocks2 v0.2.2-0.20261110092058-52ade8ab7e95 // indirect
	github.com/sagernet/sing-shadowtls v0.2.2-0.20261125124310-5028b2558ebf // indirect
	github.com/sagernet/sing-snell v0.0.0-20261125120456-6dfe590430a8 // indirect
	github.com/sagernet/sing-usbip v0.0.0-20260817040617-28bd42667eca // indirect
	github.com/sagernet/sing-vmess v0.2.9-0.20261124075649-4acc92a6f6c6 // indirect
	github.com/sagernet/smux v1.5.50-sing-box-mod.1 // indirect
	github.com/sagernet/wireguard-go v0.0.8-0.20261125120826-afd160a056dc // indirect
	github.com/sagernet/ws v0.0.0-20231204124109-acfe8907c854 // indirect
	github.com/vishvananda/netns v0.0.5 // indirect
	github.com/zeebo/blake3 v0.2.4 // indirect
	go.uber.org/multierr v1.11.0 // indirect
	go.uber.org/zap v1.27.1 // indirect
	go.uber.org/zap/exp v0.3.0 // indirect
	go4.org/netipx v0.0.0-20231129151722-fdeea329fbba // indirect
	golang.org/x/crypto v0.55.0 // indirect
	golang.org/x/exp v0.0.0-20260410095643-746e56fc9e2f // indirect
	golang.org/x/mod v0.38.0 // indirect
	golang.org/x/sync v0.22.0 // indirect
	golang.org/x/text v0.41.0 // indirect
	golang.org/x/time v0.15.0 // indirect
	golang.org/x/tools v0.48.0 // indirect
	golang.zx2c4.com/wintun v0.0.0-20230126152724-0fa3db229ce2 // indirect
	google.golang.org/genproto/googleapis/rpc v0.0.0-20251202230838-ff82c1b0f217 // indirect
	google.golang.org/grpc v1.79.3 // indirect
	google.golang.org/protobuf v1.36.11 // indirect
	lukechampine.com/blake3 v1.3.0 // indirect
)

replace github.com/matsuridayo/libneko => ../../libneko

replace github.com/sagernet/sing-box => ../../sing-box

replace github.com/sagernet/cronet-go => github.com/justinwoo280/cronet-go v0.0.0-20261126220904-c67c5dc12708

replace github.com/sagernet/cronet-go/all => github.com/justinwoo280/cronet-go/all v0.0.0-20261126220912-ffdf7fe53517

replace github.com/sagernet/cronet-go/lib/linux_amd64 => github.com/justinwoo280/cronet-go/lib/linux_amd64 v0.0.0-20261126220904-c67c5dc12708

replace github.com/sagernet/cronet-go/lib/android_arm64 => github.com/justinwoo280/cronet-go/lib/android_arm64 v0.0.0-20261126220904-c67c5dc12708

replace (
	github.com/sagernet/cronet-go/lib/android_386 => github.com/justinwoo280/cronet-go/lib/android_386 v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/android_amd64 => github.com/justinwoo280/cronet-go/lib/android_amd64 v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/android_arm => github.com/justinwoo280/cronet-go/lib/android_arm v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/darwin_amd64 => github.com/justinwoo280/cronet-go/lib/darwin_amd64 v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/darwin_arm64 => github.com/justinwoo280/cronet-go/lib/darwin_arm64 v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/ios_amd64_simulator => github.com/justinwoo280/cronet-go/lib/ios_amd64_simulator v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/ios_arm64 => github.com/justinwoo280/cronet-go/lib/ios_arm64 v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/ios_arm64_simulator => github.com/justinwoo280/cronet-go/lib/ios_arm64_simulator v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/linux_386 => github.com/justinwoo280/cronet-go/lib/linux_386 v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/linux_386_musl => github.com/justinwoo280/cronet-go/lib/linux_386_musl v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/linux_amd64_musl => github.com/justinwoo280/cronet-go/lib/linux_amd64_musl v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/linux_arm => github.com/justinwoo280/cronet-go/lib/linux_arm v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/linux_arm64 => github.com/justinwoo280/cronet-go/lib/linux_arm64 v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/linux_arm64_musl => github.com/justinwoo280/cronet-go/lib/linux_arm64_musl v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/linux_arm_musl => github.com/justinwoo280/cronet-go/lib/linux_arm_musl v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/linux_loong64 => github.com/justinwoo280/cronet-go/lib/linux_loong64 v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/linux_loong64_musl => github.com/justinwoo280/cronet-go/lib/linux_loong64_musl v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/linux_mips64le => github.com/justinwoo280/cronet-go/lib/linux_mips64le v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/linux_mipsle => github.com/justinwoo280/cronet-go/lib/linux_mipsle v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/linux_mipsle_musl => github.com/justinwoo280/cronet-go/lib/linux_mipsle_musl v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/linux_riscv64 => github.com/justinwoo280/cronet-go/lib/linux_riscv64 v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/linux_riscv64_musl => github.com/justinwoo280/cronet-go/lib/linux_riscv64_musl v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/tvos_amd64_simulator => github.com/justinwoo280/cronet-go/lib/tvos_amd64_simulator v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/tvos_arm64 => github.com/justinwoo280/cronet-go/lib/tvos_arm64 v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/tvos_arm64_simulator => github.com/justinwoo280/cronet-go/lib/tvos_arm64_simulator v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/windows_amd64 => github.com/justinwoo280/cronet-go/lib/windows_amd64 v0.0.0-20261126220904-c67c5dc12708
	github.com/sagernet/cronet-go/lib/windows_arm64 => github.com/justinwoo280/cronet-go/lib/windows_arm64 v0.0.0-20261126220904-c67c5dc12708
)

replace github.com/sagernet/sing-tun => github.com/justinwoo280/sing-tun v0.9.7-0.20261126133200-ff0e24508fc9
