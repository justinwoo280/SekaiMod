package io.nekohasekai.sagernet.fmt.hysteria

import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.fmt.LOCALHOST
import io.nekohasekai.sagernet.ktx.*
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.listByLineOrComma
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.io.File


// hysteria://host:port?auth=123456&peer=sni.domain&insecure=1|0&upmbps=100&downmbps=100&alpn=hysteria&obfs=xplus&obfsParam=123456#remarks
fun parseHysteria1(url: String): HysteriaBean {
    val link = url.replace("hysteria://", "https://").toHttpUrlOrNull() ?: error(
        "invalid hysteria link $url"
    )
    return HysteriaBean().apply {
        protocolVersion = 1
        serverAddress = link.host
        serverPorts = link.port.toString()
        name = link.fragment

        link.queryParameter("mport")?.also {
            serverPorts = it
        }
        link.queryParameter("peer")?.also {
            sni = it
        }
        link.queryParameter("auth")?.takeIf { it.isNotBlank() }?.also {
            authPayloadType = HysteriaBean.TYPE_STRING
            authPayload = it
        }
        link.queryParameter("insecure")?.also {
            allowInsecure = it == "1" || it == "true"
        }
        link.queryParameter("upmbps")?.also {
            uploadMbps = it.toIntOrNull() ?: uploadMbps
        }
        link.queryParameter("downmbps")?.also {
            downloadMbps = it.toIntOrNull() ?: downloadMbps
        }
        link.queryParameter("alpn")?.also {
            alpn = it
        }
        link.queryParameter("obfsParam")?.also {
            obfuscation = it
        }
        link.queryParameter("protocol")?.also {
            when (it) {
                "faketcp" -> {
                    protocol = HysteriaBean.PROTOCOL_FAKETCP
                }

                "wechat-video" -> {
                    protocol = HysteriaBean.PROTOCOL_WECHAT_VIDEO
                }
            }
        }
    }
}

// hysteria2://[auth@]hostname[:port]/?[key=value]&[key=value]...
fun parseHysteria2(url: String): HysteriaBean {
    val link = url
        .replace("hysteria2://", "https://")
        .replace("hy2://", "https://")
        .toHttpUrlOrNull() ?: error("invalid hysteria link $url")
    return HysteriaBean().apply {
        protocolVersion = 2
        serverAddress = link.host
        serverPorts = link.port.toString()
        authPayload = if (link.password.isNotBlank()) {
            link.username + ":" + link.password
        } else {
            link.username
        }
        name = link.fragment

        link.queryParameter("mport")?.also {
            serverPorts = it
        }
        link.queryParameter("sni")?.also {
            sni = it
        }
        link.queryParameter("insecure")?.also {
            allowInsecure = it == "1" || it == "true"
        }
//        link.queryParameter("upmbps")?.also {
//            uploadMbps = it.toIntOrNull() ?: uploadMbps
//        }
//        link.queryParameter("downmbps")?.also {
//            downloadMbps = it.toIntOrNull() ?: downloadMbps
//        }
        link.queryParameter("obfs-password")?.also {
            obfuscation = it
        }
        link.queryParameter("obfs")?.let { obfsType = it }
        link.queryParameter("obfs-min-packet-size")?.let { geckoMinPacketSize = it.toInt() }
        link.queryParameter("obfs-max-packet-size")?.let { geckoMaxPacketSize = it.toInt() }
        link.queryParameter("hop-interval")?.let { hopInterval = it.toInt() }
        link.queryParameter("hop-interval-max")?.let { hopIntervalMax = it.toInt() }
        link.queryParameter("bbr-profile")?.let { bbrProfile = it }
        link.queryParameter("disable-chrome-parrot")?.let { disableChromeParrot = it == "1" || it == "true" }
        link.queryParameter("brutal-debug")?.let { brutalDebug = it == "1" || it == "true" }
        link.queryParameter("ech")?.also {
            enableECH = it == "1" || it == "true"
        }
        link.queryParameter("echCfg")?.also {
            echConfig = it.replace("|", "\n")
        }
        link.queryParameter("echQs")?.also {
            echQueryServerName = it
        }
//        link.queryParameter("pinSHA256")?.also {
//            // TODO your box do not support it
//        }
    }
}

fun HysteriaBean.toUri(): String {
    var un = ""
    var pw = ""
    if (protocolVersion == 2) {
        if (authPayload.contains(":")) {
            un = authPayload.substringBefore(":")
            pw = authPayload.substringAfter(":")
        } else {
            un = authPayload
        }
    }
    //
    val builder = linkBuilder()
        .host(serverAddress)
        .port(getFirstPort(serverPorts))
        .username(un)
        .password(pw)
    if (serverPorts.toIntOrNull() == null) {
        builder.addQueryParameter("mport", serverPorts)
    }
    if (name.isNotBlank()) {
        builder.encodedFragment(name.urlSafe())
    }
    if (allowInsecure) {
        builder.addQueryParameter("insecure", "1")
    }
    if (protocolVersion == 1) {
        if (sni.isNotBlank()) {
            builder.addQueryParameter("peer", sni)
        }
        if (authPayload.isNotBlank()) {
            builder.addQueryParameter("auth", authPayload)
        }
        builder.addQueryParameter("upmbps", "$uploadMbps")
        builder.addQueryParameter("downmbps", "$downloadMbps")
        if (alpn.isNotBlank()) {
            builder.addQueryParameter("alpn", alpn)
        }
        if (obfuscation.isNotBlank()) {
            builder.addQueryParameter("obfs", "xplus")
            builder.addQueryParameter("obfsParam", obfuscation)
        }
        when (protocol) {
            HysteriaBean.PROTOCOL_FAKETCP -> {
                builder.addQueryParameter("protocol", "faketcp")
            }

            HysteriaBean.PROTOCOL_WECHAT_VIDEO -> {
                builder.addQueryParameter("protocol", "wechat-video")
            }
        }
    } else {
        if (sni.isNotBlank()) {
            builder.addQueryParameter("sni", sni)
        }
        if (obfuscation.isNotBlank() && obfsType != "none") {
            builder.addQueryParameter("obfs", obfsType)
            builder.addQueryParameter("obfs-password", obfuscation)
            if (obfsType == "gecko") {
                if (geckoMinPacketSize > 0) builder.addQueryParameter("obfs-min-packet-size", geckoMinPacketSize.toString())
                if (geckoMaxPacketSize > 0) builder.addQueryParameter("obfs-max-packet-size", geckoMaxPacketSize.toString())
            }
        }
        builder.addQueryParameter("hop-interval", hopInterval.toString())
        if (hopIntervalMax > 0) builder.addQueryParameter("hop-interval-max", hopIntervalMax.toString())
        if (bbrProfile.isNotBlank()) builder.addQueryParameter("bbr-profile", bbrProfile)
        if (disableChromeParrot) builder.addQueryParameter("disable-chrome-parrot", "1")
        if (brutalDebug) builder.addQueryParameter("brutal-debug", "1")
    }
    if (enableECH) {
        builder.addQueryParameter("ech", "1")
        if (echConfig.isNotBlank()) {
            builder.addQueryParameter("echCfg", echConfig.replace("\n", "|"))
        }
        if (echQueryServerName.isNotBlank()) {
            builder.addQueryParameter("echQs", echQueryServerName)
        }
    }
    return builder.toLink(if (protocolVersion == 2) "hy2" else "hysteria")
}

fun JSONObject.parseHysteria1Json(): HysteriaBean {
    // TODO parse HY2 JSON+YAML
    return HysteriaBean().apply {
        protocolVersion = 1
        serverAddress = optString("server").substringBeforeLast(":")
        serverPorts = optString("server").substringAfterLast(":")
        uploadMbps = getIntNya("up_mbps")
        downloadMbps = getIntNya("down_mbps")
        obfuscation = getStr("obfs")
        getStr("auth")?.also {
            authPayloadType = HysteriaBean.TYPE_BASE64
            authPayload = it
        }
        getStr("auth_str")?.also {
            authPayloadType = HysteriaBean.TYPE_STRING
            authPayload = it
        }
        getStr("protocol")?.also {
            when (it) {
                "faketcp" -> {
                    protocol = HysteriaBean.PROTOCOL_FAKETCP
                }

                "wechat-video" -> {
                    protocol = HysteriaBean.PROTOCOL_WECHAT_VIDEO
                }
            }
        }
        sni = getStr("server_name")
        alpn = getStr("alpn")
        allowInsecure = getBool("insecure")

        streamReceiveWindow = getIntNya("recv_window_conn")
        connectionReceiveWindow = getIntNya("recv_window")
        disableMtuDiscovery = getBool("disable_mtu_discovery")
    }
}

fun HysteriaBean.buildHysteria1Config(port: Int, cacheFile: (() -> File)?): String {
    if (protocolVersion != 1) {
        throw Exception("error version: $protocolVersion")
    }
    return JSONObject().apply {
        put("server", displayAddress())
        when (protocol) {
            HysteriaBean.PROTOCOL_FAKETCP -> {
                put("protocol", "faketcp")
            }

            HysteriaBean.PROTOCOL_WECHAT_VIDEO -> {
                put("protocol", "wechat-video")
            }
        }
        put("up_mbps", uploadMbps)
        put("down_mbps", downloadMbps)
        put(
            "socks5", JSONObject(
                mapOf(
                    "listen" to "$LOCALHOST:$port",
                )
            )
        )
        put("retry", 5)
        put("fast_open", true)
        put("lazy_start", true)
        put("obfs", obfuscation)
        when (authPayloadType) {
            HysteriaBean.TYPE_BASE64 -> put("auth", authPayload)
            HysteriaBean.TYPE_STRING -> put("auth_str", authPayload)
        }
        if (sni.isBlank() && finalAddress == LOCALHOST && !serverAddress.isIpAddress()) {
            sni = serverAddress
        }
        if (sni.isNotBlank()) {
            put("server_name", sni)
        }
        if (alpn.isNotBlank()) put("alpn", alpn)
        if (caText.isNotBlank() && cacheFile != null) {
            val caFile = cacheFile()
            caFile.writeText(caText)
            put("ca", caFile.absolutePath)
        }

        if (allowInsecure) put("insecure", true)
        if (streamReceiveWindow > 0) put("recv_window_conn", streamReceiveWindow)
        if (connectionReceiveWindow > 0) put("recv_window", connectionReceiveWindow)
        if (disableMtuDiscovery) put("disable_mtu_discovery", true)

        put("hop_interval", hopInterval)
    }.toStringPretty()
}

fun isMultiPort(hyAddr: String): Boolean {
    if (!hyAddr.contains(":")) return false
    val p = hyAddr.substringAfterLast(":")
    if (p.contains("-") || p.contains(",")) return true
    return false
}

fun getFirstPort(portStr: String): Int {
    return portStr.substringBefore(",").substringBefore("-").substringBefore(":").trim().toIntOrNull() ?: 443
}

fun HysteriaBean.canUseSingBox(): Boolean {
    return protocolVersion == 2 || protocol == HysteriaBean.PROTOCOL_UDP
}

fun buildSingBoxOutboundHysteriaBean(bean: HysteriaBean): SingBoxOptions.SingBoxOption {
    return when (bean.protocolVersion) {
        1 -> SingBoxOptions.Outbound_HysteriaOptions().apply {
            type = "hysteria"
            server = bean.serverAddress
            val port = bean.serverPorts.toIntOrNull()
            if (port != null) {
                server_port = port
            } else {
                server_ports = hopPortsToSingboxList(bean.serverPorts)
            }
            hop_interval = "${bean.hopInterval}s"
            up_mbps = bean.uploadMbps
            down_mbps = bean.downloadMbps
            obfs = bean.obfuscation
            disable_path_mtu_discovery = bean.disableMtuDiscovery
            when (bean.authPayloadType) {
                HysteriaBean.TYPE_BASE64 -> auth = bean.authPayload
                HysteriaBean.TYPE_STRING -> auth_str = bean.authPayload
            }
            if (bean.streamReceiveWindow > 0) {
                stream_receive_window = bean.streamReceiveWindow.toLong()
            }
            if (bean.connectionReceiveWindow > 0) {
                connection_receive_window = bean.connectionReceiveWindow.toLong()
            }
            tls = SingBoxOptions.OutboundTLSOptions().apply {
                if (bean.sni.isNotBlank()) {
                    server_name = bean.sni
                }
                if (bean.alpn.isNotBlank()) {
                    alpn = bean.alpn.listByLineOrComma()
                }
                if (bean.caText.isNotBlank()) {
                    certificate = bean.caText
                }
                insecure = bean.allowInsecure || DataStore.globalAllowInsecure
                enabled = true
                applyEch(bean)
            }
        }

        2 -> SingBoxOptions.Outbound_Hysteria2Options().apply {
            type = "hysteria2"
            server = bean.serverAddress
            val port = bean.serverPorts.toIntOrNull()
            if (port != null) {
                server_port = port
            } else {
                server_ports = hopPortsToSingboxList(bean.serverPorts)
            }
            hop_interval = "${bean.hopInterval}s"
            require(bean.hopIntervalMax == 0 || bean.hopIntervalMax >= bean.hopInterval) { "Maximum hop interval must not be shorter than the minimum" }
            if (bean.hopIntervalMax > 0) hop_interval_max = "${bean.hopIntervalMax}s"
            require(bean.bbrProfile in listOf("", "standard", "conservative", "aggressive")) { "Unknown Hysteria2 BBR profile" }
            if (bean.bbrProfile.isNotBlank()) bbr_profile = bean.bbrProfile
            if (bean.disableChromeParrot) disable_chrome_parrot = true
            if (bean.brutalDebug) brutal_debug = true
            up_mbps = bean.uploadMbps
            down_mbps = bean.downloadMbps
            require(bean.obfsType in listOf("none", "salamander", "gecko")) { "Unknown Hysteria2 obfuscation type" }
            if (bean.obfuscation.isNotBlank() && bean.obfsType != "none") {
                obfs = SingBoxOptions.Hysteria2Obfs().apply {
                    type = bean.obfsType
                    password = bean.obfuscation
                    if (bean.obfsType == "gecko") {
                        require(bean.geckoMinPacketSize >= 0 && bean.geckoMaxPacketSize >= 0) { "Gecko packet sizes must not be negative" }
                        require(bean.geckoMinPacketSize == 0 || bean.geckoMaxPacketSize == 0 || bean.geckoMinPacketSize <= bean.geckoMaxPacketSize) { "Invalid Gecko packet size range" }
                        if (bean.geckoMinPacketSize > 0) min_packet_size = bean.geckoMinPacketSize
                        if (bean.geckoMaxPacketSize > 0) max_packet_size = bean.geckoMaxPacketSize
                    }
                }
            }
            disable_path_mtu_discovery = bean.disableMtuDiscovery
            password = bean.authPayload
            if (bean.streamReceiveWindow > 0) {
                stream_receive_window = bean.streamReceiveWindow.toLong()
            }
            if (bean.connectionReceiveWindow > 0) {
                connection_receive_window = bean.connectionReceiveWindow.toLong()
            }
            tls = SingBoxOptions.OutboundTLSOptions().apply {
                if (bean.sni.isNotBlank()) {
                    server_name = bean.sni
                }
                alpn = listOf("h3")
                if (bean.caText.isNotBlank()) {
                    certificate = bean.caText
                }
                insecure = bean.allowInsecure || DataStore.globalAllowInsecure
                enabled = true
                applyEch(bean)
            }
        }

        else -> error("error_version $bean.protocolVersion")
    }
}

private fun SingBoxOptions.OutboundTLSOptions.applyEch(bean: HysteriaBean) {
    if (bean.enableECH) {
        ech = SingBoxOptions.OutboundECHOptions().apply {
            enabled = true
            if (bean.echConfig.isNotBlank()) {
                config = bean.echConfig.lines()
            }
            if (bean.echQueryServerName.isNotBlank()) {
                query_server_name = bean.echQueryServerName
            }
        }
    }
}

fun hopPortsToSingboxList(s: String): List<String> {
    return s.split(",").map {
        val range = it.trim().replace("-", ":")
        val bounds = range.split(":")
        require(bounds.size in 1..2) { "Invalid port range: $it" }
        val ports = bounds.map { part -> part.toIntOrNull() ?: error("Invalid port: $part") }
        require(ports.all { port -> port in 1..65535 } && ports.first() <= ports.last()) { "Invalid port range: $it" }
        // The core's server_ports parser requires ranges, even for one port.
        if (ports.size == 1) "${ports[0]}:${ports[0]}" else ports.joinToString(":")
    }
}
