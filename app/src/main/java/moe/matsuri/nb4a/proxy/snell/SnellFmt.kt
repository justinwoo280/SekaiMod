package moe.matsuri.nb4a.proxy.snell

import com.google.gson.JsonObject
import io.nekohasekai.sagernet.ktx.blankAsNull
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.JavaUtil.gson

fun validateSnell(bean: SnellBean) {
    require(bean.version == 4 || bean.version == 6) { "Snell supports client versions 4 and 6" }
    require(bean.serverAddress.isNotBlank() && bean.serverPort in 1..65535) { "Snell requires a server and a valid port" }
    require(bean.psk.isNotEmpty()) { "Snell PSK is required" }
    if (bean.version == 6) {
        require(bean.psk.toByteArray(Charsets.UTF_8).size in 12..255) { "Snell v6 PSK must contain 12–255 UTF-8 bytes" }
        require(bean.mode in listOf("", "default", "unshaped", "unsafe-raw")) { "Unknown Snell v6 mode" }
    } else {
        require(bean.obfsMode in listOf("", "none", "http", "tls")) { "Unknown Snell v4 obfuscation mode" }
    }
    require(bean.userKey.toByteArray(Charsets.UTF_8).size <= 255) { "Snell user key is too long" }
    require(bean.network in listOf("", "tcp", "udp")) { "Snell network must be TCP, UDP or both" }
}

fun buildSingBoxOutboundSnellBean(bean: SnellBean): SingBoxOptions.Outbound_SnellOptions {
    validateSnell(bean)
    return SingBoxOptions.Outbound_SnellOptions().apply {
        type = "snell"
        server = bean.serverAddress
        server_port = bean.serverPort
        version = bean.version
        psk = bean.psk
        userkey = bean.userKey.blankAsNull()
        if (bean.reuse) reuse = true
        network = bean.network.blankAsNull()
        if (bean.version == 4) {
            obfs_mode = bean.obfsMode.takeUnless { it.isBlank() || it == "none" }
            obfs_host = bean.obfsHost.blankAsNull()
        } else {
            mode = bean.mode.takeUnless { it.isBlank() || it == "default" }
        }
    }
}

fun parseSnellOutbound(json: String): SnellBean {
    val options = gson.fromJson(json, JsonObject::class.java)
    fun take(key: String): String? = options.remove(key)?.takeUnless { it.isJsonNull }?.asString
    require(take("type") == "snell") { "Not a Snell outbound" }
    return SnellBean().apply {
        initializeDefaultValues()
        name = take("tag") ?: ""
        serverAddress = take("server") ?: error("Snell server is required")
        serverPort = take("server_port")?.toInt() ?: error("Snell server port is required")
        version = take("version")?.toInt() ?: error("Snell version is required")
        psk = take("psk") ?: ""
        userKey = take("userkey") ?: ""
        reuse = take("reuse")?.toBooleanStrict() ?: false
        options.remove("network")?.takeUnless { it.isJsonNull }?.let {
            val networks = if (it.isJsonArray) it.asJsonArray.map { value -> value.asString } else it.asString.split(",")
            val unique = networks.distinct()
            network = if (unique.isEmpty() || unique.toSet() == setOf("tcp", "udp")) "" else unique.single()
        }
        if (version == 4) {
            obfsMode = take("obfs_mode") ?: "none"
            obfsHost = take("obfs_host") ?: ""
        } else {
            mode = take("mode") ?: "default"
        }
        // Preserve dialer/advanced fields that do not have dedicated controls.
        customOutboundJson = if (options.size() == 0) "" else gson.toJson(options)
        validateSnell(this)
    }
}

fun parseClashSnell(proxy: Map<String, Any?>): SnellBean = SnellBean().apply {
    initializeDefaultValues()
    name = proxy["name"]?.toString() ?: ""
    serverAddress = proxy["server"]?.toString() ?: error("Snell server is required")
    serverPort = proxy["port"]?.toString()?.toInt() ?: error("Snell server port is required")
    // Older Clash Snell formats default to older, unsupported protocols. Do
    // not silently reinterpret an unspecified version as this core's v4.
    version = requireNotNull(proxy["version"]) { "Snell import requires an explicit version (4 or 6)" }.toString().toInt()
    psk = proxy["psk"]?.toString() ?: ""
    userKey = (proxy["userkey"] ?: proxy["user-key"])?.toString() ?: ""
    reuse = proxy["reuse"]?.toString()?.toBooleanStrict() ?: false
    network = proxy["network"]?.toString() ?: if (proxy["udp"] == false) "tcp" else ""
    val obfs = proxy["obfs-opts"] as? Map<*, *>
    obfsMode = obfs?.get("mode")?.toString() ?: "none"
    obfsHost = obfs?.get("host")?.toString() ?: ""
    mode = proxy["mode"]?.toString() ?: "default"
    validateSnell(this)
}
