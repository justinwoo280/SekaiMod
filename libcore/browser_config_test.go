//go:build with_cronet && with_cronet_test

package libcore

import (
	"fmt"
	"strings"
	"testing"
)

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
