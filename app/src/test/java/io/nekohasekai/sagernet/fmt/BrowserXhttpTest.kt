package io.nekohasekai.sagernet.fmt

import io.nekohasekai.sagernet.fmt.v2ray.VMessBean
import io.nekohasekai.sagernet.fmt.v2ray.buildSingBoxOutboundStandardV2RayBean
import io.nekohasekai.sagernet.fmt.v2ray.parseDuckSoft
import io.nekohasekai.sagernet.fmt.v2ray.toUriVMessVLESSTrojan
import moe.matsuri.nb4a.SingBoxOptions.OutboundTLSOptions
import moe.matsuri.nb4a.SingBoxOptions.Outbound_VLESSOptions
import moe.matsuri.nb4a.SingBoxOptions.V2RayTransportOptions_XHTTPOptions
import moe.matsuri.nb4a.proxy.ewp.EwpBean
import moe.matsuri.nb4a.proxy.ewp.buildSingBoxOutboundEwpBean
import moe.matsuri.nb4a.proxy.ewp.parseEwp
import moe.matsuri.nb4a.proxy.ewp.toUri
import moe.matsuri.nb4a.utils.JavaUtil.gson
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class BrowserXhttpTest {
    private fun vless() = VMessBean().apply {
        alterId = -1
        initializeDefaultValues()
        type = "xhttp"
        security = "tls"
        xhttpBrowser = true
        xhttpXmuxMaxConcurrency = "4-8"
        allowInsecure = true
        utlsFingerprint = "firefox"
        alpn = "h3"
        sni = "inner.example.com"
        uuid = "11111111-2222-3333-4444-555555555555"
    }

    private fun ewp() = EwpBean().apply {
        initializeDefaultValues()
        type = "xhttp"
        xhttpBrowser = true
        xhttpXmuxMaxConcurrency = "4-8"
        allowInsecure = true
        utlsFingerprint = "firefox"
        alpn = "h3"
        tlsFragment = true
        tlsRecordFragment = true
        sni = "inner.example.com"
        uuid = "11111111-2222-3333-4444-555555555555"
        serverPublicKey = "test-server-key"
        serverId = "test-server"
        routeEpoch = 7
    }

    private fun assertBrowserTls(tls: OutboundTLSOptions) {
        assertEquals(false, tls.insecure)
        assertNull(tls.utls)
        assertNull(tls.alpn)
        assertNull(tls.fragment)
        assertNull(tls.record_fragment)
        assertEquals("inner.example.com", tls.server_name)
    }

    @Test fun savedProfilesRetainBrowserAndAdjacentFields() {
        val beans = listOf(vless(), ewp())
        for (bean in beans) {
            bean.name = "Browser profile"
            bean.customOutboundJson = "{\"detour\":\"direct\"}"
            val restored = bean.clone()
            assertEquals(gson.toJson(bean), gson.toJson(restored))
        }
        assertTrue(vless().clone().xhttpBrowser)
        assertTrue(ewp().clone().xhttpBrowser)
        assertFalse(VMessBean().apply { initializeDefaultValues() }.xhttpBrowser)
        assertFalse(EwpBean().apply { initializeDefaultValues() }.xhttpBrowser)
    }

    @Test fun browserNormalizesModesAndDropsIncompatibleSettings() {
        for (mode in listOf("auto", "stream-one", "packet-up", "stream-up")) {
            val standard = buildSingBoxOutboundStandardV2RayBean(vless().apply {
                xhttpMode = mode
            }) as Outbound_VLESSOptions
            val ewp = buildSingBoxOutboundEwpBean(ewp().apply { xhttpMode = mode })
            for (transport in listOf(standard.transport, ewp.transport)) {
                transport as V2RayTransportOptions_XHTTPOptions
                assertTrue(transport.browser)
                assertEquals(if (mode == "stream-up") mode else "packet-up", transport.mode)
                assertNull(transport.xmux)
            }
            assertBrowserTls(standard.tls)
            assertBrowserTls(ewp.tls)
        }
    }

    @Test fun disablingBrowserPreservesOrdinaryXhttpSettings() {
        val standard = buildSingBoxOutboundStandardV2RayBean(vless().apply {
            xhttpBrowser = false
            xhttpMode = "stream-one"
        }) as Outbound_VLESSOptions
        val ewp = buildSingBoxOutboundEwpBean(ewp().apply {
            xhttpBrowser = false
            xhttpMode = "stream-one"
        })
        for (transport in listOf(standard.transport, ewp.transport)) {
            transport as V2RayTransportOptions_XHTTPOptions
            assertFalse(transport.browser)
            assertEquals("stream-one", transport.mode)
            assertEquals("4-8", transport.xmux.max_concurrency)
        }
        for (tls in listOf(standard.tls, ewp.tls)) {
            assertTrue(tls.insecure)
            assertEquals("firefox", tls.utls.fingerprint)
            assertEquals(listOf("h3"), tls.alpn)
        }
        assertTrue(ewp.tls.fragment)
        assertTrue(ewp.tls.record_fragment)
    }

    @Test fun browserRealityExcludesEchAndCustomCertificates() {
        val standard = buildSingBoxOutboundStandardV2RayBean(vless().apply {
            realityPubKey = "test-reality-key"
            realityShortId = "abcd"
            enableECH = true
            certificates = "custom CA"
        }) as Outbound_VLESSOptions
        val ewp = buildSingBoxOutboundEwpBean(ewp().apply {
            realityPubKey = "test-reality-key"
            realityShortId = "abcd"
            enableECH = true
            certificates = "custom CA"
        })
        for (tls in listOf(standard.tls, ewp.tls)) {
            assertBrowserTls(tls)
            assertEquals("test-reality-key", tls.reality.public_key)
            assertEquals("abcd", tls.reality.short_id)
            assertNull(tls.ech)
            assertNull(tls.certificate)
        }
    }

    @Test fun browserEchKeepsLookupNameAndNativeAlpn() {
        val standard = buildSingBoxOutboundStandardV2RayBean(vless().apply {
            enableECH = true
            echQueryServerName = "ech.example.com"
            alpn = "h2,http/1.1"
        }) as Outbound_VLESSOptions
        val ewp = buildSingBoxOutboundEwpBean(ewp().apply {
            enableECH = true
            echQueryServerName = "ech.example.com"
            alpn = "h2,http/1.1"
        })
        for (tls in listOf(standard.tls, ewp.tls)) {
            assertTrue(tls.ech.enabled)
            assertEquals("ech.example.com", tls.ech.query_server_name)
            assertEquals(listOf("h2", "http/1.1"), tls.alpn)
            assertNull(tls.reality)
        }
    }

    @Test fun shareLinksRetainBrowserModeAndReality() {
        val standard = vless().apply { xhttpMode = "stream-up" }
        val link = standard.toUriVMessVLESSTrojan(false).replace("vless://", "https://").toHttpUrl()
        val imported = VMessBean().apply {
            alterId = -1
            parseDuckSoft(link)
            initializeDefaultValues()
        }
        assertTrue(imported.xhttpBrowser)
        assertEquals("stream-up", imported.xhttpMode)
        assertNull(link.queryParameter("fp"))
        assertNull(link.queryParameter("allowInsecure"))

        val ewp = ewp().apply {
            realityPubKey = "test-reality-key"
            realityShortId = "abcd"
        }
        val importedEwp = parseEwp(ewp.toUri()).apply { initializeDefaultValues() }
        assertTrue(importedEwp.xhttpBrowser)
        assertEquals("packet-up", importedEwp.xhttpMode)
        assertEquals(ewp.realityPubKey, importedEwp.realityPubKey)
        assertEquals(ewp.realityShortId, importedEwp.realityShortId)
        assertEquals(ewp.serverPublicKey, importedEwp.serverPublicKey)
        assertEquals(ewp.routeEpoch, importedEwp.routeEpoch)
    }

    @Test fun customOverridesCannotUndoBrowserSettings() {
        val outbound = buildSingBoxOutboundStandardV2RayBean(vless()) as Outbound_VLESSOptions
        outbound._hack_custom_config = sanitizeBrowserXHTTPOutboundJson("""
            {"detour":"direct", "transport":{"browser":false,"mode":"stream-one",
              "xmux":{"max_concurrency":"8-16"},"path":"/custom"},
             "tls":{"insecure":true,"utls":{"enabled":true,"fingerprint":"firefox"},
              "alpn":["h3"],"server_name":"custom.example.com"}}
        """.trimIndent())
        val json = gson.toJsonTree(outbound.asMap()).asJsonObject
        val transport = json.getAsJsonObject("transport")
        assertTrue(transport["browser"].asBoolean)
        assertEquals("packet-up", transport["mode"].asString)
        assertFalse(transport.has("xmux"))
        assertEquals("/custom", transport["path"].asString)
        val tls = json.getAsJsonObject("tls")
        assertFalse(tls["insecure"].asBoolean)
        assertFalse(tls.has("utls"))
        assertFalse(tls.has("alpn"))
        assertEquals("custom.example.com", tls["server_name"].asString)
        assertEquals("direct", json["detour"].asString)
    }
}
