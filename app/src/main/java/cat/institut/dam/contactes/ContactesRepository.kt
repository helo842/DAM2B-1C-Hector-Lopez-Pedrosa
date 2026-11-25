package cat.institut.dam.contactes

import android.net.Uri

/**
 * ContactesRepository — dades compartides entre les dues pantalles.
 *
 * És un object (singleton): només n'hi ha un per tota l'app, així que la
 * MainActivity i la ContacteDetallActivity veuen sempre les mateixes dades.
 * Les dades són en memòria: es perden quan es tanca l'app.
 */
object ContactesRepository {

    private val contactes = mutableListOf<Contacte>()

    /** Ids dels contactes marcats com a favorits (un Set no té duplicats). */
    private val favorits = mutableSetOf<Int>()

    /** Fotos triades per l'usuari a la galeria, per id de contacte. */
    private val fotos = mutableMapOf<Int, Uri>()

    /** La llista de contactes carregats (només lectura des de fora). */
    val llista: List<Contacte>
        get() = contactes

    /** Substitueix la llista sencera per la nova. */
    fun actualitza(novs: List<Contacte>) {
        contactes.clear()
        contactes.addAll(novs)
    }

    /** Busca un contacte pel seu id. Torna null si no hi és. */
    fun contacte(id: Int): Contacte? = contactes.firstOrNull { it.id == id }

    fun esFavorit(id: Int): Boolean = id in favorits

    /** Si era favorit el treu, i si no ho era l'afegeix. */
    fun alternaFavorit(id: Int) {
        if (!favorits.remove(id)) {
            favorits.add(id)
        }
    }

    /**
     * Filtra per nom, ciutat o correu. Amb el text buit torna tots els
     * contactes (si no, el cercador es quedaria buit en esborrar-ho tot).
     */
    fun cerca(texte: String): List<Contacte> {
        val t = texte.trim()
        if (t.isEmpty()) return llista
        return contactes.filter { c ->
            c.nom.contains(t, ignoreCase = true) ||
                c.ciutat.contains(t, ignoreCase = true) ||
                c.email.contains(t, ignoreCase = true)
        }
    }

    fun desaFoto(id: Int, uri: Uri) {
        fotos[id] = uri
    }

    fun foto(id: Int): Uri? = fotos[id]
}
