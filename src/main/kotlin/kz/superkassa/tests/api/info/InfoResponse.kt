package kz.superkassa.tests.api.info

data class InfoResponse(
    val name: String?,
    val version: String?,
    val mode: String?,
    val nodeId: String?,
    val ofdProtocolVersion: String?,
    val storage: Storage?,
    val statistics: Statistics?,
    val features: Features?,
) {
    data class Storage(
        val engine: String?,
        val jdbcUrl: String?,
    )

    data class Statistics(
        val registeredKkms: Int?,
    )

    data class Features(
        val allowSettingsChanges: Boolean?,
        val deliveryChannels: List<String>?,
        val ofdTimeoutSeconds: Int?,
        val ofdReconnectIntervalSeconds: Int?,
    )
}
