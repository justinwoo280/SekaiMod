package io.nekohasekai.sagernet.fmt

import io.nekohasekai.sagernet.IPv6Mode
import io.nekohasekai.sagernet.TunImplementation
import moe.matsuri.nb4a.utils.JavaUtil.gson
import org.junit.Assert.*
import org.junit.Test

class TunConfigTest {
    private fun tunJson(implementation: Int, mtu: Int, ipv6Mode: Int) =
        gson.toJsonTree(buildTunInbound(implementation, mtu, ipv6Mode).asMap()).asJsonObject

    @Test fun persistedStackSelectionsKeepTheirMeaning() {
        // IDs stored by previous releases must survive the new menu ordering.
        for ((storedValue, expected) in mapOf(0 to "gvisor", 1 to "system", 2 to "mixed")) {
            val json = tunJson(storedValue, 9000, IPv6Mode.ENABLE)
            assertEquals(expected, json["stack"].asString)
            assertEquals("tun", json["type"].asString)
            assertEquals("tun-in", json["tag"].asString)
        }
    }

    @Test fun switchingStacksPreservesVpnAddressesAndMtu() {
        for (ipv6Mode in listOf(IPv6Mode.DISABLE, IPv6Mode.ENABLE, IPv6Mode.ONLY)) {
            val reference = tunJson(TunImplementation.GVISOR, 1500, ipv6Mode)
            reference.remove("stack")
            for ((value, name) in mapOf(TunImplementation.GO to "go", TunImplementation.LWIP to "lwip")) {
                val generated = tunJson(value, 1500, ipv6Mode)
                assertEquals(name, generated.remove("stack").asString)
                assertEquals(reference, generated)
                val addresses = generated.getAsJsonArray("address").map { it.asString }
                assertEquals(ipv6Mode != IPv6Mode.ONLY, addresses.any { it.contains('.') })
                assertEquals(ipv6Mode != IPv6Mode.DISABLE, addresses.any { it.contains(':') })
                assertFalse(generated.has("inet4_address"))
                assertFalse(generated.has("endpoint_independent_nat"))
                assertFalse(generated.has("sniff"))
                assertFalse(generated.has("domain_strategy"))
            }
        }
    }

    @Test fun unknownSelectionUsesTheCoreDefault() {
        val generated = tunJson(-1, 9000, IPv6Mode.DISABLE)
        assertEquals("go", generated["stack"].asString)
    }
}
