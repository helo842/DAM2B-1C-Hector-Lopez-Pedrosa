# Contactes — Pràctica 1C

**Alumne:** Héctor López Pedrosa · DAM2B
**Mòdul:** 0489 Programació Multimèdia i Dispositius Mòbils

## Què fa l'app

*Contactes* és una agenda que carrega els contactes d'Internet
(`https://jsonplaceholder.typicode.com/users`) i els mostra en una llista amb cercador.
Si no hi ha connexió, carrega la còpia local `assets/contactes.json` i ho avisa amb un
missatge taronja.

- **Pantalla principal (`MainActivity`)**: llista amb inicials, nom, ciutat, correu i
  casella de favorit. El cercador filtra per nom, ciutat o correu a cada tecla. El botó
  *Actualitza* torna a carregar. En acabar la càrrega es demana el permís de
  notificacions i es mostra una notificació amb el nombre de contactes.
- **Fitxa (`ContacteDetallActivity`)**: foto (baixada de la xarxa o triada de la
  galeria), dades del contacte, favorit i sis botons que obren altres apps amb Intents
  implícits: Trucar, Escriu, Mapa, Web, Alarma i Galeria.

Les dues pantalles comparteixen les dades amb un `object ContactesRepository`
(singleton) i l'Intent només porta l'`id` del contacte (`EXTRA_ID`).

## Fitxers que he afegit

| Fitxer | Responsabilitat |
|---|---|
| `Contacte.kt` | Model (`data class`) i conversió des de JSON |
| `ContactesRepository.kt` | Dades compartides, favorits, cerca i fotos triades |
| `ContactesService.kt` | Petició HTTP, còpia local i baixada d'imatges |
| `ContacteAdapter.kt` | Adapter i ViewHolder del `RecyclerView` |
| `ContacteDetallActivity.kt` | Fitxa del contacte i Intents implícits |
| `Notificacions.kt` | Canal i notificació "contactes carregats" |
| `res/layout/item_contacte.xml` | Fila de la llista |
| `res/layout/activity_detall.xml` | Pantalla de detall (dins d'un `ScrollView`) |

També he completat els TODO 1–6 de `MainActivity.kt` i el TODO 7 de
`AndroidManifest.xml`. A més he afegit el permís `com.android.alarm.permission.SET_ALARM`
al manifest, perquè sense ell el botó *Alarma* petava (ho explico a l'informe).

## Com executar-la

1. Android Studio → `File` → `Open` → aquesta carpeta (la que té `settings.gradle.kts`).
2. Esperar el **Gradle Sync**. Si el `local.properties` no apunta al teu SDK,
   Android Studio el regenera (no es puja al repositori).
3. Triar un emulador (provat amb un Pixel 7) i prémer **Run ▶**.

Des de terminal: `./gradlew assembleDebug` i l'APK queda a `app/build/outputs/apk/debug/`.

## Resum de proves

He fet les 15 proves de l'apartat 5.1 de l'enunciat a l'emulador i totes donen el
resultat esperat. També he provat l'app en castellà, anglès i català, en tema clar i
fosc, i que res quedi tapat per les barres del sistema (edge-to-edge). Les captures i
la taula completa són a l'informe: [`informe/informe-prac1c.pdf`](informe/informe-prac1c.pdf).

| Prova | Resultat |
|---|---|
| Càrrega amb connexió (10 contactes) | ✅ |
| Actualitza | ✅ |
| Cerca "Barcelona" amb dades locals → 3 contactes | ✅ |
| Cerca sense resultats → missatge | ✅ |
| Favorits que es mantenen en tornar de la fitxa | ✅ |
| Fitxa amb les dades i la foto | ✅ |
| Trucar / Escriu / Mapa / Web / Alarma / Galeria | ✅ |
| Sense xarxa → dades locals i avís taronja | ✅ |
| Permís de notificacions denegat → l'app continua | ✅ |
| Idiomes (ca, es, en) i tema fosc | ✅ |
