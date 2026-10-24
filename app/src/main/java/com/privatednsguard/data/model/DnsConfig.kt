package com.privatednsguard.data.model

enum class DnsMode(val systemValue: String, val displayName: String) {
    OFF("off", "Off"),
    OPPORTUNISTIC("opportunistic", "Automatic"),
    HOSTNAME("hostname", "Strict Private DNS");

    companion object {
        fun fromSystemValue(value: String?): DnsMode {
            return entries.firstOrNull { it.systemValue.equals(value, ignoreCase = true) }
                ?: OPPORTUNISTIC
        }
    }
}

enum class GuardStatus {
    PROTECTED,
    UNPROTECTED,
    SNOOZED,
    TAMPER_DETECTED,
    PERMISSION_REQUIRED
}

data class DnsProviderPreset(
    val id: String,
    val name: String,
    val hostname: String,
    val description: String,
    val category: String,
    val isCustomTemplate: Boolean = false
) {
    companion object {
        val PRESETS = listOf(
            DnsProviderPreset(
                id = "nextdns",
                name = "NextDNS",
                hostname = "dns.nextdns.io",
                description = "Customizable cloud firewall, ad blocking, and parental control",
                category = "Custom & Privacy",
                isCustomTemplate = true
            ),
            DnsProviderPreset(
                id = "quad9",
                name = "Quad9",
                hostname = "dns.quad9.net",
                description = "Blocks malicious domains, phishing, and spyware (Non-profit)",
                category = "Security"
            ),
            DnsProviderPreset(
                id = "cloudflare",
                name = "Cloudflare (1.1.1.1)",
                hostname = "one.one.one.one",
                description = "Fast, privacy-first DNS by Cloudflare",
                category = "General & Speed"
            ),
            DnsProviderPreset(
                id = "cloudflare_security",
                name = "Cloudflare Security",
                hostname = "security.cloudflare-dns.com",
                description = "Automatically blocks known malware and phishing attacks",
                category = "Security"
            ),
            DnsProviderPreset(
                id = "cloudflare_family",
                name = "Cloudflare Family",
                hostname = "family.cloudflare-dns.com",
                description = "Blocks malware and adult/explicit content",
                category = "Family Filter"
            ),
            DnsProviderPreset(
                id = "adguard",
                name = "AdGuard DNS",
                hostname = "dns.adguard-dns.com",
                description = "Blocks ads, trackers, and malicious websites",
                category = "Ad Blocking"
            ),
            DnsProviderPreset(
                id = "mullvad",
                name = "Mullvad DNS",
                hostname = "dns.mullvad.net",
                description = "Zero-logging privacy DNS from Mullvad VPN",
                category = "Privacy"
            ),
            DnsProviderPreset(
                id = "cleanbrowsing",
                name = "CleanBrowsing Family",
                hostname = "family-filter-dns.cleanbrowsing.org",
                description = "Strict family-safe DNS blocking adult content and enforcing SafeSearch",
                category = "Family Filter"
            )
        )
    }
}

data class DnsPolicyState(
    val isGuardEnabled: Boolean = false,
    val targetMode: DnsMode = DnsMode.HOSTNAME,
    val targetSpecifier: String = "dns.nextdns.io",
    val activeMode: DnsMode = DnsMode.OPPORTUNISTIC,
    val activeSpecifier: String = "",
    val hasSecureSettingsPermission: Boolean = false,
    val isShizukuAvailable: Boolean = false,
    val isShizukuGranted: Boolean = false,
    val isDeviceAdminActive: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val snoozeRemainingSeconds: Long = 0L,
    val blockedTamperCount: Int = 0
) {
    val isGuardingMatch: Boolean
        get() = isGuardEnabled &&
                activeMode == targetMode &&
                (targetMode != DnsMode.HOSTNAME || activeSpecifier.equals(targetSpecifier, ignoreCase = true))

    val status: GuardStatus
        get() = when {
            !hasSecureSettingsPermission -> GuardStatus.PERMISSION_REQUIRED
            snoozeRemainingSeconds > 0 -> GuardStatus.SNOOZED
            !isGuardEnabled -> GuardStatus.UNPROTECTED
            !isGuardingMatch -> GuardStatus.TAMPER_DETECTED
            else -> GuardStatus.PROTECTED
        }
}
