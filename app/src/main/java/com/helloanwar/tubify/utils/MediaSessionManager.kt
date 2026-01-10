package com.helloanwar.tubify.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.helloanwar.tubify.MainActivity
import com.helloanwar.tubify.R

class MediaSessionManager(
    private val context: Context,
    private val isService: Boolean = false,
    private val onPlayCallback: () -> Unit,
    private val onPauseCallback: () -> Unit,
    private val onNextCallback: () -> Unit,
    private val onPreviousCallback: () -> Unit,
    private val onSeekToCallback: (Long) -> Unit
) {
    private val mediaSession: MediaSessionCompat = MediaSessionCompat(context, "TubifyMediaSession")

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "media_playback_channel"

    private val notificationReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PLAY -> mediaSession.controller.transportControls.play()
                ACTION_PAUSE -> mediaSession.controller.transportControls.pause()
                ACTION_NEXT -> mediaSession.controller.transportControls.skipToNext()
                ACTION_PREV -> mediaSession.controller.transportControls.skipToPrevious()
                ACTION_STOP -> {
                    mediaSession.controller.transportControls.stop()
                    notificationManager.cancel(NOTIFICATION_ID)
                }
            }
        }
    }

    init {
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                onPlayCallback()
                updateState(isPlaying = true)
            }

            override fun onPause() {
                onPauseCallback()
                updateState(isPlaying = false)
            }

            override fun onSkipToNext() {
                onNextCallback()
            }

            override fun onSkipToPrevious() {
                onPreviousCallback()
            }

            override fun onSeekTo(pos: Long) {
                onSeekToCallback(pos)
                val isPlaying = mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING
                updateState(isPlaying, pos)
            }
        })

        createNotificationChannel()
        val filter = android.content.IntentFilter().apply {
            addAction(ACTION_PLAY)
            addAction(ACTION_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREV)
            addAction(ACTION_STOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(notificationReceiver, filter)
        }
    }

    fun startSession() {
        mediaSession.isActive = true
    }

    fun release() {
        mediaSession.release()
        notificationManager.cancel(NOTIFICATION_ID)
        try {
            context.unregisterReceiver(notificationReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }

    fun updateState(isPlaying: Boolean, position: Long = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN) {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SEEK_TO

        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, position, 1f)
                .build()
        )
        
        showNotification(isPlaying)
    }

    fun updateMetadata(title: String, duration: Long = 0) {
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .build()
        )
        // Refresh notification with new metadata
        showNotification(mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING)
    }

    private fun showNotification(isPlaying: Boolean) {
        val controller = mediaSession.controller
        val mediaMetadata = controller.metadata
        val description = mediaMetadata?.description

        // Helper to create pending intents for our custom actions
        fun createActionIntent(action: String): PendingIntent {
            val intent = Intent(action).setPackage(context.packageName)
            return PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(description?.title ?: "Tubify")
            .setContentText(description?.subtitle)
            .setSubText(description?.description)
            .setLargeIcon(description?.iconBitmap)
            .setContentIntent(controller.sessionActivity)
            .setDeleteIntent(createActionIntent(ACTION_STOP))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    // connecting to media session allows the system to use "Media" controls appearance
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        // Previous Action
        builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_media_previous,
                "Previous",
                createActionIntent(ACTION_PREV)
            )
        )

        // Play/Pause Action
        if (isPlaying) {
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_pause,
                    "Pause",
                    createActionIntent(ACTION_PAUSE)
                )
            )
        } else {
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_play,
                    "Play",
                    createActionIntent(ACTION_PLAY)
                )
            )
        }

        // Next Action
        builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_media_next,
                "Next",
                createActionIntent(ACTION_NEXT)
            )
        )
        
        // Open playing activity info
        val resultIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)
        
        val notification = builder.build()
        if (isService && context is android.app.Service) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                context.startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Media Playback"
            val descriptionText = "Media playback controls"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_ID = 101
        const val ACTION_PLAY = "com.helloanwar.tubify.ACTION_PLAY"
        const val ACTION_PAUSE = "com.helloanwar.tubify.ACTION_PAUSE"
        const val ACTION_NEXT = "com.helloanwar.tubify.ACTION_NEXT"
        const val ACTION_PREV = "com.helloanwar.tubify.ACTION_PREV"
        const val ACTION_STOP = "com.helloanwar.tubify.ACTION_STOP"
    }
}
