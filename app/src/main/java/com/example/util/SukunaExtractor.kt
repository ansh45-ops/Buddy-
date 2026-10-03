package com.example.util

import java.util.Locale
import java.util.regex.Pattern
import kotlin.random.Random

enum class CursedPrefixType(val label: String, val prefix: String) {
    RANDOM_CRYPTIC("Cryptic Hex (Default)", ""),
    SUKUNA_VESSEL("Sukuna Vessel", "sukuna"),
    MALEVOLENT_SHRINE("Malevolent Shrine", "malevolent"),
    CLEAVE_DISMANTLE("Cleave & Dismantle", "cleave"),
    CURSED_ENERGY("Cursed Energy", "curse"),
    KING_OF_CURSES("King of Curses", "ryomen")
}

object SukunaExtractor {

    // Regex for 4 to 8 digit verification codes
    private val OTP_PATTERN_WITH_CONTEXT = Pattern.compile(
        """(?i)(?:code|otp|pin|verification|passcode|token|key|activation|security)[\s:=#\-]{1,5}([0-9]{4,8})\b"""
    )
    private val OTP_STANDALONE_PATTERN = Pattern.compile("""\b([0-9]{4,8})\b""")

    // Regex to match URLs
    private val URL_PATTERN = Pattern.compile("""(?i)\b(https?://[^\s"'<>]+)""")

    /**
     * Extracts an OTP or verification code from the subject and body text.
     * Prioritizes patterns with context keywords (code, verification, otp, etc.).
     */
    fun extractOtp(subject: String, body: String): String? {
        val combined = "$subject\n$body"
        
        // 1. Try contextual match first
        val contextMatcher = OTP_PATTERN_WITH_CONTEXT.matcher(combined)
        if (contextMatcher.find()) {
            val code = contextMatcher.group(1)
            if (code != null && isValidOtp(code)) {
                return code
            }
        }

        // 2. Try standalone digits in subject
        val subjMatcher = OTP_STANDALONE_PATTERN.matcher(subject)
        while (subjMatcher.find()) {
            val candidate = subjMatcher.group(1)
            if (candidate != null && isValidOtp(candidate)) {
                return candidate
            }
        }

        // 3. Try standalone in body
        val bodyMatcher = OTP_STANDALONE_PATTERN.matcher(body)
        while (bodyMatcher.find()) {
            val candidate = bodyMatcher.group(1)
            if (candidate != null && isValidOtp(candidate)) {
                return candidate
            }
        }

        return null
    }

    private fun isValidOtp(candidate: String): Boolean {
        // Exclude common years (2020-2030) unless specifically flagged
        if (candidate.length == 4) {
            val year = candidate.toIntOrNull()
            if (year in 2020..2030) return false
        }
        return true
    }

    /**
     * Extracts and cleans actionable HTTP/HTTPS links from content.
     */
    fun extractLinks(text: String): List<String> {
        val list = mutableListOf<String>()
        val matcher = URL_PATTERN.matcher(text)
        while (matcher.find()) {
            val raw = matcher.group(1) ?: continue
            val cleaned = cleanUrl(raw)
            if (cleaned.length > 12 && !list.contains(cleaned)) {
                list.add(cleaned)
            }
        }
        return list
    }

    private fun cleanUrl(url: String): String {
        var res = url
        // Strip trailing punctuation often caught in regex
        while (res.isNotEmpty() && (res.endsWith(".") || res.endsWith(",") || res.endsWith(")") || res.endsWith("]") || res.endsWith(";"))) {
            res = res.dropLast(1)
        }
        return res
    }

    /**
     * Finds the primary verification or action link if available.
     */
    fun prioritizeVerificationLink(links: List<String>): String? {
        val keywords = listOf("verify", "confirm", "activate", "validation", "token", "auth", "signin", "login")
        return links.firstOrNull { link ->
            val lower = link.lowercase(Locale.ROOT)
            keywords.any { lower.contains(it) }
        } ?: links.firstOrNull()
    }

    /**
     * Generates a Sukuna-themed or random cryptographic username.
     */
    fun generateUsername(prefixType: CursedPrefixType = CursedPrefixType.RANDOM_CRYPTIC, customPrefix: String? = null): String {
        val randomSuffix = (1..6)
            .map { "abcdefghijklmnopqrstuvwxyz0123456789".random() }
            .joinToString("")

        if (!customPrefix.isNullOrBlank()) {
            val clean = customPrefix.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
            return if (clean.isNotEmpty()) "${clean}_$randomSuffix" else randomSuffix
        }

        return when (prefixType) {
            CursedPrefixType.RANDOM_CRYPTIC -> {
                // 9-character random hex/alphanumeric just like the reference Python script
                (1..9)
                    .map { "abcdefghijklmnopqrstuvwxyz0123456789".random() }
                    .joinToString("")
            }
            else -> {
                "${prefixType.prefix}_$randomSuffix"
            }
        }
    }

    /**
     * Generates a strong random password for Mail.tm/Mail.gw.
     */
    fun generatePassword(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$"
        return (1..14).map { chars.random() }.joinToString("")
    }
}
