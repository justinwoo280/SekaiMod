package libcore

import (
	"context"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"net/url"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/sagernet/sing-box/common/tls"
	"github.com/sagernet/sing-box/option"
	"github.com/sagernet/sing-box/transport/v2ray"
	"github.com/sagernet/sing/common/logger"
	M "github.com/sagernet/sing/common/metadata"
	N "github.com/sagernet/sing/common/network"

	"golang.org/x/net/http2/hpack"
)

// XHTTPExtraTest writes the real serialized Android outbounds after a share
// link import, Kryo round trip and configuration build. Only the fixture's
// loopback ports change for the HTTP probes; do not reconstruct its transport.
func TestGeneratedXHTTPConfigurations(t *testing.T) {
	dir := os.Getenv("SEKAIMOD_XHTTP_FIXTURES")
	if dir == "" {
		t.Skip("no Kotlin-generated XHTTP fixtures supplied")
	}
	for _, group := range []struct {
		file      string
		protocols []string
		split     bool
	}{
		{"profiles.json", []string{"vless", "vmess", "trojan", "ewp"}, false},
		{"downloads.json", []string{"vless", "ewp"}, true},
	} {
		data, err := os.ReadFile(filepath.Join(dir, group.file))
		if err != nil {
			t.Fatal(err)
		}
		var fixtures map[string]json.RawMessage
		if err = json.Unmarshal(data, &fixtures); err != nil {
			t.Fatal(err)
		}
		for _, protocol := range group.protocols {
			t.Run(group.file+"/"+protocol, func(t *testing.T) {
				raw := fixtures[protocol]
				if len(raw) == 0 {
					t.Fatal("missing generated outbound")
				}
				if err := CheckSingBoxConfig(`{"outbounds":[`+string(raw)+`]}`, nil); err != nil {
					t.Fatal(err)
				}
				checkGeneratedXHTTPWire(t, raw, group.split)
			})
		}
	}
}

func checkGeneratedXHTTPWire(t *testing.T, raw json.RawMessage, split bool) {
	t.Helper()
	const payload = "Kotlin share to native XHTTP wire"
	type upload struct{ id, body string }
	posted := make(chan upload, 1)
	get := func(w http.ResponseWriter, r *http.Request, session string) {
		select {
		case up := <-posted:
			if up.id == "" || up.id != session {
				t.Errorf("upload/download session mismatch")
			}
			_, _ = io.WriteString(w, up.body)
		case <-r.Context().Done():
		}
	}
	main := httptest.NewUnstartedServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.ProtoMajor != 2 || r.Host != "example.com" || r.URL.Query().Get("route") != "up" || !strings.HasPrefix(r.URL.Path, "/profile/") {
			t.Errorf("unexpected main route: %s %s %s", r.Proto, r.Host, r.RequestURI)
		}
		if r.UserAgent() != "grpc-node-js/1.14.3" || r.Header.Get("Authorization") != "Digest synthetic=A+B%2F&v=1" ||
			strings.Join(r.Header.Values("X-Multi"), ",") != "one,two" {
			t.Error("lost imported headers")
		}
		if size := hpack.HuffmanEncodeLength(r.Header.Get("Auth-Token")); size < 62 || size > 66 || r.Referer() != "" || r.Header.Get("_key") != "" {
			t.Error("incorrect tokenish padding")
		}
		session := strings.TrimPrefix(r.URL.Path, "/profile/")
		switch {
		case r.Method == http.MethodGet && !split:
			get(w, r, session)
		case r.Method == http.MethodPost:
			body := make([]byte, len(payload))
			if _, err := io.ReadFull(r.Body, body); err != nil {
				t.Errorf("read upload: %v", err)
				return
			}
			posted <- upload{session, string(body)}
		default:
			http.Error(w, "unexpected request", http.StatusBadRequest)
			t.Error("unexpected HTTP method on main endpoint")
		}
	}))
	main.EnableHTTP2 = true
	main.StartTLS()
	defer main.Close()
	var opts struct {
		option.ServerOptions
		TLS       option.OutboundTLSOptions    `json:"tls"`
		Transport option.V2RayTransportOptions `json:"transport"`
	}
	// Keep this fixture bridge compatible with the prior core's Go API too.
	if split {
		down := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			ref, err := url.Parse(r.Referer())
			if r.Method != http.MethodGet || r.ProtoMajor != 1 || r.TLS != nil || r.Host != "down.test" || r.URL.Path != "/down/" || r.URL.Query().Get("route") != "two" ||
				r.Header.Get("Authorization") != "" || r.Header.Get("Auth-Token") != "" || err != nil || len(ref.Query().Get("x_padding")) < 100 {
				t.Error("download did not apply its URL, cleartext, headers or padding overrides")
			}
			get(w, r, r.URL.Query().Get("download_sid"))
		}))
		defer down.Close()
		var tree map[string]any
		if err := json.Unmarshal(raw, &tree); err != nil {
			t.Fatal(err)
		}
		dl := tree["transport"].(map[string]any)["download"].(map[string]any)
		if dl["server"] != "127.0.0.1" {
			t.Fatal("expected a loopback download fixture")
		}
		dl["server_port"] = M.SocksaddrFromNet(down.Listener.Addr()).Port
		var err error
		raw, err = json.Marshal(tree)
		if err != nil {
			t.Fatal(err)
		}
	}
	if err := json.Unmarshal(raw, &opts); err != nil {
		t.Fatal(err)
	}
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	config, err := tls.NewClient(ctx, logger.NOP(), opts.Server, opts.TLS)
	if err != nil {
		t.Fatal(err)
	}
	client, err := v2ray.NewClientTransport(ctx, N.SystemDialer, M.SocksaddrFromNet(main.Listener.Addr()), opts.Transport, config)
	if err != nil {
		t.Fatal(err)
	}
	defer client.Close()
	conn, err := client.DialContext(ctx)
	if err != nil {
		t.Fatal(err)
	}
	defer conn.Close()
	if _, err = io.WriteString(conn, payload); err != nil {
		t.Fatal(err)
	}
	got := make([]byte, len(payload))
	if _, err = io.ReadFull(conn, got); err != nil || string(got) != payload {
		t.Fatalf("echo=%q error=%v", got, err)
	}
}
