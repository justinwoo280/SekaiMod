package io.nekohasekai.sagernet.fmt

import io.nekohasekai.sagernet.IPv6Mode
import io.nekohasekai.sagernet.TunImplementation
import moe.matsuri.nb4a.SingBoxOptions.gsonSingbox
import org.junit.Assert.*
import org.junit.Test

class TunConfigTest {
    @Test fun persistedStackSelectionsKeepTheirMeaning() {
        // IDs stored by previous releases must survive the new menu ordering.
        for ((storedValue, expected) in mapOf(0 to "gvisor", 1 to "system", 2 to "mixed")) {
            val json = gsonSingbox.toJsonTree(buildTunInbound(storedValue, 9000, IPv6Mode.ENABLE)).asJsonObject
            assertEquals(expected, json["stack"].asString)
            assertEquals("tun", json["type"].asString)
            assertEquals("tun-in", json["tag"].asString)
        }
    }

    @Test fun switchingStacksPreservesVpnAddressesAndMtu() {
        for (ipv6Mode in listOf(IPv6Mode.DISABLE, IPv6Mode.ENABLE, IPv6Mode.ONLY)) {
            val reference = gsonSingbox.toJsonTree(buildTunInbound(TunImplementation.GVISOR, 1500, ipv6Mode)).asJsonObject
            reference.remove("stack")
            for ((value, name) in mapOf(TunImplementation.GO to "go", TunImplementation.LWIP to "lwip")) {
                val generated = gsonSingbox.toJsonTree(buildTunInbound(value, 1500, ipv6Mode)).asJsonObject
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
        val generated = gsonSingbox.toJsonTree(buildTunInbound(-1, 9000, IPv6Mode.DISABLE)).asJsonObject
        assertEquals("go", generated["stack"].asString)
    }
}
