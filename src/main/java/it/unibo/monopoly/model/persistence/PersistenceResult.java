package it.unibo.monopoly.model.persistence;

/**
 * Esito di un salvataggio o di un caricamento, nella forma che serve alla view: se
 * e' andato a buon fine e un messaggio da mostrare all'utente.
 * <p>
 * Chi salva o carica attraverso il {@link it.unibo.monopoly.controller.GameEngine
 * GameEngine} non riceve eccezioni per i problemi sui file: riceve un esito, lo mostra
 * e la partita continua. Cosi' un disco pieno o un file danneggiato non fanno mai
 * "cadere" l'interfaccia.
 *
 * @param successful true se l'operazione e' riuscita
 * @param message    descrizione dell'esito, pronta da mostrare
 */
public record PersistenceResult(boolean successful, String message) {

    /**
     * Controlla che il messaggio sia presente.
     *
     * @throws IllegalArgumentException se il messaggio e' null o vuoto
     */
    public PersistenceResult {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("L'esito deve avere un messaggio da mostrare");
        }
    }

    /**
     * @param message il messaggio da mostrare
     * @return un esito positivo
     */
    public static PersistenceResult success(final String message) {
        return new PersistenceResult(true, message);
    }

    /**
     * @param message il messaggio da mostrare, con il motivo del fallimento
     * @return un esito negativo
     */
    public static PersistenceResult failure(final String message) {
        return new PersistenceResult(false, message);
    }
}
