package com.indiza.cotisation.users

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.text.format.DateUtils
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.indiza.cotisation.MainActivity
import com.indiza.cotisation.network.PushTokenRegistrar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class CotisationMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var pushTokenRegistrar: PushTokenRegistrar

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        Log.i(TAG, "A new FCM token was received; registering this device.")
        serviceScope.launch {
            runCatching { pushTokenRegistrar.registerToken(token) }
                .onSuccess { registered ->
                    Log.i(
                        TAG,
                        if (registered) "FCM token registration completed."
                        else "FCM token was not registered because no user is signed in."
                    )
                }
                .onFailure { Log.w(TAG, "Unable to register refreshed FCM token.", it) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        Log.i(TAG, "==================================================")
        Log.i(TAG, ">>> INCOMING FCM MESSAGE RECEIVED! id=${message.messageId}")
        Log.i(TAG, "Data payload (${data.size} keys): $data")
        Log.i(TAG, "Notification payload: title='${message.notification?.title}', body='${message.notification?.body}'")

        val title = data["title"] ?: message.notification?.title ?: run {
            Log.e(TAG, "FCM MESSAGE DROPPED: Neither data['title'] nor message.notification.title exists!")
            return
        }
        val body = data["body"] ?: message.notification?.body.orEmpty()
        val notificationType = data["type"].orEmpty()
        val notificationId = data["notification_id"] ?: message.messageId
            ?: System.currentTimeMillis().toString()
        val groupKey = data["group_key"]?.takeIf(String::isNotBlank)
            ?: "event:$notificationId"
        val actorId = data["actor_id"]?.takeIf(String::isNotBlank)
            ?: notificationId
        val actorName = data["actor_name"]?.takeIf(String::isNotBlank)
            ?: title
        val contextName = data["context_name"]?.takeIf(String::isNotBlank)
            .orEmpty()
        val timestamp = data["created_at"]?.toLongOrNull()
            ?.takeIf { it >= MIN_TIMESTAMP_MILLIS }
            ?: System.currentTimeMillis()

        Log.i(TAG, "Parsed message -> Title: '$title', Body: '$body', Type: '$notificationType', GroupKey: '$groupKey'")

        createNotificationChannel()

        // 1. Permission Check (Android 13+)
        val hasPostNotifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        Log.i(TAG, "Check 1/3 - POST_NOTIFICATIONS Permission: ${if (hasPostNotifPermission) "GRANTED ✅" else "NOT GRANTED ❌"}")
        if (!hasPostNotifPermission) {
            Log.e(TAG, "CRITICAL DROP: POST_NOTIFICATIONS permission is NOT granted on Android 13+!")
            return
        }

        // 2. System Notifications Enabled Check
        val notificationManager = NotificationManagerCompat.from(this)
        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
        Log.i(TAG, "Check 2/3 - System Notifications Enabled: ${if (areNotificationsEnabled) "ENABLED ✅" else "DISABLED ❌"}")
        if (!areNotificationsEnabled) {
            Log.e(TAG, "CRITICAL DROP: Notifications are disabled in Android Settings for this app!")
            return
        }

        // 3. Channel Importance Check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = getSystemService(NotificationManager::class.java)?.getNotificationChannel(CHANNEL_ID)
            val channelImportance = channel?.importance ?: NotificationManager.IMPORTANCE_NONE
            Log.i(TAG, "Check 3/3 - Channel '$CHANNEL_ID' Importance: $channelImportance")
            if (channelImportance == NotificationManager.IMPORTANCE_NONE) {
                Log.e(TAG, "CRITICAL DROP: Notification channel '$CHANNEL_ID' is BLOCKED by user/system!")
                return
            }
        }

        if (groupKey.startsWith("chat:") || groupKey.startsWith("join_request:")) {
            showGroupedNotification(
                groupKey = groupKey,
                actorId = actorId,
                actorName = actorName,
                contextName = contextName,
                notificationType = notificationType,
                fallbackTitle = title,
                fallbackBody = body,
                timestamp = timestamp
            )
        } else {
            showSingleNotification(
                notificationId,
                title,
                body,
                data["reference_id"],
                notificationType
            )
        }
        Log.i(TAG, ">>> SUCCESS: Notification posted to system status bar for type='$notificationType'.")
        Log.i(TAG, "==================================================")
    }

    private fun showGroupedNotification(
        groupKey: String,
        actorId: String,
        actorName: String,
        contextName: String,
        notificationType: String,
        fallbackTitle: String,
        fallbackBody: String,
        timestamp: Long
    ) {
        val preferences = getSharedPreferences(GROUPING_PREFERENCES, MODE_PRIVATE)
        val encodedGroup = encodeKey(groupKey)
        val groupPrefix = "group_${encodedGroup}_actor_"
        val actorPrefix = "$groupPrefix${encodeKey(actorId)}_"
        val lastKey = "${actorPrefix}last"
        val previousTimestamp = preferences.getLong(lastKey, 0L)
        val isSuccessive = previousTimestamp > 0L &&
            abs(timestamp - previousTimestamp) <= GROUPING_WINDOW_MILLIS
        val countKey = "${actorPrefix}count"
        val count = if (isSuccessive) preferences.getInt(countKey, 0) + 1 else 1
        val latestTimestamp = if (isSuccessive) maxOf(previousTimestamp, timestamp) else timestamp

        preferences.edit()
            .putInt(countKey, count)
            .putLong(lastKey, latestTimestamp)
            .putString("${actorPrefix}name", actorName)
            .apply()

        val now = System.currentTimeMillis()
        val actors = preferences.all.keys
            .asSequence()
            .filter { it.startsWith(groupPrefix) && it.endsWith("_last") }
            .mapNotNull { key ->
                val prefix = key.removeSuffix("last")
                val lastMessageAt = preferences.getLong(key, 0L)
                if (now - lastMessageAt > GROUPING_WINDOW_MILLIS &&
                    !key.startsWith(actorPrefix)
                ) {
                    return@mapNotNull null
                }
                GroupedActor(
                    id = prefix.removePrefix(groupPrefix).removeSuffix("_"),
                    name = preferences.getString("${prefix}name", null) ?: "Un membre",
                    count = preferences.getInt("${prefix}count", 1),
                    lastMessageAt = lastMessageAt
                )
            }
            .sortedByDescending(GroupedActor::lastMessageAt)
            .toList()

        val childTitle = when {
            notificationType == CHAT_MESSAGE && count > 1 ->
                "$actorName a envoyé $count messages"
            notificationType == CHAT_MESSAGE -> "$actorName a envoyé un message"
            notificationType == JOIN_REQUEST ->
                "$actorName souhaite rejoindre"
            else -> fallbackTitle
        }
        val childBody = when {
            contextName.isBlank() -> fallbackBody
            notificationType == JOIN_REQUEST -> "$contextName · ${relativeTime(latestTimestamp)}"
            notificationType == CHAT_MESSAGE -> "Dans $contextName · ${relativeTime(latestTimestamp)}"
            else -> fallbackBody
        }
        val groupId = stableNotificationId(groupKey)
        val actorNotificationId = stableNotificationId("$groupKey:$actorId")
        val manager = NotificationManagerCompat.from(this)

        manager.notify(
            actorNotificationId,
            buildNotification(
                groupKey = groupKey,
                title = childTitle,
                body = childBody,
                timestamp = latestTimestamp,
                isGroupSummary = false,
                referenceId = null
            )
        )

        val activeCount = actors.sumOf(GroupedActor::count)
        val summaryTitle: String
        val summaryBody: String
        when {
            notificationType == CHAT_MESSAGE && actors.size == 1 -> {
                summaryTitle = childTitle
                summaryBody = childBody
            }
            notificationType == CHAT_MESSAGE -> {
                summaryTitle = "Nouveaux messages"
                summaryBody =
                    "$activeCount messages dans ${contextName.ifBlank { "la réunion" }} · " +
                        relativeTime(latestTimestamp)
            }
            notificationType == JOIN_REQUEST && actors.size == 1 -> {
                summaryTitle = childTitle
                summaryBody = "$contextName · ${relativeTime(latestTimestamp)}"
            }
            notificationType == JOIN_REQUEST -> {
                summaryTitle = "${actors.size} demandes d’adhésion"
                val names = actors.take(MAX_NAMES_IN_SUMMARY).joinToString(", ") { it.name }
                summaryBody = (if (actors.size <= MAX_NAMES_IN_SUMMARY) {
                    "$names souhaitent rejoindre $contextName"
                } else {
                    "${actors.size} personnes souhaitent rejoindre $contextName"
                }) + " · ${relativeTime(latestTimestamp)}"
            }
            else -> {
                summaryTitle = fallbackTitle
                summaryBody = fallbackBody
            }
        }
        val inboxStyle = NotificationCompat.InboxStyle()
        actors.take(MAX_NAMES_IN_SUMMARY).forEach { actor ->
            val line = if (notificationType == CHAT_MESSAGE) {
                "${actor.name} · ${actor.count} message(s) · ${relativeTime(actor.lastMessageAt)}"
            } else {
                "${actor.name} souhaite rejoindre · ${relativeTime(actor.lastMessageAt)}"
            }
            inboxStyle.addLine(line)
        }

        manager.notify(
            groupId,
            buildNotification(
                groupKey = groupKey,
                title = summaryTitle,
                body = summaryBody,
                timestamp = latestTimestamp,
                isGroupSummary = true,
                referenceId = null,
                style = inboxStyle
            )
        )
    }

    private fun showSingleNotification(
        notificationId: String,
        title: String,
        body: String,
        referenceId: String?,
        notificationType: String
    ) {
        val targetId = stableNotificationId(notificationId)
        Log.i(TAG, "Posting Single Notification -> Target System ID: $targetId, Title: '$title'")
        NotificationManagerCompat.from(this).notify(
            targetId,
            buildNotification(
                groupKey = null,
                title = title,
                body = body,
                timestamp = System.currentTimeMillis(),
                isGroupSummary = false,
                referenceId = referenceId,
                notificationType = notificationType
            )
        )
    }

    private fun buildNotification(
        groupKey: String?,
        title: String,
        body: String,
        timestamp: Long,
        isGroupSummary: Boolean,
        referenceId: String?,
        notificationType: String? = null,
        style: NotificationCompat.Style? = null
    ): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            referenceId?.let { putExtra(EXTRA_NOTIFICATION_REFERENCE_ID, it) }
            notificationType?.let { putExtra(EXTRA_NOTIFICATION_TYPE, it) }
        }
        val notificationId = stableNotificationId("$title:$timestamp")
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.indiza.cotisation.R.mipmap.logocaisses)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(style ?: NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setWhen(timestamp)
            .setShowWhen(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply {
                if (groupKey != null) {
                    setGroup(groupKey)
                    setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                    setGroupSummary(isGroupSummary)
                }
            }
            .build()
    }

    private fun relativeTime(timestamp: Long): String =
        DateUtils.getRelativeTimeSpanString(
            timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()

    private fun encodeKey(value: String): String =
        Base64.encodeToString(
            value.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

    private fun stableNotificationId(value: String): Int =
        value.hashCode().and(Int.MAX_VALUE).coerceAtLeast(1)

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notifications Cotisation",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            Log.i(TAG, "Notification channel '$CHANNEL_ID' verified/created with IMPORTANCE_HIGH.")
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private data class GroupedActor(
        val id: String,
        val name: String,
        val count: Int,
        val lastMessageAt: Long
    )

    private companion object {
        const val TAG = "CotisationPush"
        const val CHANNEL_ID = "cotisation_push_v2"
        const val GROUPING_PREFERENCES = "cotisation_notification_groups"
        const val GROUPING_WINDOW_MILLIS = 5 * 60 * 1000L
        const val MAX_NAMES_IN_SUMMARY = 3
        const val MIN_TIMESTAMP_MILLIS = 1_000_000_000_000L
        const val CHAT_MESSAGE = "CHAT_MESSAGE"
        const val JOIN_REQUEST = "JOIN_REQUEST"
        const val EXTRA_NOTIFICATION_REFERENCE_ID = "notification_reference_id"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
    }
}
