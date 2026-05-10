package libcore

import (
	"context"
	"net/netip"

	"github.com/sagernet/sing-box/adapter"
	"github.com/sagernet/sing-box/experimental/libbox/platform"
	"github.com/sagernet/sing-box/option"
	tun "github.com/sagernet/sing-tun"
)

// boxPlatformInterfaceAdapter bridges the existing
// boxPlatformInterfaceWrapper (which implements the old NekoBox
// experimental/libbox/platform.Interface) to sing-box 1.13's new
// adapter.PlatformInterface, which is the type the route /
// NetworkManager / DNS layers now look up via service.FromContext.
//
// Without registering a service.PlatformInterface implementation under
// the new adapter.PlatformInterface key, NetworkManager falls back to
// the netlink path on Android — which is forbidden by SELinux on
// modern Android user-space, producing ErrNetlinkBanned at startup.
type boxPlatformInterfaceAdapter struct {
	*boxPlatformInterfaceWrapper
}

func newBoxPlatformInterfaceAdapter(w *boxPlatformInterfaceWrapper) *boxPlatformInterfaceAdapter {
	return &boxPlatformInterfaceAdapter{w}
}

// --- methods unique to adapter.PlatformInterface in 1.13 ---

// OpenInterface is the 1.13 name for what NekoBox's wrapper exposes
// as OpenTun. The signature is identical, so we just forward.
func (a *boxPlatformInterfaceAdapter) OpenInterface(options *tun.Options, platformOptions option.TunPlatformOptions) (tun.Tun, error) {
	return a.boxPlatformInterfaceWrapper.OpenTun(options, platformOptions)
}

func (a *boxPlatformInterfaceAdapter) UsePlatformInterface() bool {
	// We always have a Java-side TUN file descriptor when running on
	// Android, so the kernel should ask us to OpenInterface instead of
	// trying to open a TUN device directly.
	return true
}

func (a *boxPlatformInterfaceAdapter) UsePlatformNetworkInterfaces() bool {
	// Android cannot enumerate interfaces via raw syscalls without
	// netlink / NETLINK; defer to the Java side.
	return a.boxPlatformInterfaceWrapper.UsePlatformInterfaceGetter()
}

func (a *boxPlatformInterfaceAdapter) NetworkInterfaces() ([]adapter.NetworkInterface, error) {
	return a.boxPlatformInterfaceWrapper.Interfaces()
}

func (a *boxPlatformInterfaceAdapter) NetworkExtensionIncludeAllNetworks() bool {
	return a.boxPlatformInterfaceWrapper.IncludeAllNetworks()
}

func (a *boxPlatformInterfaceAdapter) RequestPermissionForWIFIState() error {
	// The Android side requests WiFi state permission lazily; nothing
	// to do at the kernel level.
	return nil
}

func (a *boxPlatformInterfaceAdapter) UsePlatformConnectionOwnerFinder() bool {
	// We have a Java-side bridge (FindConnectionOwner) that uses the
	// Android ConnectivityManager; preferable to the kernel's procfs
	// fallback because Android restricts /proc/net access.
	return true
}

func (a *boxPlatformInterfaceAdapter) FindConnectionOwner(req *adapter.FindConnectionOwnerRequest) (*adapter.ConnectionOwner, error) {
	src, err := netip.ParseAddrPort(req.SourceAddress + ":" + addrPortNumber(req.SourcePort))
	if err != nil {
		return nil, err
	}
	dst, err := netip.ParseAddrPort(req.DestinationAddress + ":" + addrPortNumber(req.DestinationPort))
	if err != nil {
		return nil, err
	}
	network := "tcp"
	if req.IpProtocol == 17 {
		network = "udp"
	}
	return a.boxPlatformInterfaceWrapper.FindProcessInfo(context.Background(), network, src, dst)
}

func (a *boxPlatformInterfaceAdapter) UsePlatformWIFIMonitor() bool {
	// We don't have a callback-based WiFi monitor on the Java side;
	// the kernel can poll ReadWIFIState if needed.
	return false
}

func (a *boxPlatformInterfaceAdapter) UsePlatformNotification() bool {
	// SekaiMod handles user-facing notifications on the Android side
	// (StatusBar / VPN service notification), not via this hook.
	return false
}

// SendNotification adapts the new adapter.Notification (1.13) shape
// to the old experimental/libbox/platform.Notification the Java side
// understands. We don't return an error from the wrapper because
// notification delivery is best-effort.
func (a *boxPlatformInterfaceAdapter) SendNotification(notification *adapter.Notification) error {
	return a.boxPlatformInterfaceWrapper.SendNotification(&platform.Notification{
		Identifier: notification.Identifier,
		TypeName:   notification.TypeName,
		TypeID:     notification.TypeID,
		Title:      notification.Title,
		Subtitle:   notification.Subtitle,
		Body:       notification.Body,
		OpenURL:    notification.OpenURL,
	})
}

func (a *boxPlatformInterfaceAdapter) MyInterfaceAddress() []netip.Addr {
	// Used by the kernel to bind outbounds to the local TUN's address
	// when auto-routing; the Android VPN service handles this
	// transparently for us, so an empty list is correct.
	return nil
}

// addrPortNumber turns an int32 port into a string suitable for
// netip.ParseAddrPort (we deliberately avoid strconv to keep the
// per-connection allocation tiny).
func addrPortNumber(p int32) string {
	if p == 0 {
		return "0"
	}
	var buf [6]byte
	n := len(buf)
	for p > 0 {
		n--
		buf[n] = byte('0' + p%10)
		p /= 10
	}
	return string(buf[n:])
}
