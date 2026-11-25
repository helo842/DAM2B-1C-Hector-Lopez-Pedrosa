package cat.institut.dam.contactes

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Notificacions — el canal i la notificació de "contactes carregats".
 */
object Notificacions {

    private const val CANAL_ID = "contactes_carregats"
    private const val NOTIFICACIO_ID = 1

    /**
     * Crea el canal (obligatori des d'Android 8). Si ja existeix no passa res,
     * el sistema conserva la configuració que tenia.
     */
    fun creaCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                context.getString(R.string.canal_notificacions),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(canal)
        }
    }

    /**
     * Mostra la notificació amb el nombre de contactes.
     * Si l'usuari no ha donat permís (o té les notificacions desactivades)
     * no fem res: l'app ha de continuar funcionant igual.
     */
    @SuppressLint("MissingPermission") // ja ho comprovem amb areNotificationsEnabled()
    fun mostraContactesCarregats(context: Context, quantitat: Int) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        creaCanal(context)

        val notificacio = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.drawable.ic_notificacio)
            .setContentTitle(context.getString(R.string.notificacio_titol))
            .setContentText(context.getString(R.string.notificacio_text, quantitat))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICACIO_ID, notificacio)
    }
}
