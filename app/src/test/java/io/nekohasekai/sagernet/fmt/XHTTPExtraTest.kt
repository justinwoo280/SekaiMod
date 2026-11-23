package io.nekohasekai.sagernet.fmt

import com.esotericsoftware.kryo.io.ByteBufferInput
import com.google.gson.JsonObject
import io.nekohasekai.sagernet.fmt.trojan.TrojanBean
import io.nekohasekai.sagernet.fmt.trojan.parseTrojan
import io.nekohasekai.sagernet.fmt.v2ray.StandardV2RayBean
import io.nekohasekai.sagernet.fmt.v2ray.buildSingBoxOutboundStandardV2RayBean
import io.nekohasekai.sagernet.fmt.v2ray.parseV2Ray
import io.nekohasekai.sagernet.fmt.v2ray.toUriVMessVLESSTrojan
import io.nekohasekai.sagernet.group.retainSubscriptionOverrides
import moe.matsuri.nb4a.SingBoxOptions.Outbound
import moe.matsuri.nb4a.proxy.ewp.EwpBean
import moe.matsuri.nb4a.proxy.ewp.buildSingBoxOutboundEwpBean
import moe.matsuri.nb4a.proxy.ewp.parseEwp
import moe.matsuri.nb4a.proxy.ewp.toUri
import moe.matsuri.nb4a.utils.JavaUtil.gson
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.ByteString.Companion.encodeUtf8
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class XHTTPExtraTest {
    private val auth = "Digest synthetic=A+B%2F&v=1"
    private val simple = """{
        "headers":{"User-Agent":"grpc-node-js/1.14.3","Authorization":"$auth","X-Multi":["one","two"]},
        "xPaddingObfsMode":true,"xPaddingPlacement":"header","xPaddingHeader":"Auth-Token",
        "x_padding_key":"_key","xPaddingMethod":"tokenish","xPaddingBytes":{"from":64,"to":64},
        "xmux":{"maxConcurrency":"2-9","hKeepAlivePeriod":-1}
    }"""
    private val uuid = "11111111-2222-3333-4444-555555555555"
    private val publicKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    private fun link(protocol: String, extra: String? = null): String {
        val builder = "https://$uuid@127.0.0.1:24443/".toHttpUrl().newBuilder()
            .addQueryParameter("type", "xhttp").addQueryParameter("mode", "stream-up")
            .addQueryParameter("host", "example.com").addQueryParameter("sni", "example.com")
            .addQueryParameter("alpn", "h2,http/1.1").addQueryParameter("fp", "safari")
            .addQueryParameter("path", "/profile?route=up").addQueryParameter("security", "tls")
            .addQueryParameter(if (protocol == "ewp") "insecure" else "allowInsecure", "1")
        if (protocol == "ewp") builder.addQueryParameter("pk", publicKey).addQueryParameter("sid", "loopback")
        if (extra != null) builder.addQueryParameter("extra", extra)
        return builder.build().toString().replaceFirst("https://", "$protocol://")
    }

    private fun decode(uri: String): XHTTPBean = when (uri.substringBefore(':')) {
        "ewp" -> parseEwp(uri)
        "trojan" -> parseTrojan(uri)
        else -> parseV2Ray(uri)
    }.apply { initializeDefaultValues() }

    private fun share(bean: XHTTPBean) = when (bean) {
        is StandardV2RayBean -> bean.toUriVMessVLESSTrojan(bean is TrojanBean)
        is EwpBean -> bean.toUri()
        else -> error("unsupported bean")
    }

    private fun generated(bean: XHTTPBean): JsonObject {
        val outbound: Outbound = when (bean) {
            is StandardV2RayBean -> buildSingBoxOutboundStandardV2RayBean(bean)
            is EwpBean -> buildSingBoxOutboundEwpBean(bean)
            else -> error("unsupported bean")
        }
        outbound._hack_custom_config = if (bean.xhttpBrowser == true) sanitizeBrowserXHTTPOutboundJson(bean.customOutboundJson) else bean.customOutboundJson
        return gson.toJsonTree(outbound.asMap()).asJsonObject
    }

    private fun transport(bean: XHTTPBean) = generated(bean).getAsJsonObject("transport")

    // Sharing can intentionally omit TLS preferences (Browser and v2rayN).
    // Inspect the actual transport builder without loading Android's global TLS preferences.
    private fun sharedTransport(bean: XHTTPBean) = gson.toJsonTree(
        bean.buildXHTTPTransport(bean.xhttpBrowser == true && (bean is EwpBean || bean is StandardV2RayBean && bean.isVLESS)).asMap()
    ).asJsonObject

    @Test fun importedProfilesRoundTripThroughDatabaseShareAndNativeJson() {
        val fixtures = JsonObject()
        for (protocol in listOf("vless", "trojan", "vmess", "ewp")) {
            val bean = decode(link(protocol, simple))
            assertEquals("", bean.customOutboundJson)
            assertEquals("2-9", bean.xhttpXmuxMaxConcurrency)
            assertEquals(-1, bean.xhttpXmuxHKeepAlivePeriod.toInt())
            bean.name = "saved"
            bean.customConfigJson = "{\"log\":{\"level\":\"warn\"}}"
            val restored = bean.clone() as XHTTPBean
            assertEquals(bean.xhttpExtra, restored.xhttpExtra)
            assertEquals(bean.name, restored.name)
            assertEquals(bean.customConfigJson, restored.customConfigJson)
            if (bean is TrojanBean) assertEquals(bean.password, (restored as TrojanBean).password)
            val output = generated(restored)
            val wire = output.getAsJsonObject("transport")
            assertEquals("stream-up", wire["mode"].asString)
            assertEquals("64", wire["x_padding_bytes"].asString)
            assertEquals("grpc-node-js/1.14.3", wire.getAsJsonObject("headers")["User-Agent"].asString)
            assertEquals(auth, wire.getAsJsonObject("headers")["Authorization"].asString)
            assertEquals(listOf("one", "two"), wire.getAsJsonObject("headers")["X-Multi"].asJsonArray.map { it.asString })
            assertEquals(true, wire["x_padding_obfs_mode"].asBoolean)
            assertEquals("Auth-Token", wire["x_padding_header"].asString)
            assertEquals("tokenish", wire["x_padding_method"].asString)
            assertEquals("2-9", wire.getAsJsonObject("xmux")["max_concurrency"].asString)
            assertEquals(-1, wire.getAsJsonObject("xmux")["h_keep_alive_period"].asInt)
            assertFalse(wire.has("grpc_framing"))
            assertFalse(wire.has("extra"))
            val shared = decode(share(restored))
            assertEquals(wire, sharedTransport(shared))
            assertEquals(bean.xhttpExtra, shared.xhttpExtra)
            fixtures.add(protocol, output)
        }
        System.getenv("SEKAIMOD_XHTTP_FIXTURES")?.let { File(it, "profiles.json").writeText(gson.toJson(fixtures)) }
    }

    @Test fun downloadKeepInheritanceAndResetsAcrossRepresentations() {
        val root = XHTTPExtra.parse(simple)
        val down = XHTTPExtra.parse("""{
          "address":"127.0.0.1","port":24444,"network":"xhttp","security":"none","xhttpSettings":{
            "path":"/down?route=two","host":"down.test","xPaddingHeader":"ignored outer value",
            "extra":{"xPaddingBytes":0,"xPaddingObfsMode":false,"headers":{},"xmux":{},"sessionIDPlacement":"query","sessionIDKey":"download_sid"}
          }}""")
        root.add("downloadSettings", down)
        val fixtures = JsonObject()
        for (protocol in listOf("vless", "ewp")) {
            val bean = decode(link(protocol, root.toString()))
            val decoded = decode(share(bean.clone() as XHTTPBean))
            val download = transport(decoded).getAsJsonObject("download")
            assertEquals(24444, download["server_port"].asInt)
            assertEquals("/down?route=two", download["path"].asString)
            assertEquals("download_sid", download["session_key"].asString)
            assertFalse(download.getAsJsonObject("tls")["enabled"].asBoolean)
            assertFalse(download.has("x_padding_header"))
            assertEquals("0", download["x_padding_bytes"].asString)
            assertFalse(download["x_padding_obfs_mode"].asBoolean)
            assertEquals(JsonObject(), download["headers"])
            assertEquals(JsonObject(), download["xmux"])
            assertEquals(down, decoded.shareXHTTPExtra()!!["downloadSettings"])
            fixtures.add(protocol, generated(decoded))
            root.remove("downloadSettings")
            root.add("download", download)
            assertEquals(transport(decoded), transport(decode(link(protocol, root.toString()))))
            root.remove("download"); root.add("downloadSettings", down)
        }
        System.getenv("SEKAIMOD_XHTTP_FIXTURES")?.let { File(it, "downloads.json").writeText(gson.toJson(fixtures)) }
    }

    @Test fun priorSchemasAndEmptyExtrasRetainDefaults() {
        for (protocol in listOf("ewp", "vless")) {
            val old = decode(link(protocol))
            old.customOutboundJson = "{\"detour\":\"direct\"}"
            val bytes = KryoConverters.serialize(old)
            assertEquals(if (protocol == "ewp") 6 else 8, ByteBufferInput(bytes).readInt())
            val bean = old.clone() as XHTTPBean
            assertEquals("", bean.xhttpExtra)
            assertEquals(old.customOutboundJson, bean.customOutboundJson)
            assertEquals(transport(bean), transport(decode(link(protocol, "{}"))))
            assertFalse(transport(bean).has("xmux"))
            assertFalse(transport(bean).has("headers"))
            assertFalse(share(bean).contains("extra="))
            assertArrayEquals(bytes, KryoConverters.serialize(bean))
        }
    }

    @Test fun existingPreferencesRemainAuthoritativeAndExportWithoutImport() {
        val node = decode(link("vless", """{"xPaddingBytes":"100-300","xmux":{"maxConcurrency":"2-9"}}"""))
        assertEquals("", node.xhttpExtra)
        node.xhttpPaddingBytes = ""
        node.xhttpXmuxMaxConcurrency = ""
        assertFalse(transport(node).has("xmux")); assertFalse(share(node).contains("extra="))
        node.xhttpXmuxMaxConcurrency = "3-8"
        node.xhttpPaddingBytes = "64-64"
        assertEquals("64-64", transport(node)["x_padding_bytes"].asString)
        assertEquals("3-8", decode(share(node)).xhttpXmuxMaxConcurrency)
        val zero = decode(link("ewp", """{"xmux":{"h_keep_alive_period":0}}"""))
        assertEquals(0, transport(zero).getAsJsonObject("xmux")["h_keep_alive_period"].asInt)
    }

    @Test fun subscriptionRefreshReplacesNodeDataWhileCustomOverridesStillWin() {
        for (protocol in listOf("ewp", "vless")) {
            val old = decode(link(protocol, simple)).apply {
                customOutboundJson = """{"transport":{"x_padding_obfs_mode":false,"download":{"headers":{},"xmux":{}}}}"""
                customConfigJson = "{\"log\":{\"level\":\"warn\"}}"
            }
            val next = decode(link(protocol, """{"headers":{"Authorization":"rotated"},"xPaddingObfsMode":true,
              "download":{"headers":{"Authorization":"down"},"xmux":{"max_connections":2}}}""")).apply { retainSubscriptionOverrides(old) }
            val wire = transport(next)
            assertEquals(old.customConfigJson, next.customConfigJson)
            assertEquals("rotated", wire.getAsJsonObject("headers")["Authorization"].asString)
            assertFalse(wire["x_padding_obfs_mode"].asBoolean)
            assertFalse(wire.has("xmux")); assertFalse(wire.has("x_padding_header"))
            assertEquals(JsonObject(), wire.getAsJsonObject("download")["headers"])
            assertEquals(JsonObject(), wire.getAsJsonObject("download")["xmux"])
            val removed = decode(link(protocol)).apply { retainSubscriptionOverrides(next) }
            assertFalse(transport(removed).has("headers")); assertEquals("", removed.xhttpExtra)
        }
    }

    @Test fun browserKeepsImportedOptionsWithoutSendingOrdinaryOnlySettings() {
        for (protocol in listOf("vless", "ewp")) {
            val node = decode(link(protocol, simple)).apply { xhttpBrowser = true }
            node.customOutboundJson = """{"transport":{"download":{"path":"/unsupported"},"x_padding_obfs_mode":true}}"""
            val wire = transport(node)
            assertTrue(wire["browser"].asBoolean)
            assertFalse(wire.has("download")); assertFalse(wire.has("xmux")); assertFalse(wire.has("x_padding_header"))
            assertFalse(wire.has("x_padding_obfs_mode")); assertTrue(wire.has("headers"))
            val saved = decode(share(node)).apply { xhttpBrowser = false }
            assertEquals("2-9", saved.xhttpXmuxMaxConcurrency)
            assertTrue(sharedTransport(saved)["x_padding_obfs_mode"].asBoolean)
        }
    }

    @Test fun invalidExtraNeverSilentlyFallsBackOrInjectsOutboundFields() {
        for (extra in listOf("not json", "[]", """{"headers":{"X":12}}""", """{"detour":"outside"}""",
            """{"xmux":{"maxConcurrency":2147483648}}""", """{"sessionIDLength":"5-2"}""",
            """{"xPaddingBytes":7,"x_padding_bytes":8}""", """{"downloadSettings":{"network":"ws"}}""")) {
            assertThrows(XHTTPExtra.Invalid::class.java) { decode(link("vless", extra)) }
        }
        val vmess = """{"v":"2","add":"127.0.0.1","port":"443","id":"$uuid","net":"xhttp","extra":42}""".encodeUtf8().base64()
        assertThrows(XHTTPExtra.Invalid::class.java) { decode("vmess://$vmess") }
    }
}
