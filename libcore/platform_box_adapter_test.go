package libcore

import (
	"testing"

	"github.com/sagernet/sing-box/adapter"
)

type ownerLookupPlatform struct {
	BoxPlatformInterface
	request adapter.FindConnectionOwnerRequest
}

func (p *ownerLookupPlatform) FindConnectionOwner(protocol int32, source string, sourcePort int32, destination string, destinationPort int32) (int32, error) {
	p.request = adapter.FindConnectionOwnerRequest{
		IpProtocol: protocol, SourceAddress: source, SourcePort: sourcePort,
		DestinationAddress: destination, DestinationPort: destinationPort,
	}
	return 10001, nil
}

func (p *ownerLookupPlatform) PackageNameByUid(uid int32) (string, error) {
	return "example.app", nil
}

func TestPlatformConnectionOwnerAddresses(t *testing.T) {
	previousPlatform, previousProcfs := intfBox, useProcfs
	t.Cleanup(func() { intfBox, useProcfs = previousPlatform, previousProcfs })
	platform := &ownerLookupPlatform{}
	intfBox, useProcfs = platform, false
	bridge := newBoxPlatformInterfaceAdapter(&boxPlatformInterfaceWrapper{})
	for _, request := range []adapter.FindConnectionOwnerRequest{
		{IpProtocol: 6, SourceAddress: "172.19.0.1", SourcePort: 40000, DestinationAddress: "203.0.113.1", DestinationPort: 443},
		{IpProtocol: 17, SourceAddress: "fd00::1", SourcePort: 40000, DestinationAddress: "2001:db8::1", DestinationPort: 53},
	} {
		owner, err := bridge.FindConnectionOwner(&request)
		if err != nil {
			t.Fatal(err)
		}
		if platform.request != request || owner.UserId != 10001 || len(owner.AndroidPackageNames) != 1 || owner.AndroidPackageNames[0] != "example.app" {
			t.Fatalf("connection-owner metadata changed: request=%+v owner=%+v", platform.request, owner)
		}
	}
	if _, err := bridge.FindConnectionOwner(&adapter.FindConnectionOwnerRequest{
		IpProtocol: 6, SourceAddress: "fd00::1", SourcePort: 65536,
		DestinationAddress: "2001:db8::1", DestinationPort: 443,
	}); err == nil {
		t.Fatal("out-of-range source port was accepted")
	}
}
