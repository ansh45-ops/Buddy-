package com.example

import com.example.util.CursedPrefixType
import com.example.util.SukunaExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testExtractOtpWithContext() {
        val subject = "Your verification code is 482910"
        val body = "Please enter this key within 10 minutes to verify your account."
        val otp = SukunaExtractor.extractOtp(subject, body)
        assertEquals("482910", otp)
    }

    @Test
    fun testExtractOtpFromBodyOnly() {
        val subject = "Welcome to our platform!"
        val body = "Here is your OTP code: 8931. Do not share it with anyone."
        val otp = SukunaExtractor.extractOtp(subject, body)
        assertEquals("8931", otp)
    }

    @Test
    fun testExtractLinks() {
        val body = "Click here to activate your account: https://service.domain.com/auth/verify?token=abc123xyz. Or visit https://domain.com/login."
        val links = SukunaExtractor.extractLinks(body)
        assertEquals(2, links.size)
        assertTrue(links[0].contains("verify"))
    }

    @Test
    fun testGenerateCursedAlias() {
        val alias = SukunaExtractor.generateUsername(CursedPrefixType.SUKUNA_VESSEL)
        assertTrue(alias.startsWith("sukuna_"))
        assertTrue(alias.length > 8)
    }
}
