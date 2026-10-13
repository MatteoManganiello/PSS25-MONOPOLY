package it.unibo.monopoly.model.board;

/**
 * Carta di un mazzo "Imprevisti" o "Probabilita'".
 * <p>
 * In questa versione ogni carta ha un solo tipo di effetto, un movimento di denaro con
 * la banca: un importo positivo e' un incasso, uno negativo un pagamento. E' la forma
 * piu' semplice che dia un senso alle caselle senza toccare le regole del turno (una
 * carta che sposta la pedina dovrebbe far scattare a sua volta l'effetto della casella
 * di arrivo). Ad applicare l'effetto e' {@link CardTile}, con le stesse regole
 * economiche usate dalle tasse e dallo stipendio del "Via".
 * <p>
 * Il testo non contiene l'importo: e' la view a scriverlo, nello stesso formato di
 * tutti gli altri importi del gioco.
 *
 * @param text   testo della carta, pronto da mostrare
 * @param amount denaro incassato dalla banca se positivo, versato alla banca se negativo;
 *               mai zero
 */
public record Card(String text, int amount) {

    /**
     * Controlla che la carta abbia un testo e un effetto.
     *
     * @throws IllegalArgumentException se il testo e' vuoto o l'importo e' zero
     */
    public Card {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Il testo della carta non puo' essere vuoto");
        }
        if (amount == 0) {
            throw new IllegalArgumentException("Una carta deve far incassare o pagare qualcosa");
        }
    }

    /** @return true se la carta fa incassare denaro, false se lo fa pagare */
    public boolean isGain() {
        return this.amount > 0;
    }
}
