package com.omega.devicemonitor

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceMonitorUnitTest {
    @Test
    fun passwordHashesUseUniqueSalts() {
        val first = com.omega.devicemonitor.util.SecurityHelper.hashPassword("correct horse battery staple")
        val second = com.omega.devicemonitor.util.SecurityHelper.hashPassword("correct horse battery staple")

        assertNotEquals(first, second)
        assertTrue(first.startsWith("pbkdf2_sha256\$"))
    }

    @Test
    fun passwordHashCanBeVerified() {
        val hash = com.omega.devicemonitor.util.SecurityHelper.hashPassword("correct horse battery staple")

        assertTrue(com.omega.devicemonitor.util.SecurityHelper.verifyPassword("correct horse battery staple", hash))
        assertTrue(!com.omega.devicemonitor.util.SecurityHelper.verifyPassword("incorrect password", hash))
    }
}
