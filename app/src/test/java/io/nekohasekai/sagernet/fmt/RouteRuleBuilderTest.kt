package io.nekohasekai.sagernet.fmt

import io.nekohasekai.sagernet.database.RuleEntity
import moe.matsuri.nb4a.SingBoxOptions.DNSRule_DefaultOptions
import moe.matsuri.nb4a.SingBoxOptions.RuleSet
import moe.matsuri.nb4a.SingBoxOptions.Rule_DefaultOptions
import moe.matsuri.nb4a.checkEmpty
import moe.matsuri.nb4a.generateRuleSet
import moe.matsuri.nb4a.makeSingBoxRule
import moe.matsuri.nb4a.utils.JavaUtil.gson
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class RouteRuleBuilderTest {
    private fun route(entity: RuleEntity, userIds: List<Int> = emptyList()): Rule_DefaultOptions? =
        buildUserRouteRule(entity, userIds)?.apply {
            outbound = when (entity.outbound) {
                -1L -> TAG_BYPASS
                -2L -> TAG_BLOCK
                else -> TAG_PROXY
            }
        }

    @Test fun builtinsNeverGenerateCatchAllDnsActions() {
        // Put the IP rule first as users can reorder the built-in rules.
        // Previously it emitted {"server":"dns-direct"}; the QUIC rule
        // similarly emitted an unconditional predefined DNS response.
        val entities = listOf(
            RuleEntity(ip = "geoip:cn", outbound = -1),
            RuleEntity(port = "443", network = "udp", outbound = -2),
            RuleEntity(domains = "geosite:category-ads-all", outbound = -2),
            RuleEntity(domains = "googleapis.cn"),
            RuleEntity(domains = "geosite:cn", outbound = -1),
        )
        val routes = entities.map { requireNotNull(route(it)) }
        val dns = routes.flatMap { buildUserDnsRules(it, false) }
        val actual = gson.toJsonTree(dns.map { it.asMap() })
        val expected = gson.fromJson("""[
            {"rule_set":["geosite:category-ads-all"],"action":"predefined","rcode":"NOERROR"},
            {"domain_suffix":["googleapis.cn"],"server":"dns-remote"},
            {"rule_set":["geosite:cn"],"server":"dns-direct"}
        ]""", com.google.gson.JsonElement::class.java)
        assertEquals(expected, actual)
        assertEquals(listOf("geoip:cn"), routes.first().rule_set)
        assertEquals(listOf(443), routes[1].port)
        assertEquals(listOf("udp"), routes[1].network)
        // Optional bridge to the native DNS-routing probe: consume the exact
        // serialized generator output, not a separately hand-written config.
        System.getenv("SEKAIMOD_ROUTING_FIXTURE")?.let { path ->
            File(path).writeText(gson.toJson(mapOf(
                "plain" to buildDnsRules(dns, true, false, listOf("full:node.test"), listOf("ech.test")).map { it.asMap() },
                "fake" to buildDnsRules(routes.flatMap { buildUserDnsRules(it, true) }, true, true,
                    listOf("full:node.test"), listOf("ech.test")).map { it.asMap() },
                "traffic" to buildBuiltinRouteRules(true).map { it.asMap() },
            )))
        }
    }

    @Test fun actionOnlyDnsRulesAreEmpty() {
        assertTrue(DNSRule_DefaultOptions().apply { server = "dns-direct" }.checkEmpty())
        assertTrue(DNSRule_DefaultOptions().apply {
            action = "predefined"
            rcode = "NOERROR"
        }.checkEmpty())
    }

    @Test fun mixedDomainAndIpRulesKeepBothRuleSets() {
        val generated = requireNotNull(route(RuleEntity(
            domains = "geosite:cn, full:Example.CN",
            ip = "geoip:cn, 203.0.113.0/24",
            outbound = -1,
        )))
        val json = gson.toJsonTree(generated.asMap()).asJsonObject
        assertEquals(listOf("geosite:cn", "geoip:cn"), json["rule_set"].asJsonArray.map { it.asString })
        assertEquals("example.cn", json["domain"].asJsonArray.single().asString)
        assertEquals("203.0.113.0/24", json["ip_cidr"].asJsonArray.single().asString)
        val sets = mutableListOf<RuleSet>()
        generateRuleSet(generated.rule_set, sets)
        assertEquals(listOf("geosite:cn", "geoip:cn"), sets.map { it.tag })
    }

    @Test fun trafficOnlyConditionsAreNotDroppedWhenDerivingDnsRules() {
        for (entity in listOf(
            RuleEntity(ip = "geoip:cn"),
            RuleEntity(ip = "geoip:private"),
            RuleEntity(port = "443"),
            RuleEntity(sourcePort = "1024:65535"),
            RuleEntity(network = "udp"),
            RuleEntity(protocol = "quic"),
            RuleEntity(config = """{"invert":true}"""),
        )) {
            entity.domains = "domain:example.com"
            entity.packages = setOf("example.app")
            entity.outbound = -1
            val generated = requireNotNull(route(entity, listOf(10001)))
            assertTrue(entity.toString(), buildUserDnsRules(generated, true).isEmpty())
        }
    }

    @Test fun domainAppAndSourceConditionsArePreservedTogether() {
        val generated = requireNotNull(route(RuleEntity(
            domains = "full:Example.COM", packages = setOf("example.app"),
            source = "192.0.2.0/24", outbound = -1,
        ), listOf(10001)))
        val dns = buildUserDnsRules(generated, false).single()
        assertEquals(listOf("example.com"), dns.domain)
        assertEquals(listOf(10001), dns.user_id)
        assertEquals(listOf("192.0.2.0/24"), dns.source_ip_cidr)
        assertFalse(dns.checkEmpty())
    }

    @Test fun unresolvedApplicationsDoNotBroadenRoutes() {
        assertNull(route(RuleEntity(packages = setOf("uninstalled.app"), outbound = -1)))
        assertNull(route(RuleEntity(
            packages = setOf("uninstalled.app"), domains = "example.com", outbound = -2,
        )))
        assertEquals(listOf(10001), route(RuleEntity(packages = setOf("installed.app")), listOf(10001))?.user_id)
    }

    @Test fun standaloneTrafficSelectorsAreNotDiscarded() {
        assertEquals(true, requireNotNull(route(RuleEntity(ip = "geoip:private"))).ip_is_private)
        assertEquals(listOf("udp"), requireNotNull(route(RuleEntity(network = "udp"))).network)
        assertEquals(listOf("quic"), requireNotNull(route(RuleEntity(protocol = "quic"))).protocol)
        assertEquals(listOf(12345), requireNotNull(route(RuleEntity(sourcePort = "12345"))).source_port)
        assertEquals(listOf("1024:65535"), requireNotNull(route(RuleEntity(sourcePort = "1024:65535"))).source_port_range)
        assertNull(route(RuleEntity()))
    }

    @Test fun fakeDnsOnlyHandlesAddressQueries() {
        val dns = buildUserDnsRules(requireNotNull(route(RuleEntity(domains = "full:example.com"))), true)
        assertEquals(2, dns.size)
        assertEquals("dns-fake", dns[0].server)
        assertEquals(listOf("A", "AAAA"), dns[0].query_type)
        assertEquals(listOf("tun-in"), dns[0].inbound)
        assertEquals(listOf("example.com"), dns[0].domain)
        assertEquals("dns-remote", dns[1].server)
        assertNull(dns[1].query_type)
    }

    @Test fun regularExpressionsKeepTheirMeaning() {
        val expression = "^\\D+\\.example\\.com$"
        val generated = requireNotNull(route(RuleEntity(domains = "regexp:$expression", outbound = -1)))
        assertEquals(listOf(expression), generated.domain_regex)
        assertEquals(listOf(expression), buildUserDnsRules(generated, false).single().domain_regex)
        val dns = DNSRule_DefaultOptions()
        dns.makeSingBoxRule(listOf("regexp:$expression"))
        assertEquals(listOf(expression), dns.domain_regex)
    }

    @Test fun dnsProtocolMatchingRunsAfterSniffing() {
        val rules = buildInboundRouteRules(true, "prefer_ipv4", true)
        assertEquals(listOf("hijack-dns", "sniff", "hijack-dns", "resolve"), rules.map { it.action })
        assertEquals(listOf(53), rules[0].port)
        assertEquals(listOf("dns"), rules[2].protocol)
        assertEquals("prefer_ipv4", rules[3].strategy)
        assertTrue(buildInboundRouteRules(false, "", false).isEmpty())
    }

    @Test fun bootstrapRulesAreNarrowAndPrecedeUserRulesAndFakeDns() {
        val userRule = requireNotNull(route(RuleEntity(domains = "geosite:cn", outbound = -1)))
        val rules = buildDnsRules(buildUserDnsRules(userRule, true), true, true,
            listOf("full:node.test", "full:node.test"), listOf("ech.test", "ech.test"))
            .map { it as DNSRule_DefaultOptions }
        assertEquals(4, rules.size)
        assertEquals(listOf("ech.test"), rules[0].domain)
        assertEquals(listOf("HTTPS", "SVCB"), rules[0].query_type)
        assertEquals(listOf("node.test"), rules[1].domain)
        assertEquals(listOf("geosite:cn"), rules[2].rule_set)
        assertEquals("dns-fake", rules[3].server)
        assertEquals(listOf("A", "AAAA"), rules[3].query_type)
        assertTrue(rules.all { !it.checkEmpty() })
        val disabled = buildDnsRules(buildUserDnsRules(userRule, false), false, false,
            listOf("full:node.test"), listOf("ech.test"))
        assertEquals(2, disabled.size)
        assertTrue(buildDnsRules(emptyList(), true, true,
            listOf("full:node.test"), listOf("ech.test"), forTest = true).isEmpty())
    }

    @Test(expected = NumberFormatException::class)
    fun malformedPortsCannotSilentlyBroadenDomainRules() {
        route(RuleEntity(domains = "example.com", port = "not-a-port"))
    }

    @Test fun emptyBootstrapNamesCannotBecomeCatchAllDirectDns() {
        assertTrue(buildDnsRules(
            listOf(DNSRule_DefaultOptions().apply { server = "dns-direct" }),
            true, false, listOf("full:", "domain:"), listOf("", " "),
        ).isEmpty())
    }

    @Test fun multicastRulesMatchEachDirectionIndependently() {
        val rules = buildBuiltinRouteRules(false)
        assertEquals(2, rules.size)
        assertEquals(listOf("224.0.0.0/3", "ff00::/8"), rules[0].ip_cidr)
        assertNull(rules[0].source_ip_cidr)
        assertEquals(listOf("224.0.0.0/3", "ff00::/8"), rules[1].source_ip_cidr)
        assertNull(rules[1].ip_cidr)
        val withLan = buildBuiltinRouteRules(true)
        assertEquals("reject", withLan[0].action)
        assertEquals("reject", withLan[1].action)
        assertEquals(TAG_BYPASS, withLan.last().outbound)
        assertEquals(true, withLan.last().ip_is_private)
    }
}
