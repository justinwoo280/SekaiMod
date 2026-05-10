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
     * EWP/v2.1 server static identity public key (base64-encoded
     * 32-byte X25519 public key). When set, the client speaks
     * EWP/v2.1 and binds the handshake KDF to this server identity
     * (closes audit findings S1 / S2 / H2). Empty falls back to
     * legacy v2.0 — note that v2.1 servers REJECT v2.0 handshakes.
     */
    public String serverStaticPubKey;

    // --------------------------------------- v2ray transport
    // tcp / ws / http / grpc / httpupgrade / quic
    public String type;
    public String host;
    public String path;

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
        if (serverStaticPubKey == null) serverStaticPubKey = "";
        if (type == null || type.isEmpty()) type = "tcp";
        if (host == null) host = "";
        if (path == null) path = "";

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
        output.writeInt(1);
        super.serialize(output);

        output.writeString(uuid);
        output.writeString(serverStaticPubKey);

        output.writeString(type);
        output.writeString(host);
        output.writeString(path);

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
        // v1+ adds the EWP/v2.1 server static identity public key.
        // v0 profiles default to empty (= legacy v2.0 behavior).
        if (version >= 1) {
            serverStaticPubKey = input.readString();
        } else {
            serverStaticPubKey = "";
        }

        type = input.readString();
        host = input.readString();
        path = input.readString();

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
