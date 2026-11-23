package io.nekohasekai.sagernet.fmt

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import okhttp3.HttpUrl
import java.io.StringReader

/** Xray share-link spelling <-> sing-box transport JSON. Never merge extra into an outbound. */
internal object XHTTPExtra {
    private val fields = mapOf(
        "headers" to "headers", "x_padding_bytes" to "xPaddingBytes",
        "no_grpc_header" to "noGRPCHeader", "no_sse_header" to "noSSEHeader",
        "sc_max_each_post_bytes" to "scMaxEachPostBytes", "sc_min_posts_interval_ms" to "scMinPostsIntervalMs",
        "sc_max_buffered_posts" to "scMaxBufferedPosts", "sc_stream_up_server_secs" to "scStreamUpServerSecs",
        "server_max_header_bytes" to "serverMaxHeaderBytes", "uplink_http_method" to "uplinkHTTPMethod",
        "x_padding_obfs_mode" to "xPaddingObfsMode", "x_padding_key" to "xPaddingKey",
        "x_padding_header" to "xPaddingHeader", "x_padding_placement" to "xPaddingPlacement",
        "x_padding_method" to "xPaddingMethod", "session_placement" to "sessionIDPlacement",
        "session_key" to "sessionIDKey", "seq_placement" to "seqPlacement", "seq_key" to "seqKey",
        "uplink_data_placement" to "uplinkDataPlacement", "uplink_data_key" to "uplinkDataKey",
        "uplink_chunk_size" to "uplinkChunkSize", "session_id_table" to "sessionIDTable",
        "session_id_length" to "sessionIDLength", "xmux" to "xmux",
    )
    private val xmux = mapOf(
        "max_concurrency" to "maxConcurrency", "max_connections" to "maxConnections",
        "c_max_reuse_times" to "cMaxReuseTimes", "h_max_request_times" to "hMaxRequestTimes",
        "h_max_reusable_secs" to "hMaxReusableSecs", "h_keep_alive_period" to "hKeepAlivePeriod",
    )
    private val names = fields.entries.associate { (native, link) -> link to native } + ("method" to "uplink_http_method")
    private val xmuxNames = xmux.entries.associate { (native, link) -> link to native }
    private val ranges = setOf("x_padding_bytes", "sc_max_each_post_bytes", "sc_min_posts_interval_ms", "sc_stream_up_server_secs", "session_id_length", "uplink_chunk_size")
    private val booleans = setOf("x_padding_obfs_mode", "no_grpc_header", "no_sse_header", "browser", "grpc_framing")
    private val integers = setOf("sc_max_buffered_posts", "server_max_header_bytes")
    private val base = setOf("host", "path", "mode", "browser", "grpc_framing")
    private val downFields = setOf("host", "path", "headers", "xmux", "session_placement", "session_key", "domain_strategy") +
            fields.keys.filter { it.startsWith("x_padding_") }
    val browserFields = setOf("headers", "x_padding_bytes", "no_grpc_header", "no_sse_header", "sc_max_each_post_bytes", "sc_min_posts_interval_ms", "sc_max_buffered_posts")
    val ordinaryOnlyFields = fields.keys - browserFields + "download"

    private val jsonAdapter = Gson().getAdapter(JsonElement::class.java)
    class Invalid(path: String, reason: String) : IllegalArgumentException("XHTTP $path: $reason")
    private fun invalid(path: String, reason: String): Nothing = throw Invalid(path, reason)

    fun parse(raw: String, path: String = "extra"): JsonObject = try {
        JsonReader(StringReader(raw)).use { reader ->
            val obj = jsonAdapter.read(reader).obj(path)
            if (reader.peek() != JsonToken.END_DOCUMENT) invalid(path, "expected one JSON object")
            obj
        }
    } catch (_: Exception) {
        invalid(path, "expected a JSON object")
    }

    private fun JsonElement.obj(path: String) = if (isJsonObject) asJsonObject else invalid(path, "expected an object")
    private fun JsonElement.text(path: String) = if (isJsonPrimitive && asJsonPrimitive.isString) asString else invalid(path, "expected a string")
    private fun int(value: JsonElement, path: String): Int =
        if (value.isJsonPrimitive) value.asString.toIntOrNull() ?: invalid(path, "expected a 32-bit integer") else invalid(path, "expected an integer")

    private fun range(value: JsonElement, path: String): JsonPrimitive {
        val (lo, hi) = if (value.isJsonObject) {
            val obj = value.asJsonObject
            if (obj.keySet().any { it != "from" && it != "to" }) invalid(path, "expected from/to")
            (obj["from"]?.let { int(it, path) } ?: 0) to (obj["to"]?.let { int(it, path) } ?: 0)
        } else {
            if (!value.isJsonPrimitive) invalid(path, "expected a range")
            val parts = value.asString.split('-', limit = 2)
            val first = parts.first().trim().toIntOrNull() ?: invalid(path, "invalid range start")
            first to (parts.last().trim().toIntOrNull() ?: invalid(path, "invalid range end"))
        }
        if (lo < 0 || hi < lo) invalid(path, "expected 0 <= from <= to")
        return JsonPrimitive(if (lo == hi) lo.toString() else "$lo-$hi")
    }

    private fun JsonObject.putUnique(name: String, value: JsonElement, path: String) {
        if (has(name) && get(name) != value) invalid("$path.$name", "conflicting values")
        add(name, value)
    }

    // Stable Bean bytes for subscription equality; header values and array order are untouched.
    fun sorted(value: JsonElement): JsonElement = when {
        value.isJsonObject -> JsonObject().apply { value.asJsonObject.keySet().sorted().forEach { add(it, sorted(value.asJsonObject[it])) } }
        value.isJsonArray -> JsonArray().apply { value.asJsonArray.forEach { add(sorted(it)) } }
        else -> value
    }

    private fun normalize(raw: JsonObject, path: String, download: Boolean): JsonObject = JsonObject().apply {
        for ((key, value) in raw.entrySet()) {
            if (value.isJsonNull) continue
            val name = names[key] ?: key
            val at = "$path.$key"
            val normalized: JsonElement = when {
                !download && key == "downloadSettings" -> xrayDownload(value.obj(at), at)
                !download && key == "download" -> normalize(value.obj(at), at, true)
                download && name == "server" -> JsonPrimitive(value.text(at))
                download && name == "server_port" -> JsonPrimitive(int(value, at).also { if (it !in 1..65535) invalid(at, "invalid port") })
                download && name == "detour" -> JsonPrimitive(value.text(at))
                download && name == "tls" -> value.obj(at)
                download && name == "domain_resolver" -> if (value.isJsonObject) value else JsonPrimitive(value.text(at))
                download && name !in downFields -> invalid(at, "not a download GET option")
                name == "xmux" -> JsonObject().apply {
                    for ((field, v) in value.obj(at).entrySet()) {
                        if (v.isJsonNull) continue
                        val n = xmuxNames[field] ?: field
                        if (n !in xmux) invalid("$at.$field", "unknown XMUX option")
                        putUnique(n, if (n == "h_keep_alive_period") JsonPrimitive(int(v, "$at.$field")) else range(v, "$at.$field"), at)
                    }
                }
                name == "headers" -> value.obj(at).also { headers ->
                    val names = HashSet<String>()
                    for ((header, v) in headers.entrySet()) {
                        if (!names.add(header.lowercase())) invalid(at, "duplicate header name")
                        fun check(item: JsonElement) {
                            if (item.text("$at.$header").any { it == '\r' || it == '\n' }) invalid("$at.$header", "invalid header value")
                        }
                        if (v.isJsonArray) v.asJsonArray.forEach(::check) else check(v)
                    }
                }
                name in ranges -> range(value, at)
                name in integers -> JsonPrimitive(int(value, at))
                name in booleans -> if (value.isJsonPrimitive && value.asJsonPrimitive.isBoolean) value else invalid(at, "expected a boolean")
                name == "x_padding_placement" && value.text(at) == "queryInHeader" -> JsonPrimitive("query-in-header")
                name in fields || name in base || download && name == "domain_strategy" -> JsonPrimitive(value.text(at))
                else -> invalid(at, "unknown XHTTP option")
            }
            putUnique(if (key == "downloadSettings") "download" else name, normalized, path)
        }
    }

    fun native(raw: JsonObject) = normalize(raw, "extra", false)

    private fun xrayDownload(raw: JsonObject, path: String): JsonObject {
        val allowed = setOf("address", "port", "network", "security", "tlsSettings", "realitySettings", "xhttpSettings", "splithttpSettings", "sockopt")
        raw.keySet().firstOrNull { it !in allowed }?.let { invalid("$path.$it", "unsupported download setting") }
        raw["network"]?.let { if (it.text(path) !in setOf("xhttp", "splithttp")) invalid("$path.network", "expected xhttp") }
        if (raw.has("xhttpSettings") && raw.has("splithttpSettings") && raw["xhttpSettings"] != raw["splithttpSettings"]) {
            invalid("$path.xhttpSettings", "conflicting download settings")
        }
        val settings = (raw["xhttpSettings"] ?: raw["splithttpSettings"])?.obj("$path.xhttpSettings") ?: JsonObject()
        // Xray's nested extra replaces the advanced settings; host/path/mode belong to the outer object.
        val wire = (settings["extra"]?.obj("$path.xhttpSettings.extra") ?: settings).deepCopy().apply {
            remove("extra"); remove("mode"); remove("host"); remove("path")
            for (key in listOf("host", "path")) settings[key]?.let { add(key, it) }
        }
        raw["address"]?.let { wire.add("server", it) }
        raw["port"]?.let { wire.add("server_port", it) }
        raw["sockopt"]?.obj("$path.sockopt")?.let { socket ->
            if (socket.keySet().any { it != "dialerProxy" }) invalid("$path.sockopt", "unsupported socket option")
            socket["dialerProxy"]?.let { wire.add("detour", it) }
        }
        val securityType = raw["security"]?.text("$path.security")
        for (type in listOf("tls", "reality")) {
            if (securityType != type && raw[type + "Settings"]?.takeUnless { it.isJsonNull }?.obj("$path.${type}Settings")?.size()?.let { it > 0 } == true) {
                invalid("$path.${type}Settings", "requires security=$type")
            }
        }
        securityType?.let { security ->
            val tls = JsonObject()
            when (security) {
                "", "none" -> tls.addProperty("enabled", false)
                "tls", "reality" -> {
                    tls.addProperty("enabled", true)
                    val reality = JsonObject()
                    val config = raw[security + "Settings"]?.obj("$path.${security}Settings") ?: JsonObject()
                    for ((key, value) in config.entrySet()) {
                        when (key) {
                            "serverName" -> tls.addProperty("server_name", value.text(key))
                            "alpn" -> tls.add("alpn", value)
                            "allowInsecure" -> tls.add("insecure", value)
                            "minVersion" -> tls.add("min_version", value)
                            "maxVersion" -> tls.add("max_version", value)
                            "fingerprint" -> if (value.text(key).isNotBlank()) tls.add("utls", JsonObject().apply {
                                addProperty("enabled", true); add("fingerprint", value)
                            })
                            "publicKey", "password", "shortId" -> {
                                if (security != "reality") invalid("$path.tlsSettings.$key", "requires REALITY")
                                reality.putUnique(if (key == "shortId") "short_id" else "public_key", JsonPrimitive(value.text(key)), path)
                            }
                            else -> invalid("$path.${security}Settings.$key", "unsupported TLS option")
                        }
                    }
                    if (security == "reality") {
                        reality.addProperty("enabled", true)
                        tls.add("reality", reality)
                        if (!tls.has("utls")) tls.add("utls", JsonObject().apply {
                            addProperty("enabled", true); addProperty("fingerprint", "chrome")
                        })
                    }
                }
                else -> invalid("$path.security", "unsupported security type")
            }
            wire.add("tls", tls)
        }
        return normalize(wire, path, true)
    }

    // Standard extra is one URL-encoded JSON object. OkHttp already decoded it.
    // Also accept the corresponding individual query keys used by some exporters.
    fun fromQuery(url: HttpUrl): JsonObject {
        val result = url.queryParameter("extra")?.takeIf { it.isNotBlank() }?.let { parse(it) } ?: JsonObject()
        for (key in url.queryParameterNames) {
            val name = names[key] ?: key
            if (name !in fields && name !in setOf("download", "downloadSettings") && !key.startsWith("xmux.")) continue
            val raw = url.queryParameter(key) ?: continue
            val value: JsonElement = when {
                name in setOf("headers", "xmux", "download", "downloadSettings") || raw.trimStart().startsWith('{') && (name in ranges || key.startsWith("xmux.")) -> parse(raw, key)
                name in booleans -> JsonPrimitive(when (raw) {
                    "true", "1" -> true
                    "false", "0" -> false
                    else -> invalid(key, "expected a boolean")
                })
                else -> JsonPrimitive(raw)
            }
            if (key.startsWith("xmux.")) {
                val pool = result["xmux"]?.obj("xmux") ?: JsonObject().also { result.add("xmux", it) }
                pool.putUnique(key.removePrefix("xmux."), value, "xmux")
            } else result.putUnique(key, value, "extra")
        }
        return result
    }

    fun forLink(extra: JsonObject): JsonObject = JsonObject().apply {
        for ((key, value) in extra.entrySet()) {
            val linkValue = when (key) {
                "xmux" -> JsonObject().apply { for ((k, v) in value.asJsonObject.entrySet()) add(xmux.getValue(k), v) }
                "x_padding_placement" -> if (value.asString == "query-in-header") JsonPrimitive("queryInHeader") else value
                else -> value
            }
            add(fields[key] ?: key, linkValue)
        }
    }
}
