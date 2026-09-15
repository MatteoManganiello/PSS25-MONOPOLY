package monopoly.model.game;

import monopoly.model.economy.Bank;
import monopoly.model.economy.EconomyManager;

/**
 * Contenitore dei servizi condivisi da tutta la partita: banca, regole economiche,
 * regole della prigione e canale degli eventi.
 * <p>
 * Nasce da un problema concreto: {@link monopoly.model.board.Tile#onLand(monopoly.model.player.Player)
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
