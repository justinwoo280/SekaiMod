package io.nekohasekai.sagernet.fmt

import io.nekohasekai.sagernet.database.RuleEntity
import moe.matsuri.nb4a.SingBoxOptions.DNSRule
import moe.matsuri.nb4a.SingBoxOptions.DNSRule_DefaultOptions
import moe.matsuri.nb4a.SingBoxOptions.Rule_DefaultOptions
import moe.matsuri.nb4a.checkEmpty
import moe.matsuri.nb4a.makeSingBoxRule
import moe.matsuri.nb4a.utils.listByLineOrComma

internal fun buildUserRouteRule(rule: RuleEntity, userIds: List<Int>): Rule_DefaultOptions? {
    // Uninstalled/unresolvable packages must not broaden an application rule
    // into a rule applying to every app.
    if (rule.packages.isNotEmpty() && userIds.isEmpty()) return null
    return Rule_DefaultOptions().apply {
        if (userIds.isNotEmpty()) user_id = userIds
        if (rule.domains.isNotBlank()) makeSingBoxRule(rule.domains.listByLineOrComma(), false)
        if (rule.ip.isNotBlank()) makeSingBoxRule(rule.ip.listByLineOrComma(), true)
        if (rule.port.isNotBlank()) {
            port = mutableListOf()
            port_range = mutableListOf()
            rule.port.listByLineOrComma().forEach {
                if (it.contains(":")) port_range.add(it) else port.add(it.toInt())
            }
        }
        if (rule.sourcePort.isNotBlank()) {
            source_port = mutableListOf()
            source_port_range = mutableListOf()
            rule.sourcePort.listByLineOrComma().forEach {
                if (it.contains(":")) source_port_range.add(it) else source_port.add(it.toInt())
            }
        }
        if (rule.network.isNotBlank()) network = rule.network.listByLineOrComma()
        if (rule.source.isNotBlank()) source_ip_cidr = rule.source.listByLineOrComma()
        if (rule.protocol.isNotBlank()) protocol = rule.protocol.listByLineOrComma()
        _hack_custom_config = rule.config
    }.takeUnless { it.checkEmpty() }
}

internal fun buildUserDnsRules(rule: Rule_DefaultOptions, useFakeDns: Boolean): List<DNSRule_DefaultOptions> {
    // DNS queries do not carry the eventual connection's destination IP/port,
    // transport or sniffed protocol. Dropping those selectors would broaden the
    // rule. Custom route JSON is opaque and must not be projected into DNS.
    if (!rule._hack_custom_config.isNullOrBlank() || rule._hack_config_map.isNotEmpty() ||
        !rule.ip_cidr.isNullOrEmpty() || rule.ip_is_private == true ||
        !rule.port.isNullOrEmpty() || !rule.port_range.isNullOrEmpty() ||
        !rule.source_port.isNullOrEmpty() || !rule.source_port_range.isNullOrEmpty() ||
        !rule.network.isNullOrEmpty() || !rule.protocol.isNullOrEmpty() ||
        rule.rule_set.orEmpty().any { !it.startsWith("geosite:") }
    ) return emptyList()

    fun matcher() = DNSRule_DefaultOptions().apply {
        domain = rule.domain
        domain_suffix = rule.domain_suffix
        domain_keyword = rule.domain_keyword
        domain_regex = rule.domain_regex
        rule_set = rule.rule_set
        user_id = rule.user_id
        source_ip_cidr = rule.source_ip_cidr
    }
    val dnsRule = matcher()
    if (dnsRule.checkEmpty()) return emptyList()
    return when (rule.outbound) {
        TAG_BYPASS -> listOf(dnsRule.apply { server = "dns-direct" })
        TAG_PROXY -> buildList {
            if (useFakeDns) add(matcher().apply {
                inbound = listOf("tun-in")
                query_type = listOf("A", "AAAA")
                server = "dns-fake"
            })
            add(dnsRule.apply { server = "dns-remote" })
        }
        TAG_BLOCK -> listOf(dnsRule.apply {
            action = "predefined"
            rcode = "NOERROR"
        })
        else -> emptyList()
    }
}

internal fun buildInboundRouteRules(sniff: Boolean, resolveStrategy: String, hijackDns: Boolean) =
    buildList {
        if (hijackDns) add(Rule_DefaultOptions().apply {
            port = listOf(53)
            action = "hijack-dns"
        })
        if (sniff) add(Rule_DefaultOptions().apply { action = "sniff" })
        // Protocol matching must run after sniffing, including DNS on non-53 ports.
        if (hijackDns) add(Rule_DefaultOptions().apply {
            protocol = listOf("dns")
            action = "hijack-dns"
        })
        if (resolveStrategy.isNotEmpty()) add(Rule_DefaultOptions().apply {
            action = "resolve"
            strategy = resolveStrategy
        })
    }

internal fun buildDnsRules(
    userRules: List<DNSRule_DefaultOptions>,
    enableDnsRouting: Boolean,
    useFakeDns: Boolean,
    bootstrapDomains: List<String>,
    echPublicNames: List<String>,
    forTest: Boolean = false,
): List<DNSRule> = if (forTest) emptyList() else buildList {
    val echNames = echPublicNames.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()
    if (echNames.isNotEmpty()) add(DNSRule_DefaultOptions().apply {
        query_type = listOf("HTTPS", "SVCB")
        domain = echNames
        server = "dns-direct"
    })
    val bootstrapRule = DNSRule_DefaultOptions().apply {
        makeSingBoxRule(bootstrapDomains.distinct())
        server = "dns-direct"
    }
    if (!bootstrapRule.checkEmpty()) add(bootstrapRule)
    if (enableDnsRouting) addAll(userRules.filterNot { it.checkEmpty() })
    if (useFakeDns) add(DNSRule_DefaultOptions().apply {
        inbound = listOf("tun-in")
        query_type = listOf("A", "AAAA")
        server = "dns-fake"
        disable_cache = true
    })
}

internal fun buildBuiltinRouteRules(bypassLan: Boolean): List<Rule_DefaultOptions> = buildList {
    // Putting source and destination in the same rule would require BOTH to
    // match. Each direction must independently reject multicast/reserved IPs.
    add(Rule_DefaultOptions().apply {
        ip_cidr = listOf("224.0.0.0/3", "ff00::/8")
        action = "reject"
    })
    add(Rule_DefaultOptions().apply {
        source_ip_cidr = listOf("224.0.0.0/3", "ff00::/8")
        action = "reject"
    })
    // sing-box treats every non-public address as ip_is_private, including
    // multicast, so this fallback must follow the explicit multicast rejects.
    if (bypassLan) add(Rule_DefaultOptions().apply {
        outbound = TAG_BYPASS
        ip_is_private = true
    })
}
