//go:build with_cronet && with_cronet_test

package libcore

import (
	"fmt"
	"strings"
	"testing"

	box "github.com/sagernet/sing-box"
	"github.com/sagernet/sing-box/adapter"
	"github.com/sagernet/sing-box/common/certificate"
	"github.com/sagernet/sing-box/option"
	"github.com/sagernet/sing/common/logger"
	"github.com/sagernet/sing/service"
)

// Linux config tests need an explicit store to reproduce Android's unconditional
// system-store registration in box.New.
func TestBrowserAndroidSystemStoreConfig(t *testing.T) {
	for _, protocol := range []string{"vless", "ewp"} {
		for _, mode := range []string{"packet-up", "stream-up"} {
			for _, security := range []string{"tls", "ech", "reality"} {
				t.Run(protocol+"/"+mode+"/"+security, func(t *testing.T) {
					ctx, cancel := newBoxContext(nil)
					defer cancel()
					store, err := certificate.NewStore(ctx, logger.NOP(), option.CertificateOptions{})
					if err != nil {
						t.Fatal(err)
					}
					defer store.Close()
					service.MustRegister[adapter.CertificateStore](ctx, store)
					extra := ""
					if protocol == "ewp" {
						extra = `"server_public_key":"X2jpIR7dIx1fopZbDutNwo628IvU3vhJsL1x5kn5YeU=","server_id":"test",`
					}
					tlsExtra := ""
					switch security {
					case "ech":
						tlsExtra = `,"ech":{"enabled":true,"query_server_name":"ech.example"}`
					case "reality":
						tlsExtra = `,"reality":{"enabled":true,"public_key":"fb4AByryoqmjLlfYbpbpYBIzfkhdf_6KQI5keMH88jk","short_id":"0123abcd"}`
					}
					config := fmt.Sprintf(`{"outbounds":[{
						"type":%q,"tag":"proxy","server":"127.0.0.1","server_port":1,
						"uuid":"01234567-89ab-cdef-0123-456789abcdef",%s
						"tls":{"enabled":true,"server_name":"browser.example"%s},
						"transport":{"type":"xhttp","browser":true,"mode":%q,"path":"/xhttp"}
					}]}`, protocol, extra, tlsExtra, mode)
					var options option.Options
					if err := options.UnmarshalJSONContext(ctx, []byte(config)); err != nil {
						t.Fatal(err)
					}
					instance, err := box.New(box.Options{Options: options, Context: ctx})
					if err != nil {
						t.Fatalf("create service with Android system store: %v", err)
					}
					if err := instance.Close(); err != nil {
						t.Fatal(err)
					}
				})
			}
		}
	}
}

// Exercise the same entry point used by the Android profile editor. The native
// engine is created and closed, but config checking must not dial the endpoint.
func TestCheckBrowserRealityConfig(t *testing.T) {
	for _, mode := range []string{"packet-up", "stream-up"} {
		t.Run(mode, func(t *testing.T) {
			config := fmt.Sprintf(`{
				"outbounds": [{
					"type": "vless",
					"tag": "proxy",
					"server": "127.0.0.1",
					"server_port": 1,
					"uuid": "01234567-89ab-cdef-0123-456789abcdef",
					"tls": {
						"enabled": true,
						"server_name": "camouflage.example",
						"utls": {"enabled": false},
						"reality": {
							"enabled": true,
							"public_key": "fb4AByryoqmjLlfYbpbpYBIzfkhdf_6KQI5keMH88jk",
							"short_id": "0123abcd"
						}
					},
					"transport": {
						"type": "xhttp",
						"browser": true,
						"mode": %q,
						"host": "front.example",
						"path": "/xhttp"
					}
				}]
			}`, mode)
			if err := CheckSingBoxConfig(config, nil); err != nil {
				t.Fatal(err)
			}
			invalid := strings.Replace(config, `"short_id": "0123abcd"`, `"short_id": "invalid"`, 1)
			if err := CheckSingBoxConfig(invalid, nil); err == nil || !strings.Contains(err.Error(), "invalid short_id") {
				t.Fatalf("invalid REALITY credential was not rejected: %v", err)
			}
		})
	}
}
