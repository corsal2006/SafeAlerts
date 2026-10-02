package com.siddesh.smsalerts.data.model

data class EmergencyContact(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val relationship: String = "Emergency Contact",
    val enabled: Boolean = true,
    val isActive: Boolean = enabled,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "phoneNumber" to phoneNumber,
            "relationship" to relationship,
            "enabled" to enabled,
            "isActive" to enabled,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
}
