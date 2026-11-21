# SekaiMod

[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![Builds](https://github.com/justinwoo280/SekaiMod/actions/workflows/preview.yml/badge.svg?branch=1.14.x)](https://github.com/justinwoo280/SekaiMod/actions/workflows/preview.yml?query=branch%3A1.14.x)
[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-orange.svg)](https://www.gnu.org/licenses/gpl-3.0)

sing-box / universal proxy toolchain for Android.

一款使用 sing-box 的 Android 通用代理软件.

## 下载 / Downloads

[下载 `1.14.x` 构建 / Download builds](https://github.com/justinwoo280/SekaiMod/actions/workflows/preview.yml?query=branch%3A1.14.x)

**Google Play 版本自 2024 年 5 月起已被第三方控制，为非开源版本，请不要下载。**

**The Google Play version has been controlled by a third party since May 2024 and is a non-open
source version. Please do not download it.**

## 项目主页 / Project

https://github.com/justinwoo280/SekaiMod/tree/1.14.x

Based on [NekoBox for Android](https://github.com/MatsuriDayo/NekoBoxForAndroid).
Upstream usage documentation: https://matsuridayo.github.io

## 支持的代理协议 / Supported Proxy Protocols

* SOCKS (4/4a/5)
* HTTP(S)
* SSH
* Shadowsocks
* VMess
* Trojan
* VLESS
* AnyTLS
* Snell (v4 / v6)
* ShadowTLS
* TUIC
* Hysteria 1/2
* WireGuard
* Trojan-Go (trojan-go-plugin)
* NaïveProxy (naive-plugin)
* Mieru (mieru-plugin)

请到[这里](https://matsuridayo.github.io/nb4a-plugin/)下载插件以获得完整的代理支持.

Please visit [here](https://matsuridayo.github.io/nb4a-plugin/) to download plugins for full proxy
supports.

## Browser XHTTP

See [the 1.14.x protocol/editor compatibility notes](PROTOCOLS_1.14.md) for
Snell and the updated AnyTLS, TUIC, Hysteria2 and SSH options.

VLESS and EWP profiles support Cronet's `packet-up`, `stream-up` and
`stream-one` modes. Browser `stream-one` requires TLS. Enabling **gRPC Framing**
selects a streaming mode and emits `grpc_framing: true`; the server must enable
the same option. Leave it disabled for stock Xray endpoints.

With framing enabled, the path names a gRPC service, such as `/xhttp`, and the
POST targets `/xhttp/Tun`. Framed `stream-up` still needs an ordinary HTTP
streaming route for its separate download GET. VLESS/EWP links preserve the
Browser switch, stream mode and framing option. Existing saved profiles keep
framing disabled.

## TUN stacks

The `1.14.x` build offers **Go**, **gVisor**, **System**, **Mixed** and **lwIP** in
the TUN implementation setting. New installations default to Go; saved selections
retain their previous meanings. The configuration generator emits the matching
`go`, `gvisor`, `system`, `mixed` or `lwip` stack name.

LibCore links lwIP statically through CGO on all four Android ABIs (API 23+),
alongside the existing Cronet support. The APK includes native notices under
`assets/licenses/sing-lwip/`. Core revisions are pinned in
`buildScript/lib/core/get_source_env.sh`, with the matching sing-tun and native
library modules recorded in `libcore/go.mod` and `go.sum`.

## 支持的订阅格式 / Supported Subscription Format

* 一些广泛使用的格式 (如 Shadowsocks, ClashMeta 和 v2rayN)
* sing-box 出站

仅支持解析出站，即节点。分流规则等信息会被忽略。

* Some widely used formats (like Shadowsocks, ClashMeta and v2rayN)
* sing-box outbound

Only resolving outbound, i.e. nodes, is supported. Information such as diversion rules are ignored.

## Routing and DNS

User rules retain their configured order. DNS routing derives rules only from
domain, app and source-address conditions. Rules containing destination IP,
port, transport, sniffed protocol or custom JSON conditions stay in traffic
routing: those conditions cannot be safely inferred from a DNS query.

For example, `geosite:cn` can select direct DNS, while `geoip:cn` still selects
the bypass outbound without becoming an unconditional direct-DNS rule. The
built-in UDP/443 block rule likewise does not block unrelated DNS queries.
Proxy endpoint and ECH bootstrap DNS rules remain narrowly scoped to their
names. FakeDNS is limited to A/AAAA queries.

## Credits

Core:

- [SagerNet/sing-box](https://github.com/SagerNet/sing-box)

Android GUI:

- [shadowsocks/shadowsocks-android](https://github.com/shadowsocks/shadowsocks-android)
- [SagerNet/SagerNet](https://github.com/SagerNet/SagerNet)

Web Dashboard:

- [Yacd-meta](https://github.com/MetaCubeX/Yacd-meta)
