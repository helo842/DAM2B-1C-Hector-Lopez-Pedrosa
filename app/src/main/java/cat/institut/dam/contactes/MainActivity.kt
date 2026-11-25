package cat.institut.dam.contactes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * MainActivity — la llista de contactes.
 *
 * Aquesta classe és l'esquelet de la pràctica: la interfície ja està
 * connectada, però la part de les dades (carregar, filtrar i obrir el
 * detall) la has de fer tu. Busca els TODO.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        /** Codi de la petició de permís de notificacions. */
        private const val REQUEST_NOTIFICACIONS = 100
        private const val TAG = "MainActivity"
    }

    /** Per tornar al fil principal des del fil de segon pla. */
    private val filPrincipal = Handler(Looper.getMainLooper())

    private lateinit var adapter: ContacteAdapter

    /** Per no llançar dues càrregues alhora si es prem el botó molts cops. */
    private var carregant = false

    /** Només demanem el permís un cop per sessió (si el denega no l'atabalem més). */
    private var permisDemanat = false

    private lateinit var etCerca: EditText
    private lateinit var btnActualitza: Button
    private lateinit var pbCarga: ProgressBar
    private lateinit var txtBuit: TextView
    private lateinit var txtAvis: TextView
    private lateinit var rvContactes: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Finestres.aplica(this, findViewById<View>(R.id.arrel))

        etCerca = findViewById(R.id.etCerca)
        btnActualitza = findViewById(R.id.btnActualitza)
        pbCarga = findViewById(R.id.pbCarga)
        txtBuit = findViewById(R.id.txtBuit)
        txtAvis = findViewById(R.id.txtAvis)
        rvContactes = findViewById(R.id.rvContactes)

        // TODO 1: l'adapter. En prémer una fila obrim el detall passant només
        // l'id a l'Intent; la fitxa ja buscarà el contacte al repositori.
        adapter = ContacteAdapter(
            onContacteClick = { contacte ->
                val intent = Intent(this, ContacteDetallActivity::class.java)
                intent.putExtra(ContacteDetallActivity.EXTRA_ID, contacte.id)
                startActivity(intent)
            },
            onFavoritClick = { contacte ->
                ContactesRepository.alternaFavorit(contacte.id)
            }
        )
        rvContactes.layoutManager = LinearLayoutManager(this)
        rvContactes.adapter = adapter

        // TODO 2: filtrem a cada tecla
        etCerca.doAfterTextChanged { mostraLlista() }

        // TODO 3: tornar a carregar
        btnActualitza.setOnClickListener { carrega() }

        // TODO 4: el permís es demana dins de carrega(), quan ja hi ha dades.
        carrega()
    }

    override fun onResume() {
        super.onResume()
        // En tornar de la fitxa el favorit pot haver canviat: repintem.
        mostraLlista()
    }

    /**
     * TODO 5: carrega els contactes.
     *
     * La petició HTTP no es pot fer al fil principal: has de fer-la en un
     * fil de segon pla i tornar al fil principal amb un Handler.
     *
     * Si no hi ha xarxa, carrega els contactes del fitxer local
     * `assets/contactes.json` i avisa l'usuari amb `txtAvis`.
     *
     * Quan la llista s'hagi carregat correctament, crida
     * `demanaPermisNotificacions()` abans de mostrar la notificació.
     */
    private fun carrega() {
        if (carregant) return
        carregant = true
        pbCarga.visibility = View.VISIBLE
        btnActualitza.isEnabled = false

        Thread {
            // Això s'executa en un fil de segon pla
            var contactes: List<Contacte>? = null
            var sonLocals = false
            try {
                contactes = ContactesService.desDeXarxa()
            } catch (e: Exception) {
                Log.w(TAG, "No s'ha pogut carregar de la xarxa, faig servir la còpia local", e)
                try {
                    contactes = ContactesService.desDeRecurs(applicationContext)
                    sonLocals = true
                } catch (e2: Exception) {
                    Log.e(TAG, "Tampoc s'ha pogut llegir la còpia local", e2)
                }
            }

            // Tornem al fil principal per tocar la interfície
            filPrincipal.post {
                if (isFinishing || isDestroyed) return@post
                carregant = false
                pbCarga.visibility = View.GONE
                btnActualitza.isEnabled = true

                if (contactes == null) {
                    Toast.makeText(this, R.string.error_carrega, Toast.LENGTH_LONG).show()
                    mostraLlista()
                    return@post
                }

                ContactesRepository.actualitza(contactes)
                txtAvis.text = getString(R.string.avis_dades_locals)
                txtAvis.visibility = if (sonLocals) View.VISIBLE else View.GONE
                mostraLlista()

                // Primer carreguem, després permís i notificació
                if (!permisDemanat) {
                    permisDemanat = true
                    demanaPermisNotificacions()
                }
                // Si encara no hi ha permís no fa res (ho comprova per dins)
                Notificacions.mostraContactesCarregats(this, contactes.size)
            }
        }.start()
    }

    /**
     * Aplica el filtre del cercador i mostra el text de llista buida si cal.
     */
    private fun mostraLlista() {
        val texte = etCerca.text?.toString().orEmpty()
        val filtrats = ContactesRepository.cerca(texte)
        adapter.actualitza(filtrats)

        if (filtrats.isEmpty() && !carregant) {
            txtBuit.setText(
                if (texte.isBlank()) R.string.txt_buit else R.string.txt_buit_cerca
            )
            txtBuit.visibility = View.VISIBLE
        } else {
            txtBuit.visibility = View.GONE
        }
    }

    /**
     * A partir d'Android 13 (API 33) cal demanar permís en temps d'execució
     * per poder mostrar notificacions. En versions anteriors el permís no
     * existeix i no cal demanar-lo.
     */
    private fun demanaPermisNotificacions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permissio = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permissio) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        requestPermissions(arrayOf(permissio), REQUEST_NOTIFICACIONS)
    }

    /**
     * Resposta de l'usuari a la petició de permís: si el denega, avisa'l amb
     * un Toast.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICACIONS) {
            val concedit = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (concedit) {
                // Ara sí: notifiquem la càrrega que ja s'havia fet
                Notificacions.mostraContactesCarregats(this, ContactesRepository.llista.size)
            } else {
                // TODO 6: no és un error, l'app continua funcionant sense notificacions
                Toast.makeText(this, R.string.permis_denegada, Toast.LENGTH_LONG).show()
            }
        }
    }
}
