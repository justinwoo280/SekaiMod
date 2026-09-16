package libcore

import (
	"testing"
)

const testSingBox14Config = `{
  "log": { "level": "warn" },
  "dns": {
    "servers": [
      { "type": "local", "tag": "dns-local" },
      { "type": "https", "tag": "dns-direct", "server": "223.5.5.5", "path": "/dns-query", "detour": "direct" },
      { "type": "https", "tag": "dns-remote", "server": "dns.google", "path": "/dns-query", "domain_resolver": "dns-direct" },
      { "type": "fakeip", "tag": "dns-fake", "inet4_range": "198.18.0.0/15", "inet6_range": "fc00::/18" }
    ],
    "rules": [
      { "inbound": ["tun-in"], "server": "dns-fake" },
      { "domain": ["example.com"], "server": "dns-direct" },
      { "query_type": ["AAAA"], "action": "predefined", "rcode": "NOERROR" }
    ],
    "strategy": "prefer_ipv4",
    "final": "dns-remote"
  },
  "inbounds": [
    {
      "type": "tun",
      "tag": "tun-in",
      "address": ["172.19.0.1/30"],
      "mtu": 9000
    },
    { "type": "mixed", "tag": "mixed-in", "listen": "127.0.0.1", "listen_port": 2080 }
  ],
  "endpoints": [
    {
      "type": "wireguard",
      "tag": "wg",
      "address": ["172.16.0.2/32"],
      "private_key": "9sJE4+SWNTDEFOMg+XTVbsSGHJdPpY0lvoOlFBWa86A=",
      "peers": [
        {
          "address": "wg.example.com",
          "port": 51820,
          "public_key": "9sJE4+SWNTDEFOMg+XTVbsSGHJdPpY0lvoOlFBWa86A=",
          "allowed_ips": ["0.0.0.0/0"],
          "reserved": "AQID"
        }
      ]
    }
  ],
  "outbounds": [
    { "type": "selector", "tag": "proxy", "outbounds": ["ewp", "vless", "wg"] },
    {
      "type": "ewp",
      "tag": "ewp",
      "server": "ewp.example.com",
      "server_port": 443,
      "uuid": "01234567-89ab-cdef-0123-456789abcdef",
      "server_public_key": "X2jpIR7dIx1fopZbDutNwo628IvU3vhJsL1x5kn5YeU=",
      "server_id": "ewp-server",
      "route_epoch": 1,
      "tls": {
        "enabled": true,
        "server_name": "cdn.example.com",
        "reality": {
          "enabled": true,
          "public_key": "fb4AByryoqmjLlfYbpbpYBIzfkhdf_6KQI5keMH88jk",
          "short_id": "0123abcd"
        },
        "utls": { "enabled": true, "fingerprint": "chrome" }
      },
      "transport": {
        "type": "xhttp",
        "mode": "auto",
        "host": "cdn.example.com",
        "path": "/xhttp"
      }
    },
    {
      "type": "vless",
      "tag": "vless",
      "server": "vless.example.com",
      "server_port": 443,
      "uuid": "01234567-89ab-cdef-0123-456789abcdef",
      "tls": {
        "enabled": true,
        "server_name": "cdn.example.com",
        "ech": { "enabled": true, "query_server_name": "dns.google" },
        "utls": { "enabled": true, "fingerprint": "chrome" }
      },
      "transport": {
        "type": "xhttp",
        "mode": "packet-up",
        "host": "cdn.example.com",
        "path": "/xhttp"
      }
    },
    {
      "type": "anytls",
      "tag": "anytls",
      "server": "anytls.example.com",
      "server_port": 443,
      "password": "test-password",
      "tls": {
        "enabled": true,
        "server_name": "cdn.example.com",
        "ech": { "enabled": true, "query_server_name": "dns.google" },
        "utls": { "enabled": true, "fingerprint": "chrome" }
      }
    },
    {
      "type": "hysteria2",
      "tag": "hy2",
      "server": "hy2.example.com",
      "server_port": 443,
      "password": "test-password",
      "tls": {
        "enabled": true,
        "server_name": "cdn.example.com",
        "ech": { "enabled": true, "query_server_name": "dns.google" }
      }
    },
    {
      "type": "tuic",
      "tag": "tuic",
      "server": "tuic.example.com",
      "server_port": 443,
      "uuid": "01234567-89ab-cdef-0123-456789abcdef",
      "password": "test-password",
      "congestion_control": "cubic",
      "tls": {
        "enabled": true,
        "server_name": "cdn.example.com",
        "ech": { "enabled": true, "query_server_name": "dns.google" }
      }
    },
    { "type": "direct", "tag": "direct" },
    { "type": "direct", "tag": "bypass" }
  ],
  "route": {
    "default_domain_resolver": "dns-direct",
    "rules": [
      { "action": "sniff" },
      { "action": "resolve", "strategy": "prefer_ipv4" },
      { "protocol": ["dns"], "action": "hijack-dns" },
      { "domain": ["example.org"], "action": "reject" }
    ],
    "final": "proxy"
  }
}`

func TestCheckSingBoxConfig14(t *testing.T) {
	if err := CheckSingBoxConfig(testSingBox14Config, nil); err != nil {
		t.Fatal(err)
	}
}

func TestCheckSingBoxConfigRejectsRemovedFields(t *testing.T) {
	cases := map[string]string{
		"legacy DNS server": `"dns":{"servers":[{"address":"https://223.5.5.5/dns-query"}]}`,
		"legacy DNS fakeip": `"dns":{"fakeip":{"enabled":true}}`,
		"legacy WireGuard":  `"outbounds":[{"type":"wireguard","tag":"wg","server":"example.com"}]`,
	}
	for name, content := range cases {
		content = "{" + content + "}"
		if err := CheckSingBoxConfig(content, nil); err == nil {
			t.Errorf("%s: expected error", name)
		} else {
			t.Logf("%s: %v", name, err)
		}
	}
}
