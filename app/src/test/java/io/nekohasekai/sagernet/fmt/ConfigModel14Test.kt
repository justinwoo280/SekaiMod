package io.nekohasekai.sagernet.fmt

import io.nekohasekai.sagernet.database.ProxyEntity
import io.nekohasekai.sagernet.group.RawUpdater
import moe.matsuri.nb4a.SingBoxOptions.*
import moe.matsuri.nb4a.proxy.config.ConfigBean
import moe.matsuri.nb4a.utils.JavaUtil.gson
import moe.matsuri.nb4a.utils.Util
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ConfigModel14Test {
    private val vless = """{"type":"vless","tag":"proxy","server":"127.0.0.1","server_port":24443,
        "uuid":"11111111-2222-3333-4444-555555555555"}"""

    private fun <T : SingBoxOption> read(json: String, type: Class<T>): T = gson.fromJson(json, type)

    private fun config(outbound: Map<String, Any>) = linkedMapOf<String, Any>(
        "dns" to mapOf("servers" to listOf(mapOf("type" to "local", "tag" to "resolver"))),
        "route" to mapOf("default_domain_resolver" to "resolver"),
        "outbounds" to listOf(outbound),
    )

    private fun imported(): ConfigBean = RawUpdater.parseJSON(JSONObject("""{
        "type":"socks","tag":"imported","server":"127.0.0.1","server_port":24443,
        "connect_timeout":"1500ms","fallback_delay":"0s","tcp_keep_alive":"90s",
        "disable_tcp_keep_alive":false,"udp_fragment":false,
        "domain_resolver":{"server":"resolver","strategy":"prefer_ipv6",
            "disable_cache":false,"rewrite_ttl":0}
    }""")).single() as ConfigBean

    @Test fun dialerDurationsRetainUnitsAndExplicitZero() {
        val json = """{"connect_timeout":"750ms","fallback_delay":"0s"}"""
        for (type in listOf(
            DialerOptions::class.java, Outbound_DirectOptions::class.java,
            Outbound_SocksOptions::class.java, Outbound_HTTPOptions::class.java,
            Outbound_VLESSOptions::class.java, Outbound_VMessOptions::class.java,
            Outbound_TrojanOptions::class.java, Outbound_HysteriaOptions::class.java,
            Outbound_Hysteria2Options::class.java, Outbound_TUICOptions::class.java,
            Outbound_SSHOptions::class.java, Outbound_ShadowTLSOptions::class.java,
            Outbound_EwpOptions::class.java, Outbound_AnyTLSOptions::class.java,
        )) {
            val encoded = read(json, type).asMap()
            assertEquals(type.simpleName, "750ms", encoded["connect_timeout"])
            assertEquals(type.simpleName, "0s", encoded["fallback_delay"])
            assertEquals(type.simpleName, setOf("connect_timeout", "fallback_delay"), encoded.keys)
        }
    }

    @Test fun transportAndServiceDurationsRoundTrip() {
        val json = """{"idle_timeout":"1m30s","ping_timeout":"250ms"}"""
        for (type in listOf(V2RayTransportOptions_HTTPOptions::class.java, V2RayTransportOptions_GRPCOptions::class.java)) {
            val encoded = read(json, type).asMap()
            assertEquals("1m30s", encoded["idle_timeout"])
            assertEquals("250ms", encoded["ping_timeout"])
        }
        assertEquals("30m", read("""{"interval":"30m"}""", NTPOptions::class.java).asMap()["interval"])
        assertEquals("3m", read("""{"interval":"3m"}""", Outbound_URLTestOptions::class.java).asMap()["interval"])
        assertEquals("15s", read("""{"heartbeat":"15s"}""", TUICOutboundOptions::class.java).asMap()["heartbeat"])
        assertEquals("2s", read("""{"max_time_difference":"2s"}""", InboundRealityOptions::class.java).asMap()["max_time_difference"])
    }

    @Test fun udpTimeoutRetainsLegacySecondsAndDurationStrings() {
        for (type in listOf(ListenOptions::class.java, Inbound_TunOptions::class.java, Inbound_MixedOptions::class.java)) {
            for ((wire, expected) in listOf("30" to 30L, "0" to 0L, "\"1m\"" to "1m", "\"0s\"" to "0s")) {
                assertEquals(type.simpleName, expected, read("""{"udp_timeout":$wire}""", type).asMap()["udp_timeout"])
            }
            assertFalse(read("{}", type).asMap().containsKey("udp_timeout"))
        }
    }

    @Test fun omittedOptionsDoNotBecomeZeroOrFalseDefaults() {
        val outbound = read(vless, Outbound_VLESSOptions::class.java).asMap()
        assertEquals(gson.fromJson(vless, Map::class.java), outbound)
        val explicit = read("""{"tcp_fast_open":false,"udp_fragment":false,"connect_timeout":"0s"}""", Outbound_DirectOptions::class.java).asMap()
        assertEquals(false, explicit["tcp_fast_open"])
        assertEquals(false, explicit["udp_fragment"])
        assertEquals("0s", explicit["connect_timeout"])
        assertFalse(explicit.containsKey("fallback_delay"))
    }

    private fun merged(): Map<String, Any> = read(vless, Outbound_VLESSOptions::class.java).apply {
        tls = read("""{"enabled":true,"server_name":"example.test","alpn":["h2"],
            "insecure":true,"utls":{"enabled":true,"fingerprint":"chrome"}}""", OutboundTLSOptions::class.java)
        _hack_config_map["connect_timeout"] = "1s"
        _hack_custom_config = """{"connect_timeout":"0s","tls":{"alpn":null,
            "insecure":false,"utls":{"enabled":false,"fingerprint":null}}}"""
    }.asMap()

    @Test fun nestedNullClearsAnOverrideWithoutLosingFalseOrSiblings() {
        val result = merged()
        assertEquals("0s", result["connect_timeout"])
        val tls = result["tls"] as Map<*, *>
        assertEquals(true, tls["enabled"])
        assertEquals("example.test", tls["server_name"])
        assertEquals(false, tls["insecure"])
        assertFalse(tls.containsKey("alpn"))
        val utls = tls["utls"] as Map<*, *>
        assertEquals(false, utls["enabled"])
        assertFalse(utls.containsKey("fingerprint"))
    }

    @Test fun nativeJsonSurvivesImportStorageAndUniversalSharePayload() {
        val bean = imported()
        assertEquals(1, bean.type)
        val expected = CustomSingBoxOption(bean.config).asMap()
        val entity = ProxyEntity().putBean(bean)
        val stored = KryoConverters.deserialize(ProxyEntity(), KryoConverters.serialize(entity)).requireBean() as ConfigBean
        assertEquals(expected, CustomSingBoxOption(stored.config).asMap())
        // The universal link carries this compressed Kryo payload; exercise its
        // production codecs without Android's Base64-only URI envelope.
        val payload = Util.zlibCompress(KryoConverters.serialize(stored), 9)
        val shared = ProxyEntity(type = entity.type).apply {
            putByteArray(Util.zlibDecompress(payload))
        }.requireBean() as ConfigBean
        assertEquals(expected, CustomSingBoxOption(shared.config).asMap())
        assertEquals("1500ms", expected["connect_timeout"])
        assertEquals("0s", expected["fallback_delay"])
        val resolver = expected["domain_resolver"] as Map<*, *>
        assertEquals(false, resolver["disable_cache"])
        assertEquals(0L, resolver["rewrite_ttl"])
        assertEquals(false, expected["udp_fragment"])
    }

    @Test fun exportNativeConfigModelFixtures() {
        val fixtures = linkedMapOf<String, Map<String, Any>>()
        fun add(name: String, config: Map<String, Any>, valid: Boolean = true) {
            fixtures[name] = mapOf("config" to config, "valid" to valid)
        }
        val direct = read("""{"type":"direct","tag":"direct","connect_timeout":"750ms",
            "fallback_delay":"300ms","tcp_fast_open":false,"udp_fragment":false}""", Outbound_DirectOptions::class.java)
        add("direct-durations", config(direct.asMap()))
        val outbound = read(vless, Outbound_VLESSOptions::class.java).apply {
            _hack_custom_config = """{"connect_timeout":"5s","fallback_delay":"0s"}"""
        }
        add("vless-durations", config(outbound.asMap()))
        for ((name, transport) in listOf(
            "http" to read("""{"type":"http","path":"/fixture","idle_timeout":"1m","ping_timeout":"750ms"}""", V2RayTransportOptions_HTTPOptions::class.java),
            "grpc" to read("""{"type":"grpc","service_name":"fixture","idle_timeout":"0s","ping_timeout":"1s","permit_without_stream":false}""", V2RayTransportOptions_GRPCOptions::class.java),
        )) {
            add("transport-$name", config(read(vless, Outbound_VLESSOptions::class.java).apply { this.transport = transport }.asMap()))
        }
        for ((name, value) in listOf("legacy" to "30", "duration" to "\"1m\"", "zero" to "\"0s\"")) {
            val inbound = read("""{"type":"mixed","tag":"mixed-in","listen":"127.0.0.1",
                "listen_port":1080,"udp_timeout":$value}""", Inbound_MixedOptions::class.java)
            add("udp-$name", config(direct.asMap()).apply { put("inbounds", listOf(inbound.asMap())) })
        }
        val group = read("""{"type":"urltest","tag":"auto","outbounds":["direct"],
            "interval":"3m"}""", Outbound_URLTestOptions::class.java)
        add("urltest-interval", config(direct.asMap()).apply { put("outbounds", listOf(direct.asMap(), group.asMap())) })
        val ntp = read("""{"enabled":true,"server":"127.0.0.1","server_port":123,
            "interval":"30m","connect_timeout":"2s","fallback_delay":"200ms"}""", NTPOptions::class.java)
        add("ntp-interval", config(direct.asMap()).apply { put("ntp", ntp.asMap()) })
        add("nested-null-override", config(merged()))
        add("raw-import", config(CustomSingBoxOption(imported().config).asMap()))
        val route = Rule_DefaultOptions().apply {
            _hack_custom_config = """{"action":"route-options","fallback_delay":"300ms","udp_timeout":"0s"}"""
        }
        add("route-durations", config(direct.asMap()).apply {
            put("route", mapOf("default_domain_resolver" to "resolver", "rules" to listOf(route.asMap())))
        })
        add("reject-unitless-number", config(CustomSingBoxOption("""{"type":"direct","connect_timeout":5}""").asMap()), false)
        add("reject-removed-inbound-field", config(direct.asMap()).apply {
            put("inbounds", listOf(read("""{"type":"mixed","listen":"127.0.0.1","listen_port":1080,"sniff":true}""", Inbound_MixedOptions::class.java).asMap()))
        }, false)
        System.getenv("SEKAIMOD_CONFIG14_FIXTURE")?.let { File(it).writeText(gson.toJson(fixtures)) }
    }
}
