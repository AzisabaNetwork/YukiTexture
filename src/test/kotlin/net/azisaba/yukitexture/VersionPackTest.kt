package net.azisaba.yukitexture

import com.google.gson.Gson
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersionPackTest {

    @Test
    fun testParseProtocolDirectNumber() {
        assertEquals(754, VersionPack.parseProtocol(754))
        assertEquals(754, VersionPack.parseProtocol("754"))
        assertNull(VersionPack.parseProtocol(null))
        assertNull(VersionPack.parseProtocol(""))
    }

    @Test
    fun testParseProtocolVersionStrings() {
        assertEquals(754, VersionPack.parseProtocol("1.16.5"))
        assertEquals(754, VersionPack.parseProtocol("v1.16.5"))
        assertEquals(754, VersionPack.parseProtocol("1.16.4"))
        assertEquals(578, VersionPack.parseProtocol("1.15.2"))
        assertEquals(340, VersionPack.parseProtocol("1.12.2"))
        assertEquals(47, VersionPack.parseProtocol("1.8.8"))
        assertEquals(765, VersionPack.parseProtocol("1.20.4"))
        assertEquals(767, VersionPack.parseProtocol("1.21"))
        assertEquals(767, VersionPack.parseProtocol("1.21.1"))
        assertEquals(767, VersionPack.parseProtocol("1.21.11"))
        assertEquals(769, VersionPack.parseProtocol("1.21.4"))
    }

    @Test
    fun testVersionPackMatches() {
        val pack1 = VersionPack(754, 754, "https://example.com/pack.zip")
        assertTrue(pack1.matches(754))
        assertFalse(pack1.matches(753))
        assertFalse(pack1.matches(755))

        val packRange = VersionPack(578, 754, "https://example.com/pack.zip")
        assertTrue(packRange.matches(578))
        assertTrue(packRange.matches(735))
        assertTrue(packRange.matches(754))
        assertFalse(packRange.matches(577))
        assertFalse(packRange.matches(755))

        val packMinOnly = VersionPack(760, null, "https://example.com/pack.zip")
        assertTrue(packMinOnly.matches(760))
        assertTrue(packMinOnly.matches(769))
        assertFalse(packMinOnly.matches(759))
    }

    @Test
    fun testPlayerDataDeserialization() {
        val gson = Gson()
        val jsonWithProtocol = """{"uuid":"00000000-0000-0000-0000-000000000001","username":"TestUser","childServer":"lobby","protocolVersion":765}"""
        val data1 = gson.fromJson(jsonWithProtocol, PlayerData::class.java)
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), data1.uuid)
        assertEquals("TestUser", data1.username)
        assertEquals("lobby", data1.childServer)
        assertEquals(765, data1.protocolVersion)

        val jsonWithoutProtocol = """{"uuid":"00000000-0000-0000-0000-000000000002","username":"TestUser2","childServer":"lobby"}"""
        val data2 = gson.fromJson(jsonWithoutProtocol, PlayerData::class.java)
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), data2.uuid)
        assertNull(data2.protocolVersion)
    }

    @Test
    fun testJedisBoxCreation() {
        val client = JedisBox.createClient("localhost", 6379, timeoutMillis = 100)
        client.close()
    }
}
