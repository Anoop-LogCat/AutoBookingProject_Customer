package production.logcat.ayeautocustomer

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessageServices : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            sendNotification(it.title.toString(), it.body.toString())
        }
    }

    override fun onNewToken(token: String) {}

    private fun sendNotification(messageTitle: String, messageBody: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder = NotificationCompat.Builder(this, getString(R.string.notification_channel_default))
                .setSmallIcon(R.drawable.auto)
                .setColor(resources.getColor(R.color.accentColor))
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.customer_icon))
                .setContentTitle(messageTitle)
                .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}