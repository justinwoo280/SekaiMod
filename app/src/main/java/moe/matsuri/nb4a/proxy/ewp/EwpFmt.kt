package moe.matsuri.nb4a.proxy.ewp

import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.fmt.buildXHTTPTransport
import io.nekohasekai.sagernet.fmt.readXHTTPQuery
import io.nekohasekai.sagernet.fmt.resolveXHTTPMode
import io.nekohasekai.sagernet.fmt.shareXHTTPExtra
import io.nekohasekai.sagernet.ktx.linkBuilder
import io.nekohasekai.sagernet.ktx.toLink
import io.nekohasekai.sagernet.ktx.urlSafe
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.SingBoxOptions.MultiplexOptions
import moe.matsuri.nb4a.SingBoxOptions.OutboundECHOptions
import moe.matsuri.nb4a.SingBoxOptions.OutboundRealityOptions
import moe.matsuri.nb4a.SingBoxOptions.OutboundTLSOptions
import moe.matsuri.nb4a.SingBoxOptions.OutboundUTLSOptions
import moe.matsuri.nb4a.SingBoxOptions.Outbound_EwpOptions
import moe.matsuri.nb4a.SingBoxOptions.V2RayTransportOptions
import moe.matsuri.nb4a.SingBoxOptions.V2RayTransportOptions_GRPCOptions
import moe.matsuri.nb4a.SingBoxOptions.V2RayTransportOptions_HTTPOptions
import moe.matsuri.nb4a.SingBoxOptions.V2RayTransportOptions_HTTPUpgradeOptions
import moe.matsuri.nb4a.SingBoxOptions.V2RayTransportOptions_WebsocketOptions
import moe.matsuri.nb4a.utils.listByLineOrComma
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Build the sing-box outbound option object for an EWP profile.
 *
 * Multiplex is intentionally NOT set here — the global mux logic in
 * ConfigBuilder will inject it via _hack_config_map after the fact,
 * matching how VLESS/Trojan are handled.
 */
fun buildSingBoxOutboundEwpBean(bean: EwpBean): Outbound_EwpOptions {
    return Outbound_EwpOptions().apply {
        type = "ewp"
        server = bean.serverAddress
        server_port = bean.serverPort
        uuid = bean.uuid
        // EWP/v2.3: pin the server's Ed25519 signing identity and the
        // server_id bound into the handshake transcript.
        server_public_key = bean.serverPublicKey
        server_id = bean.serverId
        bean.routeEpoch?.let { if (it > 0) route_epoch = it.toLong() }
        early_server_finished = bean.earlyServerFinished == true
        tls = buildEwpTLS(bean)
        transport = buildEwpTransport(bean)
    }
}

private fun buildEwpTLS(bean: EwpBean): OutboundTLSOptions {
    val browser = bean.type == "xhttp" && bean.xhttpBrowser == true
    val browserReality = browser && bean.realityPubKey.isNotBlank()
    return OutboundTLSOptions().apply {
        enabled = true
        insecure = !browser && (bean.allowInsecure || DataStore.globalAllowInsecure)
        if (bean.sni.isNotBlank()) server_name = bean.sni
        if (bean.alpn.isNotBlank()) {
            alpn = bean.alpn.listByLineOrComma()
        }
        if (bean.certificates.isNotBlank() && !browserReality) certificate = bean.certificates

        // TLS fragmentation (anti-censor)
        if (!browser) {
            if (bean.tlsFragment) fragment = true
            if (bean.tlsRecordFragment) record_fragment = true
        }

        // uTLS / Reality
        var fp: String? = if (browser) null else bean.utlsFingerprint
        if (bean.realityPubKey.isNotBlank()) {
            reality = OutboundRealityOptions().apply {
                enabled = true
                public_key = bean.realityPubKey
                short_id = bean.realityShortId
            }
            if (fp.isNullOrBlank()) fp = "chrome"
        }
        if (!browser && !fp.isNullOrBlank()) {
            utls = OutboundUTLSOptions().apply {
                enabled = true
                fingerprint = fp!!
            }
        }

        // ECH (with fork-only query_server_name support).
        //
        // REALITY and ECH are mutually exclusive at the protocol level:
        // REALITY needs the outer SNI in cleartext to mimic a real
        // target, ECH encrypts the SNI inside HPKE. sing-box itself
        // silently ignores ECH when REALITY is on (see
        // common/tls/client.go), so we drop ECH here on purpose to
        // make the resulting JSON match what the kernel will actually
        // execute. UI layer also hides ECH when REALITY is set.
        if (bean.enableECH && bean.realityPubKey.isBlank()) {
            ech = OutboundECHOptions().apply {
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
}

private fun buildEwpTransport(bean: EwpBean): V2RayTransportOptions? {
    return when (bean.type) {
        "tcp", "" -> null

        "ws" -> V2RayTransportOptions_WebsocketOptions().apply {
            type = "ws"
            if (bean.host.isNotBlank()) {
                headers = hashMapOf("Host" to bean.host)
            }
            path = bean.path.takeIf { it.isNotBlank() } ?: "/"
        }

        "http" -> V2RayTransportOptions_HTTPOptions().apply {
            type = "http"
            if (bean.host.isNotBlank()) host = bean.host.split(",")
            path = bean.path.takeIf { it.isNotBlank() } ?: "/"
        }

        "httpupgrade" -> V2RayTransportOptions_HTTPUpgradeOptions().apply {
            type = "httpupgrade"
            host = bean.host
            path = bean.path
        }

        "grpc" -> V2RayTransportOptions_GRPCOptions().apply {
            type = "grpc"
            service_name = bean.path
        }

        "quic" -> V2RayTransportOptions().apply {
            type = "quic"
        }

        "xhttp" -> bean.buildXHTTPTransport(bean.xhttpBrowser == true)

        else -> null
    }
}

// =================================================================
//                         Link scheme
// =================================================================
//
//   ewp://<uuid>@host:port?pk=<ed25519-pub>&sid=<server-id>&type=ws
//        &host=...&path=...&sni=...&alpn=...
//        &fp=chrome&insecure=0&ech=1&echQs=public.example
//        &early_server_finished=1
//        #remarks
//
// Mirrors the ad-hoc style used by AnyTLS / VLESS share links.

fun EwpBean.toUri(): String {
    val builder = linkBuilder()
        .username(uuid)
        .host(serverAddress)
        .port(serverPort)
    val browserXhttp = type == "xhttp" && xhttpBrowser == true

    if (!name.isNullOrBlank()) {
        builder.encodedFragment(name.urlSafe())
    }

    // EWP/v2.3: server Ed25519 signing identity public key + server_id.
    if (serverPublicKey.isNotBlank()) builder.addQueryParameter("pk", serverPublicKey)
    if (serverId.isNotBlank()) builder.addQueryParameter("sid", serverId)
    routeEpoch?.let { if (it > 0) builder.addQueryParameter("re", it.toString()) }
    if (earlyServerFinished == true) builder.addQueryParameter("early_server_finished", "1")

    if (type.isNotBlank() && type != "tcp") builder.addQueryParameter("type", type)
    if (host.isNotBlank()) builder.addQueryParameter("host", host)
    if (path.isNotBlank()) builder.addQueryParameter("path", path)

    if (type == "xhttp") {
        val mode = resolveXHTTPMode(xhttpMode, browserXhttp, xhttpGRPCFraming == true)
        if (mode.isNotBlank() && mode != "auto") builder.addQueryParameter("mode", mode)
        if (xhttpGRPCFraming == true) builder.addQueryParameter("grpc_framing", "1")
        shareXHTTPExtra()?.let { builder.addQueryParameter("extra", it.toString()) }
    }

    if (browserXhttp) {
        builder.addQueryParameter("browser", "1")
    }

    if (sni.isNotBlank()) builder.addQueryParameter("sni", sni)
    if (alpn.isNotBlank()) builder.addQueryParameter("alpn", alpn)
    if (utlsFingerprint.isNotBlank() && !browserXhttp) builder.addQueryParameter("fp", utlsFingerprint)
    if (allowInsecure && !browserXhttp) builder.addQueryParameter("insecure", "1")

    if (realityPubKey.isNotBlank()) {
        builder.addQueryParameter("rpk", realityPubKey)
        if (realityShortId.isNotBlank()) builder.addQueryParameter("rsid", realityShortId)
    }
    if (certificates.isNotBlank() && !(browserXhttp && realityPubKey.isNotBlank())) {
        builder.addQueryParameter("cert", certificates)
    }

    if (enableECH && realityPubKey.isBlank()) {
        builder.addQueryParameter("ech", "1")
        if (echConfig.isNotBlank()) builder.addQueryParameter("echCfg", echConfig.replace("\n", "|"))
        if (echQueryServerName.isNotBlank()) builder.addQueryParameter("echQs", echQueryServerName)
    }

    if (tlsFragment && !browserXhttp) builder.addQueryParameter("frag", "1")
    if (tlsRecordFragment && !browserXhttp) builder.addQueryParameter("rfrag", "1")

    return builder.toLink("ewp")
}

fun parseEwp(url: String): EwpBean {
    val link = url.replace("ewp://", "https://").toHttpUrlOrNull()
        ?: error("invalid ewp link")
    return EwpBean().apply {
        serverAddress = link.host
        serverPort = link.port
        name = link.fragment
        uuid = link.username

        // EWP/v2.3: server Ed25519 signing identity + server_id.
        // ("sk" from v2.1 links is base64 X25519 and cannot be reused;
        // it is intentionally ignored so old links fail loudly at
        // connect time instead of silently misbinding identity.)
        serverPublicKey = link.queryParameter("pk") ?: ""
        serverId = link.queryParameter("sid") ?: ""
        routeEpoch = link.queryParameter("re")?.toIntOrNull() ?: 0
        link.queryParameter("early_server_finished")?.also {
            earlyServerFinished = it == "1" || it == "true"
        }

        type = (link.queryParameter("type") ?: "tcp").let { if (it == "splithttp") "xhttp" else it }
        host = link.queryParameter("host") ?: ""
        path = link.queryParameter("path") ?: ""

        if (type == "xhttp") readXHTTPQuery(link)

        sni = link.queryParameter("sni") ?: ""
        alpn = link.queryParameter("alpn") ?: ""
        certificates = link.queryParameter("cert") ?: ""
        utlsFingerprint = link.queryParameter("fp") ?: ""
        link.queryParameter("insecure")?.also {
            allowInsecure = it == "1" || it == "true"
        }

        link.queryParameter("ech")?.also {
            enableECH = it == "1" || it == "true"
        }
        echConfig = link.queryParameter("echCfg")?.replace("|", "\n") ?: ""
        echQueryServerName = link.queryParameter("echQs") ?: ""

        link.queryParameter("frag")?.also {
            tlsFragment = it == "1" || it == "true"
        }
        link.queryParameter("rfrag")?.also {
            tlsRecordFragment = it == "1" || it == "true"
        }
        realityPubKey = link.queryParameter("rpk") ?: ""
        realityShortId = link.queryParameter("rsid") ?: ""
    }
}
