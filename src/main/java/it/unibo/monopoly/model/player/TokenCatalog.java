package it.unibo.monopoly.model.player;

import java.util.List;

/**
 * Le pedine disponibili a inizio partita: il "sacchetto" da cui i giocatori scelgono.
 * <p>
 * Le pedine sono dati fissi del gioco, come le caselle del tabellone, e stanno nel
 * model perche' non c'entrano niente con l'interfaccia: la schermata di setup si
 * limita a mostrarle, e una partita avviata da riga di comando puo' usare le stesse.
 * <p>
 * I colori sono scritti come stringhe ("RED", "BLUE", ...) perche' cosi' vuole
 * {@link Token}: e' la view a trasformarli in colori veri
 * ({@link it.unibo.monopoly.view.ViewStyle#colorOf(Token) ViewStyle.colorOf}), e in
 * questo modo il model non dipende da Swing. Sono tutti diversi fra loro, cosi' due
 * pedine non si confondono mai sul tabellone.
 * <p>
 * Le pedine sono {@value #SIZE}, cioe' abbastanza per il numero massimo di giocatori
 * previsto da {@link it.unibo.monopoly.model.game.GameState GameState}: ogni
 * giocatore puo' averne una diversa anche a tavolo pieno.
 * <p>
 * Classe di sola utilita': tutti i membri sono statici e non e' istanziabile.
 */
public final class TokenCatalog {

    /** Quante pedine ci sono in tutto. */
    public static final int SIZE = 8;

    /**
     * L'elenco delle pedine, in ordine di presentazione.
     * <p>
     * {@link List#of} restituisce una lista immutabile: nessuno puo' aggiungere,
     * togliere o riordinare le pedine del gioco.
     */
    private static final List<Token> TOKENS = List.of(
            new Token("Cappello", "RED"),
            new Token("Auto", "BLUE"),
            new Token("Cane", "GREEN"),
            new Token("Nave", "YELLOW"),
            new Token("Ditale", "ORANGE"),
            new Token("Scarpa", "PURPLE"),
            new Token("Carriola", "CYAN"),
            new Token("Gatto", "BROWN"));

    /** Classe di utilita': non deve essere istanziata. */
    private TokenCatalog() {
    }

    /**
     * @return tutte le pedine fra cui si puo' scegliere, in sola lettura e sempre
     *         nello stesso ordine
     */
    public static List<Token> availableTokens() {
        return TOKENS;
    }

    /**
     * La pedina che si propone al giocatore numero {@code index}, cosi' chi apre la
     * schermata di setup trova gia' pedine diverse invece di doverle cambiare tutte.
     *
     * @param index il numero del giocatore, a partire da 0
     * @return la pedina proposta
     * @throws IllegalArgumentException se non esiste una pedina per quel numero
     */
    public static Token defaultTokenFor(final int index) {
        if (index < 0 || index >= SIZE) {
            throw new IllegalArgumentException("Non c'e' una pedina per il giocatore numero " + index);
        }
        return TOKENS.get(index);
    }
}
