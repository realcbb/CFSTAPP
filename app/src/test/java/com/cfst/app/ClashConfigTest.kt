package com.cfst.app

import org.junit.Assert.*
import org.junit.Test

class ClashConfigTest {
    private val options = ClashOptions("00000000-0000-0000-0000-000000000000", "example.com", "example.com")

    @Test fun exportsMultipleNodesAndGroups() {
        val yaml = ClashConfig.generate(listOf("104.16.0.1", "104.16.0.2", "104.16.0.1"), options)
        assertEquals(2, Regex("    type: vless").findAll(yaml).count())
        assertTrue(yaml.contains("type: url-test"))
        assertTrue(yaml.contains("MATCH,🐟 漏网之鱼"))
        assertTrue(yaml.contains("uuid: '00000000-0000-0000-0000-000000000000'"))
    }

    @Test fun supportsIpv6AndQuotesPaths() {
        val yaml = ClashConfig.generate(listOf("2606:4700::1111"), options.copy(path = "/it's", autoSelect = false))
        assertTrue(yaml.contains("server: '2606:4700::1111'"))
        assertTrue(yaml.contains("path: '/it''s'"))
        assertFalse(yaml.contains("♻️ 自动选择"))
    }

    @Test fun validatesInputs() {
        assertNotNull(options.copy(port = "0").error())
        assertNotNull(options.copy(uuid = "invalid").error())
        assertNotNull(options.copy(path = "/\ninjected").error())
        assertTrue(runCatching { ClashConfig.generate(emptyList(), options) }.isFailure)
        assertTrue(runCatching { ClashConfig.generate(listOf("999.1.1.1"), options) }.isFailure)
    }
}
