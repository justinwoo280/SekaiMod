package moe.matsuri.nb4a.proxy.ewp;

import androidx.annotation.NonNull;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import org.jetbrains.annotations.NotNull;

import io.nekohasekai.sagernet.fmt.AbstractBean;
import io.nekohasekai.sagernet.fmt.KryoConverters;

/**
 * EWP (Encrypted Wire Protocol) outbound bean.
 *
 * Mirrors the shape of sing-box's EWPOutboundOptions:
 *   uuid + TLS (full set) + optional v2ray transport + optional mux.
 *
 * Field naming follows StandardV2RayBean conventions so that future code
 * sharing (e.g. ECH/uTLS sub-options) stays simple.
 */
public class EwpBean extends AbstractBean {

    public static final Creator<EwpBean> CREATOR = new CREATOR<EwpBean>() {
        @NonNull
        @Override
        public EwpBean newInstance() {
            return new EwpBean();
        }

        @Override
        public EwpBean[] newArray(int size) {
            return new EwpBean[size];
        }
    };

    // --------------------------------------- core
    public String uuid;
    /**
     * EWP/v2.3 server signing identity public key (base64-encoded
     * 32-byte Ed25519 public key). The client pins this identity and
     * verifies the server's signatures on short-term outer keys and
     * handshake transcripts. Generate the server's keypair with
     * `sing-box generate ewp-keypair`; the PublicKey goes here.
     *
     * Replaces v2.1's serverStaticPubKey (base64 X25519): v2.3 moved
     * the server identity from a static X25519 key to an Ed25519
     * signing identity with signed short-term outer keys (forward
     * secrecy over the rotation window). Old v2.1 profiles cannot be
     * migrated automatically — the key material is incompatible and
     * every v2.3 server has a fresh Ed25519 identity anyway.
     */
    public String serverPublicKey;
    /**
     * EWP/v2.3 server_id: names the listener in the handshake
     * transcript and in every signed short-term outer key. Must match
     * the server's "server_id" exactly or the handshake is rejected.
     */
    public String serverId;
    /**
     * EWP/v2.3 route_epoch: 0 unless the operator rotates route tags.
     * Must match the server's "route_epoch" when non-zero.
     */
    public Integer routeEpoch;

    // --------------------------------------- v2ray transport
    // tcp / ws / http / grpc / httpupgrade / quic
    public String type;
    public String host;
    public String path;

    // --------------------------------------- xhttp
    public Boolean xhttpBrowser;

    public String xhttpMode;
    public String xhttpPaddingBytes;
    public String xhttpXmuxMaxConcurrency;
    public String xhttpXmuxMaxConnections;
    public String xhttpXmuxCMaxReuseTimes;
    public String xhttpXmuxHMaxRequestTimes;
    public String xhttpXmuxHMaxReusableSecs;
    public Integer xhttpXmuxHKeepAlivePeriod;

    // --------------------------------------- TLS
    public String sni;
    public String alpn;
    public String certificates;
    public String utlsFingerprint;
    public Boolean allowInsecure;

    // --------------------------------------- ECH
    public Boolean enableECH;
    public String echConfig;
    /** Decouples inner SNI from public ECH key fetch domain. */
    public String echQueryServerName;

    // --------------------------------------- Reality (kept for completeness)
    public String realityPubKey;
    public String realityShortId;

    // --------------------------------------- TLS fragmentation (anti-censor)
    public Boolean tlsFragment;
    public Boolean tlsRecordFragment;

    // --------------------------------------- Mux
    public Boolean enableMux;
    public Boolean muxPadding;
    public Integer muxType;
    public Integer muxConcurrency;

    // --------------------------------------- packet encoding
    public Integer packetEncoding; // 0:none 1:packet 2:xudp

    @Override
    public void initializeDefaultValues() {
        super.initializeDefaultValues();
        if (uuid == null) uuid = "";
        if (serverPublicKey == null) serverPublicKey = "";
        if (serverId == null) serverId = "";
        if (routeEpoch == null) routeEpoch = 0;
        if (type == null || type.isEmpty()) type = "tcp";
        if (host == null) host = "";
        if (path == null) path = "";

        if (xhttpBrowser == null) xhttpBrowser = false;
        if (xhttpMode == null || xhttpMode.isEmpty()) xhttpMode = "auto";
        if (xhttpPaddingBytes == null) xhttpPaddingBytes = "";
        if (xhttpXmuxMaxConcurrency == null) xhttpXmuxMaxConcurrency = "";
        if (xhttpXmuxMaxConnections == null) xhttpXmuxMaxConnections = "";
        if (xhttpXmuxCMaxReuseTimes == null) xhttpXmuxCMaxReuseTimes = "";
        if (xhttpXmuxHMaxRequestTimes == null) xhttpXmuxHMaxRequestTimes = "";
        if (xhttpXmuxHMaxReusableSecs == null) xhttpXmuxHMaxReusableSecs = "";
        if (xhttpXmuxHKeepAlivePeriod == null) xhttpXmuxHKeepAlivePeriod = 0;

        if (sni == null) sni = "";
        if (alpn == null) alpn = "";
        if (certificates == null) certificates = "";
        if (utlsFingerprint == null) utlsFingerprint = "";
        if (allowInsecure == null) allowInsecure = false;

        if (enableECH == null) enableECH = false;
        if (echConfig == null) echConfig = "";
        if (echQueryServerName == null) echQueryServerName = "";

        if (realityPubKey == null) realityPubKey = "";
        if (realityShortId == null) realityShortId = "";

        if (tlsFragment == null) tlsFragment = false;
        if (tlsRecordFragment == null) tlsRecordFragment = false;

        if (enableMux == null) enableMux = false;
        if (muxPadding == null) muxPadding = false;
        if (muxType == null) muxType = 0;
        if (muxConcurrency == null) muxConcurrency = 8;

        if (packetEncoding == null) packetEncoding = 0;
    }

    @Override
    public void serialize(ByteBufferOutput output) {
        // Schema version - bump on field addition
        // v0: initial layout (EWP/v2.0)
        // v1: + serverStaticPubKey (EWP/v2.1 opt-in)
        // v2: + xhttp transport fields
        // v3: EWP/v2.3 — replaced serverStaticPubKey with
        //     serverPublicKey (Ed25519) + serverId + routeEpoch
        // v4: + Browser XHTTP transport switch
        output.writeInt(4);
        super.serialize(output);

        output.writeString(uuid);
        output.writeString(serverPublicKey);
        output.writeString(serverId);
        output.writeInt(routeEpoch);

        output.writeString(type);
        output.writeString(host);
        output.writeString(path);

        output.writeBoolean(Boolean.TRUE.equals(xhttpBrowser));
        output.writeString(xhttpMode);
        output.writeString(xhttpPaddingBytes);
        output.writeString(xhttpXmuxMaxConcurrency);
        output.writeString(xhttpXmuxMaxConnections);
        output.writeString(xhttpXmuxCMaxReuseTimes);
        output.writeString(xhttpXmuxHMaxRequestTimes);
        output.writeString(xhttpXmuxHMaxReusableSecs);
        output.writeInt(xhttpXmuxHKeepAlivePeriod);

        output.writeString(sni);
        output.writeString(alpn);
        output.writeString(certificates);
        output.writeString(utlsFingerprint);
        output.writeBoolean(allowInsecure);

        output.writeBoolean(enableECH);
        output.writeString(echConfig);
        output.writeString(echQueryServerName);

        output.writeString(realityPubKey);
        output.writeString(realityShortId);

        output.writeBoolean(tlsFragment);
        output.writeBoolean(tlsRecordFragment);

        output.writeBoolean(enableMux);
        output.writeBoolean(muxPadding);
        output.writeInt(muxType);
        output.writeInt(muxConcurrency);

        output.writeInt(packetEncoding);
    }

    @Override
    public void deserialize(ByteBufferInput input) {
        int version = input.readInt();
        super.deserialize(input);

        uuid = input.readString();
        if (version >= 3) {
            serverPublicKey = input.readString();
            serverId = input.readString();
            routeEpoch = input.readInt();
        } else {
            // v1/v2 stored serverStaticPubKey (base64 X25519) here.
            // Read and DISCARD it: v2.3 server identities are Ed25519
            // and cannot be derived from the old key. The user must
            // re-enter the server's Ed25519 public key and server_id.
            if (version >= 1) {
                input.readString();
            }
            serverPublicKey = "";
            serverId = "";
            routeEpoch = 0;
        }

        type = input.readString();
        host = input.readString();
        path = input.readString();

        if (version >= 2) {
            if (version >= 4) {
                xhttpBrowser = input.readBoolean();
            } else {
                xhttpBrowser = false;
            }
            xhttpMode = input.readString();
            xhttpPaddingBytes = input.readString();
            xhttpXmuxMaxConcurrency = input.readString();
            xhttpXmuxMaxConnections = input.readString();
            xhttpXmuxCMaxReuseTimes = input.readString();
            xhttpXmuxHMaxRequestTimes = input.readString();
            xhttpXmuxHMaxReusableSecs = input.readString();
            xhttpXmuxHKeepAlivePeriod = input.readInt();
        } else {
            xhttpMode = "auto";
            xhttpPaddingBytes = "";
            xhttpXmuxMaxConcurrency = "";
            xhttpXmuxMaxConnections = "";
            xhttpXmuxCMaxReuseTimes = "";
            xhttpXmuxHMaxRequestTimes = "";
            xhttpXmuxHMaxReusableSecs = "";
            xhttpXmuxHKeepAlivePeriod = 0;
        }

        sni = input.readString();
        alpn = input.readString();
        certificates = input.readString();
        utlsFingerprint = input.readString();
        allowInsecure = input.readBoolean();

        enableECH = input.readBoolean();
        echConfig = input.readString();
        echQueryServerName = input.readString();

        realityPubKey = input.readString();
        realityShortId = input.readString();

        tlsFragment = input.readBoolean();
        tlsRecordFragment = input.readBoolean();

        enableMux = input.readBoolean();
        muxPadding = input.readBoolean();
        muxType = input.readInt();
        muxConcurrency = input.readInt();

        packetEncoding = input.readInt();
    }

    @NotNull
    @Override
    public EwpBean clone() {
        return KryoConverters.deserialize(new EwpBean(), KryoConverters.serialize(this));
    }
}
