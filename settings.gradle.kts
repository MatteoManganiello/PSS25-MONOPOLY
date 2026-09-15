/*
 * Impostazioni del progetto Gradle.
 * Progetto a modulo singolo: qui si dichiara solo il nome della root.
 */

plugins {
    /*
     * Risolutore automatico delle toolchain Java.
     *
     * build.gradle.kts richiede il JDK 25: se sulla macchina non c'e', senza questo
     * plugin la build si ferma con "Cannot find a Java installation ... matching
     * languageVersion=25". Il plugin insegna a Gradle dove cercarlo (l'indice
     * pubblico foojay.io) e glielo fa scaricare da solo la prima volta, tenendolo
     * nella cache di Gradle: il progetto resta un progetto Java 25 e compila anche
     * su un computer con un JDK diverso, senza installare nulla a mano.
     */
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "monopoly"
