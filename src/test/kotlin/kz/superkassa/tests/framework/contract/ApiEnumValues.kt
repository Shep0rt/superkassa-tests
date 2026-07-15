package kz.superkassa.tests.framework.contract

object ApiEnumValues {
    val INFO_MODES = setOf("DESKTOP", "SERVER")
    val STORAGE_ENGINES = setOf("SQLITE", "POSTGRESQL", "MYSQL")
    val DELIVERY_CHANNELS = setOf("PRINT", "EMAIL", "SMS", "TELEGRAM", "WHATSAPP")

    val KKM_MODES = setOf("REGISTRATION", "PROGRAMMING")
    val KKM_STATES = setOf("IDLE", "ACTIVE", "PROGRAMMING", "BLOCKED")
    val BRANDING_LANGUAGES = setOf("RU", "KK", "MIXED")
    val BRANDING_THEME_COLORS = setOf("#1F1C2C", "#000000", "#007AFF", "#34C759", "#FF9500", "#FF3B30", "#5856D6")
    val TAX_REGIMES = setOf("NO_VAT", "VAT_PAYER", "MIXED")
    val VAT_GROUPS = setOf("NO_VAT", "VAT_0", "VAT_12", "VAT_16")
    val OFD_ENVIRONMENTS = setOf("DEV", "TEST", "PROD")
    val OFD_IDS = setOf("KAZAKHTELECOM")

    val HEALTH_OFD_STATUS_PREFIXES = setOf("OK", "DEGRADED", "SKIPPED", "ERROR")
}
