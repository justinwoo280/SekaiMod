package io.nekohasekai.sagernet.fmt

import moe.matsuri.nb4a.SingBoxOptions.DNSServerOptions
import moe.matsuri.nb4a.SingBoxOptions.DomainResolveOptions
import java.net.URI

fun domainResolver(server: String, strategy: String? = null) = DomainResolveOptions().apply {
    this.server = server
    this.strategy = strategy?.takeIf { it.isNotBlank() }
}

/** Convert the DNS address saved by the settings UI to the typed 1.14 format. */
fun buildDNSServer(address: String, tag: String, resolver: String, detour: String? = null): DNSServerOptions {
    val value = address.trim()
    require(value.isNotEmpty()) { "Empty DNS address ($tag)" }
    if (value == "local" || value == "system") return DNSServerOptions().apply {
        type = "local"
        this.tag = tag
        this.detour = detour
    }
    val scheme = if ("://" in value) value.substringBefore("://").lowercase() else "udp"
    require(scheme in listOf("udp", "tcp", "tls", "https", "quic", "h3", "dhcp")) {
        "Unsupported DNS address ($tag): $value"
    }
    val authority = value.substringAfter("://", value)
    if (scheme == "dhcp") return DNSServerOptions().apply {
        type = "dhcp"
        this.tag = tag
        interface_ = authority.takeUnless { it == "auto" || it.isBlank() }
        this.detour = detour
    }
    val uri = URI("$scheme://" + if ("://" !in value && authority.count { it == ':' } > 1 && !authority.startsWith("[")) "[$authority]" else authority)
    val host = uri.host?.removeSurrounding("[", "]")
    require(!host.isNullOrBlank() && uri.userInfo == null && uri.fragment == null) { "Invalid DNS address ($tag): $value" }
    require(uri.port == -1 || uri.port in 1..65535) { "Invalid DNS port ($tag): $value" }
    val isHTTPS = scheme == "https" || scheme == "h3"
    require(isHTTPS || (uri.rawPath.isNullOrEmpty() && uri.rawQuery == null)) { "DNS path requires HTTPS ($tag): $value" }
    return DNSServerOptions().apply {
        type = scheme
        this.tag = tag
        server = host
        if (uri.port != -1) server_port = uri.port
        if (isHTTPS) {
            path = uri.rawPath?.takeIf { it.isNotEmpty() } ?: "/dns-query"
            uri.rawQuery?.let { path += "?$it" }
        }
        // Literal addresses must not depend on another DNS transport.
        if (!host.contains(':') && !host.matches(Regex("[0-9.]+"))) {
            domain_resolver = domainResolver(resolver)
        }
        this.detour = detour
    }
}
