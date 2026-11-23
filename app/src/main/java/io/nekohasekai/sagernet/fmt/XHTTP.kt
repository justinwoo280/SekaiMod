package io.nekohasekai.sagernet.fmt

import com.google.gson.JsonObject
import moe.matsuri.nb4a.SingBoxOptions.V2RayTransportOptions_XHTTPOptions
import moe.matsuri.nb4a.utils.JavaUtil.gson
import okhttp3.HttpUrl

fun resolveXHTTPMode(mode: String?, browser: Boolean, grpcFraming: Boolean): String {
    if (grpcFraming) return mode?.takeIf { it == "stream-up" || it == "stream-one" } ?: "stream-one"
    if (browser) return mode?.takeIf { it == "packet-up" || it == "stream-up" || it == "stream-one" } ?: "packet-up"
    return mode?.takeIf { it.isNotBlank() } ?: "auto"
}

internal fun XHTTPBean.readXHTTPQuery(url: HttpUrl) {
    importXHTTPExtra(XHTTPExtra.fromQuery(url))
    url.queryParameter("host")?.let { host = it }
    url.queryParameter("path")?.let { path = it }
    url.queryParameter("mode")?.let { xhttpMode = it }
    url.queryParameter("browser")?.let { xhttpBrowser = it == "1" || it == "true" }
    url.queryParameter("grpc_framing")?.let { xhttpGRPCFraming = it == "1" || it == "true" }
}

internal fun XHTTPBean.importXHTTPExtra(raw: JsonObject) {
    val extra = XHTTPExtra.native(raw)
    extra.remove("host")?.let { host = it.asString }
    extra.remove("path")?.let { path = it.asString }
    extra.remove("mode")?.let { xhttpMode = it.asString }
    extra.remove("browser")?.let { xhttpBrowser = it.asBoolean }
    extra.remove("grpc_framing")?.let { xhttpGRPCFraming = it.asBoolean }

    // These preferences already have owners. Do not keep a second copy which
    // could silently restore a value after it has been cleared in the UI.
    xhttpPaddingBytes = extra.remove("x_padding_bytes")?.asString.orEmpty()
    val pool = extra.remove("xmux")?.asJsonObject
    xhttpXmuxMaxConcurrency = pool?.get("max_concurrency")?.asString.orEmpty()
    xhttpXmuxMaxConnections = pool?.get("max_connections")?.asString.orEmpty()
    xhttpXmuxCMaxReuseTimes = pool?.get("c_max_reuse_times")?.asString.orEmpty()
    xhttpXmuxHMaxRequestTimes = pool?.get("h_max_request_times")?.asString.orEmpty()
    xhttpXmuxHMaxReusableSecs = pool?.get("h_max_reusable_secs")?.asString.orEmpty()
    xhttpXmuxHKeepAlivePeriod = pool?.get("h_keep_alive_period")?.asInt ?: 0
    if (pool != null && (pool.size() == 0 || pool.has("h_keep_alive_period") && xhttpXmuxHKeepAlivePeriod == 0)) {
        // The integer preference uses zero for unset. Preserve explicit object/zero presence.
        extra.add("xmux", JsonObject().apply { pool["h_keep_alive_period"]?.let { add("h_keep_alive_period", it) } })
    }
    raw["downloadSettings"]?.takeUnless { it.isJsonNull }?.let {
        // Keep one lossless share representation. It is converted to a sing-box
        // download only when generating config, never passed to the core verbatim.
        extra.remove("download")
        extra.add("downloadSettings", it)
    }
    xhttpExtra = if (extra.size() == 0) "" else XHTTPExtra.sorted(extra).toString()
}

private fun XHTTPBean.xhttpFields(forCore: Boolean): JsonObject {
    val stored = xhttpExtra?.takeIf { it.isNotBlank() }?.let { XHTTPExtra.parse(it) } ?: JsonObject()
    val extra = if (forCore) XHTTPExtra.native(stored) else stored
    xhttpPaddingBytes?.takeIf { it.isNotBlank() }?.let { extra.addProperty("x_padding_bytes", it) }
    val pool = extra.getAsJsonObject("xmux") ?: JsonObject()
    fun set(name: String, value: String?) {
        if (!value.isNullOrBlank()) pool.addProperty(name, value)
    }
    set("max_concurrency", xhttpXmuxMaxConcurrency)
    set("max_connections", xhttpXmuxMaxConnections)
    set("c_max_reuse_times", xhttpXmuxCMaxReuseTimes)
    set("h_max_request_times", xhttpXmuxHMaxRequestTimes)
    set("h_max_reusable_secs", xhttpXmuxHMaxReusableSecs)
    xhttpXmuxHKeepAlivePeriod?.takeIf { it != 0 }?.let { pool.addProperty("h_keep_alive_period", it) }
    if (pool.size() != 0) extra.add("xmux", pool)
    return extra
}

internal fun XHTTPBean.shareXHTTPExtra(): JsonObject? = xhttpFields(false).takeIf { it.size() != 0 }?.let(XHTTPExtra::forLink)

internal fun XHTTPBean.buildXHTTPTransport(browserEnabled: Boolean): V2RayTransportOptions_XHTTPOptions {
    val fields = xhttpFields(true)
    if (browserEnabled) fields.keySet().retainAll(XHTTPExtra.browserFields)
    val options = if (fields.size() == 0) V2RayTransportOptions_XHTTPOptions() else
        gson.fromJson(fields, V2RayTransportOptions_XHTTPOptions::class.java)
    options.type = "xhttp"
    options.browser = browserEnabled
    options.mode = resolveXHTTPMode(xhttpMode, browserEnabled, xhttpGRPCFraming == true)
    if (xhttpGRPCFraming == true) options.grpc_framing = true
    if (!host.isNullOrBlank()) options.host = host
    if (!path.isNullOrBlank()) options.path = path
    return options
}
