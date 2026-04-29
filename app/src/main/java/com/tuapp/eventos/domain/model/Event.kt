package com.tuapp.eventos.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateSerializer : KSerializer<Date> {
    private val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun formatDate(date: Date): String = format.format(date)

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeString(format.format(value))
    }

    override fun deserialize(decoder: Decoder): Date {
        return try {
            format.parse(decoder.decodeString()) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
}

object TimestampSerializer : KSerializer<Date> {
    private val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Timestamp", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeString(format.format(value))
    }

    override fun deserialize(decoder: Decoder): Date {
        return try {
            format.parse(decoder.decodeString()) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
}

@Serializable
data class EventSettings(
    @SerialName("custom_roles_enabled")
    val customRolesEnabled: Boolean = true,
    @SerialName("fines_enabled")
    val finesEnabled: Boolean = false,
    @SerialName("notification_reminder_minutes")
    val notificationReminderMinutes: Int = 30
)

@Serializable
data class Participant(
    @SerialName("user_id")
    val userId: String,
    @SerialName("user_name")
    val userName: String,
    @SerialName("role_id")
    val roleId: String? = null
)

@Serializable
data class Event(
    @SerialName("id")
    val id: String? = null,
    
    @SerialName("created_by")
    val createdBy: String,
    
    @SerialName("visibility")
    val visibility: String, // "public", "private"
    
    @SerialName("name")
    val name: String,
    
    @SerialName("description")
    val description: String? = null,
    
    @SerialName("slug")
    val slug: String? = null,
    
    @Serializable(with = DateSerializer::class)
    @SerialName("start_date")
    val startDate: Date? = null,
    
    @Serializable(with = DateSerializer::class)
    @SerialName("end_date")
    val endDate: Date? = null,
    
    @Serializable(with = TimestampSerializer::class)
    @SerialName("created_at")
    val createdAt: Date? = null,
    
    @Serializable(with = TimestampSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Date? = null,
    
    @SerialName("group_id")
    val groupId: String? = null,
    
    @SerialName("settings")
    val settings: EventSettings? = EventSettings(),

    @SerialName("status")
    val status: String = "pending", // "pending", "started", "finished"
    
    // UI temporary state (Transient - not sent to DB)
    @kotlinx.serialization.Transient
    val isUserParticipating: Boolean = false
)
