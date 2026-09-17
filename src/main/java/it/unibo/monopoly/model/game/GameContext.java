package it.unibo.monopoly.model.game;

import it.unibo.monopoly.model.economy.Bank;
import it.unibo.monopoly.model.economy.EconomyManager;

/**
 * Contenitore dei servizi condivisi da tutta la partita: banca, regole economiche,
 * regole della prigione e canale degli eventi.
 * <p>
 * Nasce da un problema concreto: {@link it.unibo.monopoly.model.board.Tile#onLand(it.unibo.monopoly.model.player.Player)
 * onLand(Player)} riceve solo il giocatore, ma una casella per fare il suo effetto
 * ha bisogno di collaboratori (per addebitare un affitto serve la banca, per mandare
 * in prigione servono le regole della prigione). La soluzione e' l'iniezione nel
 * costruttore: ogni casella riceve cio' che le serve quando viene creata, e al
 * momento dell'atterraggio le basta il giocatore.
 * <p>
 * Raccogliere quei collaboratori in un unico oggetto evita di trascinare tre o
 * quattro parametri in ogni costruttore e, soprattutto, garantisce che
 * <em>tabellone</em>, {@link TurnManager} e controller lavorino sulla stessa banca
 * e sulla stessa prigione: il contesto viene creato una volta sola all'inizio della
 * partita (vedi {@link GameState#createStandardGame}) e passato a tutti.
 */
public class GameContext {

    private final Bank bank;
    private final GameEventSupport events;
    private final EconomyManager economy;
    private final JailManager jail;

    /**
     * La partita a cui appartiene questo contesto.
     * <p>
     * Serve alle caselle: quando un giocatore si ferma su una proprieta' libera, la
     * casella deve registrare la decisione di acquisto in sospeso, e quella vive nel
     * {@link GameState}. Il collegamento lo fa il costruttore di {@code GameState}, che
     * e' anche l'unico momento in cui il contesto e la partita si conoscono.
     */
    private GameState state;

    /** Crea un contesto con una banca nuova e i relativi servizi. */
    public GameContext() {
        this(new Bank());
    }

    /**
     * Crea un contesto attorno a una banca gia' esistente (utile nei test e, in
     * futuro, per ricaricare una partita salvata).
     *
     * @param bank la banca della partita
     * @throws IllegalArgumentException se la banca e' null
     */
    public GameContext(final Bank bank) {
        if (bank == null) {
            throw new IllegalArgumentException("La banca non puo' essere null");
        }
        this.bank = bank;
        this.events = new GameEventSupport();
        this.economy = new EconomyManager(bank, this.events);
        this.jail = new JailManager(this.economy, this.events);
    }

    /**
     * Collega il contesto alla partita che lo usa. Lo chiama il costruttore di
     * {@link GameState}: non va usato da nessun altro.
     *
     * @param state la partita a cui appartiene questo contesto
     */
    void attachState(final GameState state) {
        this.state = state;
    }

    /**
     * @return la partita a cui appartiene questo contesto
     * @throws IllegalStateException se il contesto non e' ancora stato collegato a una
     *                               partita, cioe' se le caselle sono state costruite
     *                               senza creare poi un {@link GameState}
     */
    public GameState getState() {
        if (this.state == null) {
            throw new IllegalStateException("Il contesto non e' collegato a nessuna partita");
        }
        return this.state;
    }

    /** @return la cassa della partita */
    public Bank getBank() {
        return this.bank;
    }

    /** @return le regole su denaro, proprieta' e fallimento */
    public EconomyManager getEconomy() {
        return this.economy;
    }

    /** @return le regole della prigione */
    public JailManager getJail() {
        return this.jail;
    }

    /** @return il canale su cui il model annuncia cio' che accade */
    public GameEventSupport getEvents() {
        return this.events;
    }

    @Override
    public String toString() {
        return "GameContext[" + this.bank + ", " + this.jail + "]";
    }
}
