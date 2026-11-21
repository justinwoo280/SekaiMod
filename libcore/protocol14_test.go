package libcore

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net"
	"os"
	"strconv"
	"testing"
	"time"

	box "github.com/sagernet/sing-box"
	boxCertificate "github.com/sagernet/sing-box/adapter/certificate"
	"github.com/sagernet/sing-box/adapter/endpoint"
	"github.com/sagernet/sing-box/adapter/inbound"
	boxService "github.com/sagernet/sing-box/adapter/service"
	"github.com/sagernet/sing-box/option"
	"github.com/sagernet/sing-box/protocol/snell"
	M "github.com/sagernet/sing/common/metadata"
)

func TestSnellAndroidRegistry(t *testing.T) {
	for _, version := range []int{4, 6} {
		config := fmt.Sprintf(`{"outbounds":[{"type":"snell","tag":"proxy","server":"127.0.0.1","server_port":24443,"version":%d,"psk":"fixture-snell-password","reuse":true}]}`, version)
		if err := CheckSingBoxConfig(config, nil); err != nil {
			t.Fatalf("Snell v%d: %v", version, err)
		}
	}
	for _, version := range []int{0, 1, 2, 3, 5, 7} {
		config := fmt.Sprintf(`{"outbounds":[{"type":"snell","server":"127.0.0.1","server_port":24443,"version":%d,"psk":"fixture-snell-password"}]}`, version)
		if err := CheckSingBoxConfig(config, nil); err == nil {
			t.Fatalf("accepted unsupported Snell client version %d", version)
		}
	}
}

// Set by the Android generator tests when doing cross-language validation.
func TestGeneratedProtocol14Configurations(t *testing.T) {
	path := os.Getenv("SEKAIMOD_PROTOCOL_FIXTURE")
	if path == "" {
		t.Skip("no Kotlin-generated fixture supplied")
	}
	data, err := os.ReadFile(path)
	if err != nil {
		t.Fatal(err)
	}
	var fixtures map[string]json.RawMessage
	if err := json.Unmarshal(data, &fixtures); err != nil {
		t.Fatal(err)
	}
	if len(fixtures) == 0 {
		t.Fatal("empty generated fixture")
	}
	for name, outbound := range fixtures {
		t.Run(name, func(t *testing.T) {
			config := `{"outbounds":[` + string(outbound) + `]}`
			if err := CheckSingBoxConfig(config, nil); err != nil {
				t.Fatal(err)
			}
		})
	}
}

func startProtocolTestBox(t *testing.T, config any) *box.Box {
	t.Helper()
	// Use exactly the outbound registry shipped by the Android embedding, with
	// native host networking for this loopback-only transport test.
	inboundRegistry := inbound.NewRegistry()
	snell.RegisterInbound(inboundRegistry)
	ctx := box.Context(context.Background(), inboundRegistry,
		nekoboxAndroidOutboundRegistry(), endpoint.NewRegistry(),
		nekoboxAndroidDNSTransportRegistry(nil), boxService.NewRegistry(), boxCertificate.NewRegistry())
	encoded, err := json.Marshal(config)
	if err != nil {
		t.Fatal(err)
	}
	var options option.Options
	if err := options.UnmarshalJSONContext(ctx, encoded); err != nil {
		t.Fatal(err)
	}
	instance, err := box.New(box.Options{Context: ctx, Options: options})
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = instance.Close() })
	if err := instance.Start(); err != nil {
		t.Fatal(err)
	}
	return instance
}

func TestSnellAndroidOutboundInterop(t *testing.T) {
	tcpEcho, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer tcpEcho.Close()
	go func() {
		for {
			conn, err := tcpEcho.Accept()
			if err != nil {
				return
			}
			go func() { defer conn.Close(); _, _ = io.Copy(conn, conn) }()
		}
	}()
	udpEcho, err := net.ListenPacket("udp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer udpEcho.Close()
	go func() {
		buffer := make([]byte, 65535)
		for {
			n, addr, err := udpEcho.ReadFrom(buffer)
			if err != nil {
				return
			}
			_, _ = udpEcho.WriteTo(buffer[:n], addr)
		}
	}()
	for _, tc := range []struct {
		version int
		mode    string
	}{
		{4, "none"}, {4, "http"}, {4, "tls"},
		{6, "default"}, {6, "unshaped"}, {6, "unsafe-raw"},
	} {
		for _, reuse := range []bool{false, true} {
			t.Run(fmt.Sprintf("v%d/%s/reuse=%v", tc.version, tc.mode, reuse), func(t *testing.T) {
				listener, err := net.Listen("tcp", "127.0.0.1:0")
				if err != nil {
					t.Fatal(err)
				}
				_, portText, _ := net.SplitHostPort(listener.Addr().String())
				port, _ := strconv.Atoi(portText)
				listener.Close()
				serverVersion := tc.version
				field := "mode"
				if tc.version == 4 {
					serverVersion = 5
					field = "obfs_mode"
				}
				server := map[string]any{
					"type": "snell", "tag": "snell-in", "listen": "127.0.0.1", "listen_port": port,
					"version": serverVersion, "psk": "fixture-snell-password", field: tc.mode,
					"users": []any{map[string]any{"name": "fixture", "userkey": "fixture-user"}},
				}
				startProtocolTestBox(t, map[string]any{
					"log": map[string]any{"disabled": true}, "inbounds": []any{server},
					"outbounds": []any{map[string]any{"type": "direct", "tag": "direct"}},
				})
				client := startProtocolTestBox(t, map[string]any{
					"log": map[string]any{"disabled": true},
					"outbounds": []any{map[string]any{
						"type": "snell", "tag": "proxy", "server": "127.0.0.1", "server_port": port,
						"version": tc.version, "psk": "fixture-snell-password", "userkey": "fixture-user", "reuse": reuse, field: tc.mode,
					}},
				})
				outbound, ok := client.Outbound().Outbound("proxy")
				if !ok {
					t.Fatal("Snell outbound missing")
				}
				ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
				defer cancel()
				for n := 0; n < 2; n++ {
					conn, err := outbound.DialContext(ctx, "tcp", M.ParseSocksaddr(tcpEcho.Addr().String()))
					if err != nil {
						t.Fatal(err)
					}
					_ = conn.SetDeadline(time.Now().Add(5 * time.Second))
					payload := bytes.Repeat([]byte("Snell TCP fixture"), 128)
					if _, err = conn.Write(payload); err != nil {
						conn.Close()
						t.Fatal(err)
					}
					actual := make([]byte, len(payload))
					_, err = io.ReadFull(conn, actual)
					conn.Close()
					if err != nil || !bytes.Equal(actual, payload) {
						t.Fatalf("TCP round trip: %v", err)
					}
				}
				packet, err := outbound.ListenPacket(ctx, M.ParseSocksaddr(udpEcho.LocalAddr().String()))
				if err != nil {
					t.Fatal(err)
				}
				defer packet.Close()
				_ = packet.SetDeadline(time.Now().Add(5 * time.Second))
				payload := []byte("Snell UDP fixture")
				if _, err = packet.WriteTo(payload, udpEcho.LocalAddr()); err != nil {
					t.Fatal(err)
				}
				actual := make([]byte, 2048)
				n, _, err := packet.ReadFrom(actual)
				if err != nil || !bytes.Equal(actual[:n], payload) {
					t.Fatalf("UDP round trip: %v", err)
				}
			})
		}
	}
}
