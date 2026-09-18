package it.unibo.monopoly.model.persistence;

/**
 * Errore nella lettura di un salvataggio: il file non c'e', non si riesce a leggere,
 * e' danneggiato oppure non e' un salvataggio che questa versione del gioco sa leggere.
 * <p>
 * E' un'eccezione <em>controllata</em> per scelta: un file sbagliato non e' un errore
 * di programmazione ma una situazione normale, che chi carica una partita deve per
 * forza gestire, e il compilatore glielo ricorda. Il {@link Problem} dice di che tipo
 * di errore si tratta (serve ai test e a chi vuole reagire in modo diverso a seconda
 * del caso); il messaggio lo spiega a parole ed e' pronto per essere mostrato.
 * <p>
 * Quando questa eccezione viene lanciata non esiste nessuna partita "a meta'":
 * {@link GameStateLoader} costruisce lo stato da zero e lo restituisce solo se ogni
 * controllo e' passato.
 */
public final class SaveFileException extends Exception {

    private static final long serialVersionUID = 1L;

    /** Tipo di problema riscontrato sul file di salvataggio. */
    public enum Problem {

        /** Il file indicato non esiste. */
        FILE_NOT_FOUND("file non trovato"),

        /** Il file esiste ma non si riesce a leggere (permessi, errore del disco, e' una cartella...). */
        UNREADABLE("impossibile leggere il file"),

        /** Il file e' un salvataggio, ma i dati sono incompleti, non validi o incoerenti fra loro. */
        CORRUPTED("salvataggio danneggiato"),

        /** Il file non e' un salvataggio di questo gioco, o e' di una versione del formato non supportata. */
        INCOMPATIBLE("salvataggio non compatibile");

        private final String description;

        Problem(final String description) {
            this.description = description;
        }

        /** @return una descrizione breve del problema, da mostrare all'utente */
        public String getDescription() {
            return this.description;
        }
    }

    private final Problem problem;

    /**
     * Crea l'errore senza una causa di piu' basso livello.
     *
     * @param problem il tipo di problema
     * @param detail  cosa e' andato storto, nel dettaglio
     * @throws IllegalArgumentException se il tipo di problema o il dettaglio sono null
     */
    public SaveFileException(final Problem problem, final String detail) {
        this(problem, detail, null);
    }

    /**
     * Crea l'errore conservando l'eccezione che l'ha provocato.
     *
     * @param problem il tipo di problema
     * @param detail  cosa e' andato storto, nel dettaglio
     * @param cause   l'eccezione originale (per esempio un errore di I/O), oppure null
     * @throws IllegalArgumentException se il tipo di problema o il dettaglio sono null
     */
    public SaveFileException(final Problem problem, final String detail, final Throwable cause) {
        super(describe(problem, detail), cause);
        this.problem = problem;
    }

    /** @return il tipo di problema riscontrato */
    public Problem getProblem() {
        return this.problem;
    }

    /** Messaggio completo: la descrizione del problema seguita dal dettaglio. */
    private static String describe(final Problem problem, final String detail) {
        if (problem == null || detail == null) {
            throw new IllegalArgumentException("Tipo di problema e dettaglio sono obbligatori");
        }
        return problem.getDescription() + ": " + detail;
    }
}
