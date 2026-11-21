package io.nekohasekai.sagernet.fmt

import com.esotericsoftware.kryo.io.ByteBufferInput
import com.esotericsoftware.kryo.io.ByteBufferOutput
import io.nekohasekai.sagernet.database.ProxyEntity
import io.nekohasekai.sagernet.fmt.hysteria.*
import io.nekohasekai.sagernet.fmt.ssh.SSHBean
import io.nekohasekai.sagernet.fmt.ssh.buildSingBoxOutboundSSHBean
import io.nekohasekai.sagernet.fmt.tuic.*
import io.nekohasekai.sagernet.group.RawUpdater
import kotlinx.coroutines.runBlocking
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.proxy.anytls.*
import moe.matsuri.nb4a.proxy.config.ConfigBean
import moe.matsuri.nb4a.proxy.snell.*
import moe.matsuri.nb4a.utils.JavaUtil.gson
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Protocol14Test {
    private fun snell(version: Int = 4) = SnellBean().apply {
        initializeDefaultValues()
        this.version = version
        serverAddress = "127.0.0.1"
        serverPort = 24443
        psk = "fixture-snell-password"
        userKey = "fixture-user"
        reuse = true
    }

    private fun hysteria() = HysteriaBean().apply {
        initializeDefaultValues()
        serverAddress = "127.0.0.1"
        serverPorts = "24443,25000-25002"
        authPayload = "fixture-password"
        allowInsecure = true // Avoid Android preferences in this pure generator test.
        obfsType = "gecko"
        obfuscation = "fixture-obfs"
        geckoMinPacketSize = 512
        geckoMaxPacketSize = 1200
        hopInterval = 10
        hopIntervalMax = 30
        bbrProfile = "conservative"
        disableChromeParrot = true
        brutalDebug = true
        streamReceiveWindow = 1048576
        connectionReceiveWindow = 2097152
    }

    private fun tuic() = TuicBean().apply {
        initializeDefaultValues()
        serverAddress = "127.0.0.1"
        serverPort = 24443
        uuid = "11111111-2222-3333-4444-555555555555"
        token = "fixture-password"
        allowInsecure = true
        udpRelayMode = "quic"
        udpOverStream = true
        reduceRTT = true
        heartbeat = "15s"
    }

    private fun anytls() = AnyTLSBean().apply {
        initializeDefaultValues()
        serverAddress = "127.0.0.1"
        serverPort = 24443
        password = "fixture-password"
        alpn = "h2,http/1.1"
        idleSessionCheckInterval = "15s"
        idleSessionTimeout = "2m"
        minIdleSession = 3
        clientMetadata = "SekaiMod-test"
    }

    private fun ssh() = SSHBean().apply {
        initializeDefaultValues()
        serverAddress = "127.0.0.1"
        password = "fixture-password"
        hostKeyAlgorithms = "ssh-ed25519,ecdsa-sha2-nistp256"
        clientVersion = "SSH-2.0-SekaiMod-test"
        ciphers = "aes128-gcm@openssh.com,chacha20-poly1305@openssh.com"
        macs = "hmac-sha2-256"
        kexAlgorithms = "curve25519-sha256"
    }

    @Test fun snellVersionSpecificFieldsDoNotLeakAcrossModes() {
        for (obfs in listOf("none", "http", "tls")) {
            val bean = snell().apply { obfsMode = obfs; obfsHost = "example.com"; mode = "unsafe-raw" }
            val json = buildSingBoxOutboundSnellBean(bean).asMap()
            assertEquals("snell", json["type"])
            assertEquals(4L, json["version"])
            assertEquals(bean.psk, json["psk"])
            assertEquals(bean.userKey, json["userkey"])
            assertFalse(json.containsKey("mode"))
            assertFalse(json.containsKey("tls"))
            assertEquals(if (obfs == "none") null else obfs, json["obfs_mode"])
        }
        for (mode in listOf("default", "unshaped", "unsafe-raw")) {
            val bean = snell(6).apply { this.mode = mode; obfsMode = "http"; obfsHost = "example.com" }
            val json = buildSingBoxOutboundSnellBean(bean).asMap()
            assertEquals(6L, json["version"])
            assertEquals(if (mode == "default") null else mode, json["mode"])
            assertFalse(json.containsKey("obfs_mode"))
            assertFalse(json.containsKey("obfs_host"))
        }
    }

    @Test fun snellPersistsThroughEntityAndBeanSerialization() {
        val bean = snell(6).apply { name = "Snell fixture"; mode = "unshaped"; network = "tcp" }
        val entity = ProxyEntity().putBean(bean)
        assertEquals(24, entity.type)
        assertEquals("Snell v6", entity.displayType())
        assertFalse(entity.needExternal())
        assertFalse(entity.haveStandardLink())
        assertEquals("snell", TypeMap.reversed[entity.type])
        val restored = KryoConverters.deserialize(ProxyEntity(), KryoConverters.serialize(entity))
        assertEquals(gson.toJson(bean), gson.toJson(restored.requireBean()))
        assertEquals(gson.toJson(bean), gson.toJson(bean.clone()))
    }

    @Test fun snellImportsUseTheNativeEditorAndRetainAdvancedDialerFields() = runBlocking {
        val json = """{"type":"snell","tag":"fixture","server":"127.0.0.1","server_port":24443,
            "version":6,"psk":"fixture-snell-password","mode":"unshaped","reuse":true,
            "network":["tcp","udp"],"connect_timeout":"8s"}"""
        val direct = RawUpdater.parseJSON(JSONObject(json)).single() as SnellBean
        val nested = RawUpdater.parseJSON(JSONObject("""{"outbounds":[$json]}""")).single() as SnellBean
        assertEquals(gson.toJson(direct), gson.toJson(nested))
        assertEquals("", direct.network)
        assertEquals("8s", gson.fromJson(direct.customOutboundJson, Map::class.java)["connect_timeout"])
        val exported = buildSingBoxOutboundSnellBean(direct).apply {
            _hack_custom_config = direct.customOutboundJson
        }.asMap()
        assertEquals("8s", exported["connect_timeout"])
        val clash = RawUpdater.parseRaw("""
            proxies:
              - name: fixture
                type: snell
                server: 127.0.0.1
                port: 24443
                version: 4
                psk: fixture-snell-password
                userkey: fixture-user
                reuse: true
                obfs-opts:
                  mode: tls
                  host: example.com
        """.trimIndent())!!.single() as SnellBean
        assertEquals("tls", clash.obfsMode)
        assertEquals("example.com", clash.obfsHost)
        assertEquals("fixture-user", clash.userKey)
    }

    @Test fun snellRejectsUnsupportedVersionsAndInvalidV6Keys() {
        for (version in listOf(0, 1, 2, 3, 5, 7)) {
            assertThrows(IllegalArgumentException::class.java) { buildSingBoxOutboundSnellBean(snell(version)) }
        }
        for (key in listOf("", "short", "x".repeat(256))) {
            assertThrows(IllegalArgumentException::class.java) { buildSingBoxOutboundSnellBean(snell(6).apply { psk = key }) }
        }
        // The native protocol length limit is in UTF-8 bytes, not characters.
        buildSingBoxOutboundSnellBean(snell(6).apply { psk = "测试密码测试" })
        assertThrows(IllegalArgumentException::class.java) {
            parseClashSnell(mapOf("server" to "127.0.0.1", "port" to 24443, "psk" to "fixture-password"))
        }
    }

    @Test fun anytlsSessionSettingsSurviveLinksAndSerialization() {
        val bean = anytls()
        val restored = parseAnytls(bean.toUri()).apply { initializeDefaultValues() }
        val expected = buildSingBoxOutboundAnyTLSBean(bean).asMap()
        assertEquals(expected, buildSingBoxOutboundAnyTLSBean(restored).asMap())
        assertEquals(expected, buildSingBoxOutboundAnyTLSBean(bean.clone()).asMap())
        assertEquals("15s", expected["idle_session_check_interval"])
        assertEquals("2m", expected["idle_session_timeout"])
        assertEquals(3L, expected["min_idle_session"])
        assertEquals("SekaiMod-test", expected["client_metadata"])
    }

    @Test fun tuicUdpOverStreamExcludesConflictingRelayMode() {
        val bean = tuic()
        val json = buildSingBoxOutboundTuicBean(bean).asMap()
        assertEquals(true, json["udp_over_stream"])
        assertFalse(json.containsKey("udp_relay_mode"))
        assertEquals("15s", json["heartbeat"])
        val restored = parseTuic(bean.toUri()).apply { initializeDefaultValues() }
        assertEquals(json, buildSingBoxOutboundTuicBean(restored).asMap())
        assertTrue(restored.reduceRTT)
        assertEquals(gson.toJson(bean), gson.toJson(bean.clone()))
        assertEquals("quic", buildSingBoxOutboundTuicBean(bean.apply { udpOverStream = false }).asMap()["udp_relay_mode"])
    }

    @Test fun clashDurationUnitsMatchEachProtocolsFormat() = runBlocking {
        val beans = RawUpdater.parseRaw("""
            proxies:
              - name: anytls
                type: anytls
                server: 127.0.0.1
                port: 24443
                password: fixture-password
                idle-session-check-interval: 15
                idle-session-timeout: 120
                min-idle-session: 3
              - name: tuic
                type: tuic
                server: 127.0.0.1
                port: 24443
                uuid: 11111111-2222-3333-4444-555555555555
                password: fixture-password
                heartbeat-interval: 15000
        """.trimIndent())!!
        assertEquals("15s", (beans[0] as AnyTLSBean).idleSessionCheckInterval)
        assertEquals("120s", (beans[0] as AnyTLSBean).idleSessionTimeout)
        assertEquals("15000ms", (beans[1] as TuicBean).heartbeat)
    }

    @Test fun hysteria2GeckoAndAdvancedSettingsSurviveLinks() {
        val bean = hysteria()
        val json = buildSingBoxOutboundHysteriaBean(bean).asMap()
        val obfs = json["obfs"] as Map<*, *>
        assertEquals("gecko", obfs["type"])
        assertEquals(512L, obfs["min_packet_size"])
        assertEquals(1200L, obfs["max_packet_size"])
        assertEquals("30s", json["hop_interval_max"])
        assertEquals("conservative", json["bbr_profile"])
        val restored = parseHysteria2(bean.toUri()).apply { initializeDefaultValues() }
        assertEquals(bean.obfsType, restored.obfsType)
        assertEquals(bean.geckoMaxPacketSize, restored.geckoMaxPacketSize)
        assertEquals(bean.hopIntervalMax, restored.hopIntervalMax)
        assertTrue(restored.disableChromeParrot)
        assertTrue(restored.brutalDebug)
        assertEquals(gson.toJson(bean), gson.toJson(bean.clone()))
        assertFalse(buildSingBoxOutboundHysteriaBean(bean.apply { obfsType = "salamander" }).asMap().toString().contains("min_packet_size"))
    }

    @Test fun portHoppingRetainsSinglePortsAndRanges() {
        assertEquals(listOf("443:443", "8443:8443", "9000:9010"), hopPortsToSingboxList("443, 8443, 9000-9010"))
        assertEquals(listOf("443:443", "9000:9010"), hopPortsToSingboxList("443,9000:9010"))
        assertEquals(8443, getFirstPort("8443-8445,443"))
        assertEquals(listOf("24443:24443", "25000:25002"), buildSingBoxOutboundHysteriaBean(hysteria()).asMap()["server_ports"])
        for (invalid in listOf("", "0", "65536", "5-2", "443,,80", "abc")) {
            assertThrows(RuntimeException::class.java) { hopPortsToSingboxList(invalid) }
        }
    }

    @Test fun nativeHysteria2JsonIsNotMisclassifiedAsLegacyHysteria1() {
        val source = JSONObject("""{"type":"hysteria2","tag":"native fixture","server":"127.0.0.1",
            "server_ports":["443:443","8443:8445"],"up_mbps":0,"password":"fixture-password",
            "bbr_profile":"conservative","obfs":{"type":"gecko","password":"fixture-obfs"},
            "tls":{"enabled":true}}""")
        val bean = RawUpdater.parseJSON(source).single() as ConfigBean
        assertEquals(1, bean.type)
        assertEquals("native fixture", bean.name)
        assertEquals(gson.fromJson(source.toString(), Map::class.java), gson.fromJson(bean.config, Map::class.java))
    }

    @Test fun switchingToHysteria2IgnoresLegacyPluginTransport() {
        assertTrue(hysteria().apply { protocol = HysteriaBean.PROTOCOL_FAKETCP }.canUseSingBox())
        assertFalse(hysteria().apply { protocolVersion = 1; protocol = HysteriaBean.PROTOCOL_FAKETCP }.canUseSingBox())
    }

    @Test fun sshAlgorithmListsAndClientVersionAreGenerated() {
        val bean = ssh()
        val json = buildSingBoxOutboundSSHBean(bean).asMap()
        assertEquals(listOf("ssh-ed25519", "ecdsa-sha2-nistp256"), json["host_key_algorithms"])
        assertEquals(listOf("curve25519-sha256"), json["kex_algorithm"])
        assertEquals(listOf("hmac-sha2-256"), json["mac"])
        assertEquals(bean.clientVersion, json["client_version"])
        assertEquals(json, buildSingBoxOutboundSSHBean(bean.clone()).asMap())
    }

    @Test fun oldProtocolRecordsRetainTheirNamesAndReceiveCompatibleDefaults() {
        // Write the old persisted layouts independently of the new serializers.
        val records: List<Pair<AbstractBean, (ByteBufferOutput) -> Unit>> = listOf(
            AnyTLSBean() to { o ->
                o.writeInt(1); o.writeString("127.0.0.1"); o.writeInt(443)
                for (s in listOf("password", "sni.test", "h2", "", "chrome")) o.writeString(s)
                o.writeBoolean(false); o.writeString(""); o.writeBoolean(false); o.writeString("")
            },
            TuicBean() to { o ->
                o.writeInt(3); o.writeString("127.0.0.1"); o.writeInt(443)
                for (s in listOf("password", "", "native", "cubic", "h3")) o.writeString(s)
                o.writeBoolean(false); o.writeBoolean(true); o.writeInt(1400); o.writeString("sni.test")
                o.writeBoolean(false); o.writeBoolean(false); o.writeString(""); o.writeInt(5)
                o.writeString("11111111-2222-3333-4444-555555555555")
                o.writeBoolean(false); o.writeString(""); o.writeString("")
            },
            SSHBean() to { o ->
                o.writeInt(0); o.writeString("127.0.0.1"); o.writeInt(22)
                o.writeString("user"); o.writeInt(SSHBean.AUTH_TYPE_PASSWORD)
                o.writeString("password"); o.writeString("")
            },
            HysteriaBean() to { o ->
                o.writeInt(8); o.writeString("127.0.0.1"); o.writeInt(443)
                o.writeInt(2); o.writeInt(HysteriaBean.TYPE_STRING); o.writeString("password"); o.writeInt(HysteriaBean.PROTOCOL_UDP)
                o.writeString("obfs-password"); o.writeString("sni.test"); o.writeString("h3")
                o.writeInt(0); o.writeInt(0); o.writeBoolean(false); o.writeString("")
                o.writeInt(0); o.writeInt(0); o.writeBoolean(false); o.writeInt(10)
                o.writeString("443,8443"); o.writeBoolean(false); o.writeString(""); o.writeString("")
            },
        )
        for ((bean, writeOld) in records) {
            val bytes = ByteBufferOutput(4096, -1).apply {
                writeOld(this)
                writeInt(1); writeString("legacy fixture"); writeString("{\"connect_timeout\":\"5s\"}"); writeString("")
            }.toBytes()
            bean.deserializeFromBuffer(ByteBufferInput(bytes))
            bean.initializeDefaultValues()
            assertEquals("legacy fixture", bean.name)
            assertEquals("{\"connect_timeout\":\"5s\"}", bean.customOutboundJson)
            when (bean) {
                is AnyTLSBean -> { assertEquals(0, bean.minIdleSession); assertEquals("", bean.clientMetadata) }
                is TuicBean -> { assertFalse(bean.udpOverStream); assertEquals("", bean.heartbeat); assertTrue(bean.reduceRTT) }
                is SSHBean -> { assertEquals("", bean.ciphers); assertEquals("user", bean.username) }
                is HysteriaBean -> { assertEquals("salamander", bean.obfsType); assertEquals("obfs-password", bean.obfuscation); assertEquals(0, bean.hopIntervalMax) }
            }
        }
    }

    @Test fun exportNativeConfigurationFixtures() {
        val fixtures = linkedMapOf<String, Map<String, Any>>()
        for (obfs in listOf("none", "http", "tls")) {
            fixtures["snell4-$obfs"] = buildSingBoxOutboundSnellBean(snell().apply { obfsMode = obfs }).asMap()
        }
        for (mode in listOf("default", "unshaped", "unsafe-raw")) {
            fixtures["snell6-$mode"] = buildSingBoxOutboundSnellBean(snell(6).apply { this.mode = mode }).asMap()
        }
        fixtures["anytls"] = buildSingBoxOutboundAnyTLSBean(anytls()).asMap()
        fixtures["tuic"] = buildSingBoxOutboundTuicBean(tuic()).asMap()
        fixtures["hysteria2"] = buildSingBoxOutboundHysteriaBean(hysteria()).asMap()
        fixtures["ssh"] = buildSingBoxOutboundSSHBean(ssh()).asMap()
        val imported = parseSnellOutbound("""{"type":"snell","server":"127.0.0.1","server_port":24443,"version":6,"psk":"fixture-snell-password","connect_timeout":"8s"}""")
        fixtures["snell6-import"] = buildSingBoxOutboundSnellBean(imported).apply {
            _hack_custom_config = imported.customOutboundJson
        }.asMap()
        assertEquals(11, fixtures.size)
        System.getenv("SEKAIMOD_PROTOCOL_FIXTURE")?.let { File(it).writeText(gson.toJson(fixtures)) }
    }
}
