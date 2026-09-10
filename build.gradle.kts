plugins {
    // Compilazione dei sorgenti Java e task di test.
    java
    // Aggiunge il task "run" e la creazione delle distribuzioni eseguibili.
    application
}

group = "monopoly"
version = "1.0.0"

java {
    // La toolchain garantisce che il progetto compili sempre con Java 21,
    // indipendentemente dal JDK con cui viene avviato Gradle.
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Il BOM allinea automaticamente le versioni di tutti gli artefatti JUnit 5.
    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // Da Gradle 9 il launcher della piattaforma va dichiarato esplicitamente.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    // Classe con il metodo main, usata dal task "run".
    mainClass = "monopoly.MonopolyApp"
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Segnala tutti i warning del compilatore: utile per tenere il codice pulito.
    options.compilerArgs.add("-Xlint:all")
}

tasks.test {
    // JUnit 5 gira sulla JUnit Platform, che va abilitata esplicitamente.
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.jar {
    // Rende il jar prodotto eseguibile con "java -jar".
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
        )
    }
}
