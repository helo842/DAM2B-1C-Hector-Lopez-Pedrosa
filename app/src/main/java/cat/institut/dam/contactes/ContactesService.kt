package cat.institut.dam.contactes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * ContactesService — d'on surten les dades: la xarxa, la còpia local i les imatges.
 *
 * ATENCIÓ: desDeXarxa() i baixaImatge() fan connexions de xarxa, així que
 * NO es poden cridar des del fil principal (s'han de cridar dins d'un Thread).
 */
object ContactesService {

    const val URL_CONTACTES = "https://jsonplaceholder.typicode.com/users"

    private const val TEMPS_MAXIM = 8000 // ms

    /** Fa la petició HTTP i torna la llista. Llança una excepció si falla. */
    fun desDeXarxa(): List<Contacte> {
        val connexio = URL(URL_CONTACTES).openConnection() as HttpURLConnection
        try {
            connexio.requestMethod = "GET"
            connexio.connectTimeout = TEMPS_MAXIM
            connexio.readTimeout = TEMPS_MAXIM

            if (connexio.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("El servidor ha respost amb ${connexio.responseCode}")
            }

            val text = connexio.inputStream.bufferedReader().use { it.readText() }
            return parseja(text)
        } finally {
            connexio.disconnect()
        }
    }

    /** Llegeix la còpia local que hi ha a assets/contactes.json. */
    fun desDeRecurs(context: Context): List<Contacte> {
        val text = context.assets.open("contactes.json").bufferedReader().use { it.readText() }
        return parseja(text)
    }

    /** Converteix el text JSON (un array d'objectes) en una llista de contactes. */
    fun parseja(text: String): List<Contacte> {
        val array = JSONArray(text)
        val resultat = mutableListOf<Contacte>()
        for (i in 0 until array.length()) {
            resultat.add(Contacte.desDeJson(array.getJSONObject(i)))
        }
        return resultat
    }

    /** Baixa una imatge i la torna com a Bitmap, o null si hi ha qualsevol error. */
    fun baixaImatge(url: String): Bitmap? {
        var connexio: HttpURLConnection? = null
        return try {
            connexio = URL(url).openConnection() as HttpURLConnection
            connexio.connectTimeout = TEMPS_MAXIM
            connexio.readTimeout = TEMPS_MAXIM
            if (connexio.responseCode != HttpURLConnection.HTTP_OK) {
                null
            } else {
                connexio.inputStream.use { BitmapFactory.decodeStream(it) }
            }
        } catch (e: Exception) {
            null
        } finally {
            connexio?.disconnect()
        }
    }
}
