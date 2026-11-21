package moe.matsuri.nb4a.proxy.anytls;

import androidx.annotation.NonNull;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import org.jetbrains.annotations.NotNull;

import io.nekohasekai.sagernet.fmt.AbstractBean;
import io.nekohasekai.sagernet.fmt.KryoConverters;

public class AnyTLSBean extends AbstractBean {

    public static final Creator<AnyTLSBean> CREATOR = new CREATOR<AnyTLSBean>() {
        @NonNull
        @Override
        public AnyTLSBean newInstance() {
            return new AnyTLSBean();
        }

        @Override
        public AnyTLSBean[] newArray(int size) {
            return new AnyTLSBean[size];
        }
    };
    public String password;
    public String sni;
    public String alpn;
    public String certificates;
    public String utlsFingerprint;
    public Boolean allowInsecure;
    // In sing-box, this seemed can be used with REALITY.
    // But even mihomo appended many options, it still not provide REALITY.
    // https://github.com/anytls/anytls-go/blob/4636d90462fa21a510420512d7706a9acf69c7b9/docs/faq.md?plain=1#L25-L37

    public String echConfig;
    public Boolean enableECH;
    public String echQueryServerName;
    public String idleSessionCheckInterval;
    public String idleSessionTimeout;
    public Integer minIdleSession;
    public String clientMetadata;

    @Override
    public void initializeDefaultValues() {
        super.initializeDefaultValues();
        if (password == null) password = "";
        if (sni == null) sni = "";
        if (alpn == null) alpn = "";
        if (certificates == null) certificates = "";
        if (utlsFingerprint == null) utlsFingerprint = "";
        if (allowInsecure == null) allowInsecure = false;
        if (echConfig == null) echConfig = "";
        if (enableECH == null) enableECH = !echConfig.isEmpty();
        if (echQueryServerName == null) echQueryServerName = "";
        if (idleSessionCheckInterval == null) idleSessionCheckInterval = "";
        if (idleSessionTimeout == null) idleSessionTimeout = "";
        if (minIdleSession == null) minIdleSession = 0;
        if (clientMetadata == null) clientMetadata = "";
    }

    @Override
    public void serialize(ByteBufferOutput output) {
        output.writeInt(2);
        super.serialize(output);
        output.writeString(password);
        output.writeString(sni);
        output.writeString(alpn);
        output.writeString(certificates);
        output.writeString(utlsFingerprint);
        output.writeBoolean(allowInsecure);
        output.writeString(echConfig);
        output.writeBoolean(enableECH);
        output.writeString(echQueryServerName);
        output.writeString(idleSessionCheckInterval);
        output.writeString(idleSessionTimeout);
        output.writeInt(minIdleSession);
        output.writeString(clientMetadata);
    }

    @Override
    public void deserialize(ByteBufferInput input) {
        int version = input.readInt();
        super.deserialize(input);
        password = input.readString();
        sni = input.readString();
        alpn = input.readString();
        certificates = input.readString();
        utlsFingerprint = input.readString();
        allowInsecure = input.readBoolean();
        echConfig = input.readString();
        if (version >= 1) {
            enableECH = input.readBoolean();
            echQueryServerName = input.readString();
        }
        if (version >= 2) {
            idleSessionCheckInterval = input.readString();
            idleSessionTimeout = input.readString();
            minIdleSession = input.readInt();
            clientMetadata = input.readString();
        }
    }

    @NotNull
    @Override
    public AnyTLSBean clone() {
        return KryoConverters.deserialize(new AnyTLSBean(), KryoConverters.serialize(this));
    }
}
