package moe.matsuri.nb4a.proxy.anytls

import io.nekohasekai.sagernet.ktx.blankAsNull
import io.nekohasekai.sagernet.ktx.linkBuilder
import io.nekohasekai.sagernet.ktx.toLink
import io.nekohasekai.sagernet.ktx.urlSafe
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.utils.listByLineOrComma
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun buildSingBoxOutboundAnyTLSBean(bean: AnyTLSBean): SingBoxOptions.Outbound_AnyTLSOptions {
    return SingBoxOptions.Outbound_AnyTLSOptions().apply {
        type = "anytls"
        server = bean.serverAddress
        server_port = bean.serverPort
        password = bean.password
        idle_session_check_interval = bean.idleSessionCheckInterval.blankAsNull()
        idle_session_timeout = bean.idleSessionTimeout.blankAsNull()
        require(bean.minIdleSession >= 0) { "AnyTLS min_idle_session must not be negative" }
        if (bean.minIdleSession > 0) min_idle_session = bean.minIdleSession
        client_metadata = bean.clientMetadata.blankAsNull()

        tls = SingBoxOptions.OutboundTLSOptions().apply {
            enabled = true
            server_name = bean.sni.blankAsNull()
            if (bean.allowInsecure) insecure = true
            alpn = bean.alpn.blankAsNull()?.listByLineOrComma()
            bean.certificates.blankAsNull()?.let {
                certificate = it
            }
            bean.utlsFingerprint.blankAsNull()?.let {
                utls = SingBoxOptions.OutboundUTLSOptions().apply {
                    enabled = true
                    fingerprint = it
                }
            }
            if (bean.enableECH) {
                ech = SingBoxOptions.OutboundECHOptions().apply {
                    enabled = true
                    bean.echConfig.blankAsNull()?.let {
                        config = it.lines()
                    }
                    bean.echQueryServerName.blankAsNull()?.let {
                        query_server_name = it
                    }
                }
            }
        }
    }
}

fun AnyTLSBean.toUri(): String {
    val builder = linkBuilder()
        .host(serverAddress)
        .port(serverPort)
        .username(password)
    if (!name.isNullOrBlank()) {
        builder.encodedFragment(name.urlSafe())
    }
    if (allowInsecure) {
        builder.addQueryParameter("insecure", "1")
    }
    if (!sni.isNullOrBlank()) {
        builder.addQueryParameter("sni", sni)
    }
    if (alpn.isNotBlank()) builder.addQueryParameter("alpn", alpn.replace("\n", ","))
    if (idleSessionCheckInterval.isNotBlank()) builder.addQueryParameter("idle_session_check_interval", idleSessionCheckInterval)
    if (idleSessionTimeout.isNotBlank()) builder.addQueryParameter("idle_session_timeout", idleSessionTimeout)
    if (minIdleSession > 0) builder.addQueryParameter("min_idle_session", minIdleSession.toString())
    if (clientMetadata.isNotBlank()) builder.addQueryParameter("client_metadata", clientMetadata)
    if (!utlsFingerprint.isNullOrBlank()) {
        builder.addQueryParameter("fp", utlsFingerprint)
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
    return builder.toLink("anytls")
}

fun parseAnytls(url: String): AnyTLSBean {
    // https://github.com/anytls/anytls-go/blob/main/docs/uri_scheme.md
    val link = url.replace("anytls://", "https://").toHttpUrlOrNull() ?: error(
        "invalid anytls link $url"
    )
    return AnyTLSBean().apply {
        serverAddress = link.host
        serverPort = link.port
        name = link.fragment
        password = link.username
        sni = link.queryParameter("sni") ?: ""
        alpn = link.queryParameter("alpn") ?: ""
        idleSessionCheckInterval = link.queryParameter("idle_session_check_interval") ?: ""
        idleSessionTimeout = link.queryParameter("idle_session_timeout") ?: ""
        minIdleSession = link.queryParameter("min_idle_session")?.toInt() ?: 0
        clientMetadata = link.queryParameter("client_metadata") ?: ""
        link.queryParameter("insecure")?.also {
            allowInsecure = it == "1" || it == "true"
        }
        link.queryParameter("fp")?.let {
            utlsFingerprint = it
        }
        link.queryParameter("ech")?.let {
            enableECH = it == "1" || it == "true"
        }
        link.queryParameter("echCfg")?.let {
            echConfig = it.replace("|", "\n")
        }
        link.queryParameter("echQs")?.let {
            echQueryServerName = it
        }
    }
}
