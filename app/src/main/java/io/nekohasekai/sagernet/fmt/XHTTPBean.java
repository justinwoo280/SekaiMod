package io.nekohasekai.sagernet.fmt;

/** Shared storage for V2Ray and EWP transports. The concrete Beans own their Kryo layouts. */
public abstract class XHTTPBean extends AbstractBean {
    public String type;
    public String host;
    public String path;

    public Boolean xhttpBrowser;
    public Boolean xhttpGRPCFraming;
    public String xhttpMode;
    public String xhttpPaddingBytes;
    public String xhttpXmuxMaxConcurrency;
    public String xhttpXmuxMaxConnections;
    public String xhttpXmuxCMaxReuseTimes;
    public String xhttpXmuxHMaxRequestTimes;
    public String xhttpXmuxHMaxReusableSecs;
    public Integer xhttpXmuxHKeepAlivePeriod;

    // Subscription/node data, not a persistent custom outbound override.
    public String xhttpExtra = "";

    @Override
    public void initializeDefaultValues() {
        super.initializeDefaultValues();
        if (xhttpBrowser == null) xhttpBrowser = false;
        if (xhttpGRPCFraming == null) xhttpGRPCFraming = false;
        if (xhttpMode == null || xhttpMode.trim().isEmpty()) xhttpMode = "auto";
        if (xhttpPaddingBytes == null) xhttpPaddingBytes = "";
        if (xhttpXmuxMaxConcurrency == null) xhttpXmuxMaxConcurrency = "";
        if (xhttpXmuxMaxConnections == null) xhttpXmuxMaxConnections = "";
        if (xhttpXmuxCMaxReuseTimes == null) xhttpXmuxCMaxReuseTimes = "";
        if (xhttpXmuxHMaxRequestTimes == null) xhttpXmuxHMaxRequestTimes = "";
        if (xhttpXmuxHMaxReusableSecs == null) xhttpXmuxHMaxReusableSecs = "";
        if (xhttpXmuxHKeepAlivePeriod == null) xhttpXmuxHKeepAlivePeriod = 0;
        if (xhttpExtra == null) xhttpExtra = "";
    }

    protected final boolean hasXHTTPExtra() {
        return "xhttp".equals(type) && xhttpExtra != null && !xhttpExtra.isEmpty();
    }
}
