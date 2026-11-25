package cat.institut.dam.contactes

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * ContacteDetallActivity — la fitxa d'un contacte.
 *
 * Rep només l'id per l'Intent (EXTRA_ID) i busca la resta de dades al
 * ContactesRepository, que és el mateix objecte que fa servir la llista.
 */
class ContacteDetallActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "cat.institut.dam.contactes.EXTRA_ID"
        private const val REQUEST_FOTO = 200
        private const val TAG = "ContacteDetall"
    }

    private val filPrincipal = Handler(Looper.getMainLooper())

    private lateinit var contacte: Contacte
    private lateinit var ivFoto: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detall)
        Finestres.aplica(this, findViewById(R.id.arrel))

        // 1. Recuperem l'id i busquem el contacte
        val id = intent.getIntExtra(EXTRA_ID, -1)
        val trobat = ContactesRepository.contacte(id)
        if (trobat == null) {
            Toast.makeText(this, R.string.error_contacte, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        contacte = trobat

        // 2. Mostrem les dades
        ivFoto = findViewById(R.id.ivFoto)
        ivFoto.clipToOutline = true // perquè la foto quedi rodona com el fons
        findViewById<TextView>(R.id.tvNom).text = contacte.nom
        findViewById<TextView>(R.id.tvEmail).text = contacte.email
        findViewById<TextView>(R.id.tvTelefon).text = contacte.telefon
        findViewById<TextView>(R.id.tvAdreca).text = contacte.adreçaCompleta
        findViewById<TextView>(R.id.tvWeb).text = contacte.web

        val chkFavorit = findViewById<CheckBox>(R.id.chkFavorit)
        chkFavorit.isChecked = ContactesRepository.esFavorit(contacte.id)
        chkFavorit.setOnCheckedChangeListener { _, _ ->
            ContactesRepository.alternaFavorit(contacte.id)
        }

        // 3. La foto: la de la galeria si n'hi ha, si no la baixem de la xarxa
        val fotoPropia = ContactesRepository.foto(contacte.id)
        if (fotoPropia != null) {
            ivFoto.setImageURI(fotoPropia)
        } else {
            carregaAvatar()
        }

        // 4. Els botons
        findViewById<Button>(R.id.btnTrucar).setOnClickListener { trucar() }
        findViewById<Button>(R.id.btnCorreu).setOnClickListener { escriure() }
        findViewById<Button>(R.id.btnMapa).setOnClickListener { obreMapa() }
        findViewById<Button>(R.id.btnWeb).setOnClickListener { obreWeb() }
        findViewById<Button>(R.id.btnAlarma).setOnClickListener { posaAlarma() }
        findViewById<Button>(R.id.btnGaleria).setOnClickListener { triaFoto() }
    }

    /** Baixa l'avatar en segon pla. Si falla, es queda el dibuix per defecte. */
    private fun carregaAvatar() {
        ivFoto.setImageResource(R.drawable.ic_contacte)
        val url = contacte.urlAvatar
        Thread {
            val bitmap = ContactesService.baixaImatge(url)
            filPrincipal.post {
                if (isFinishing || isDestroyed) return@post
                // Si mentrestant l'usuari ha triat una foto, no la xafem
                if (bitmap != null && ContactesRepository.foto(contacte.id) == null) {
                    ivFoto.setImageBitmap(bitmap)
                }
            }
        }.start()
    }

    // ---------- Intents implícits ----------

    private fun trucar() {
        obre(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contacte.telefon}")))
    }

    private fun escriure() {
        obre(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contacte.email}")))
    }

    private fun obreMapa() {
        // Uri.encode per si el nom té espais o accents
        val etiqueta = Uri.encode(contacte.nom)
        val lat = contacte.latitud
        val lng = contacte.longitud
        obre(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng($etiqueta)")))
    }

    private fun obreWeb() {
        // El servidor torna "hildegard.org" sense esquema: l'hi afegim
        var web = contacte.web
        if (!web.startsWith("http")) web = "https://$web"
        obre(Intent(Intent.ACTION_VIEW, Uri.parse(web)))
    }

    private fun posaAlarma() {
        val alarma = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, 9)
            putExtra(AlarmClock.EXTRA_MINUTES, 0)
            putExtra(AlarmClock.EXTRA_MESSAGE, getString(R.string.alarma_missatge, contacte.nom))
        }
        obre(alarma)
    }

    /**
     * Llança un Intent implícit. Si no hi ha cap app que el pugui obrir,
     * Android llança ActivityNotFoundException: la capturem perquè l'app no peti.
     */
    private fun obre(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Cap app per a ${intent.action} ${intent.data}", e)
            Toast.makeText(this, R.string.error_app_no_disponible, Toast.LENGTH_LONG).show()
        } catch (e: SecurityException) {
            // Passa si l'altra app exigeix un permís que no tenim (ens va passar amb l'alarma)
            Log.e(TAG, "Permís denegat obrint ${intent.action}", e)
            Toast.makeText(this, R.string.error_app_no_disponible, Toast.LENGTH_LONG).show()
        }
    }

    // ---------- Galeria ----------

    @Suppress("DEPRECATION")
    private fun triaFoto() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
        try {
            startActivityForResult(intent, REQUEST_FOTO)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error_app_no_disponible, Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Fem servir startActivityForResult com a l'enunciat")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_FOTO || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            // Alguns selectors no donen permís persistent; per a aquesta sessió ja ens val
            Log.w(TAG, "No s'ha pogut obtenir el permís persistent", e)
        }
        ContactesRepository.desaFoto(contacte.id, uri)
        ivFoto.setImageURI(uri)
    }
}
