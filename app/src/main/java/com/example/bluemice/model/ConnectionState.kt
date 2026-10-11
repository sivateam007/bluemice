package com.example.bluemice.model

enum class ConnectionStatus {
    Disconnected,
    Scanning,
    Pairing,
    Connected
}

data class ConnectionState(
    val status: ConnectionStatus = ConnectionStatus.Disconnected,
    val deviceName: String? = null,
    val deviceAddress: String? = null
)
