package monopoly.model.game;

import java.util.HashMap;
import java.util.Map;

import monopoly.model.board.Board;
import monopoly.model.economy.EconomyManager;
import monopoly.model.player.Player;
import monopoly.model.player.PlayerStatus;

/**
 * Regole della prigione: chi ci entra, come ci si esce e quanto costa uscirne.
 * <p>
 * Sta in un'unica classe perche' in prigione si finisce da due strade diverse
 * (tre doppi consecutivi, gestiti dal {@link TurnManager}, e la casella
 * {@link monopoly.model.board.GoToJailTile GoToJailTile}) e si esce da tre
 * ({@link #releaseWithDouble(Player)}, cauzione pagata di propria volonta',
 * cauzione obbligatoria dopo i tentativi falliti): avere un solo posto in cui la
 * regola e' scritta evita che le due strade si comportino in modo diverso.
 * <p>
 * <b>Regola scelta</b> (semplificata ma completa):
 * <ol>
 *   <li>chi va in prigione viene spostato sulla casella {@link Board#JAIL_POSITION}
 *       e passa nello stato {@link PlayerStatus#IN_JAIL}; non e' eliminato, continua
 *       a ricevere il proprio turno;</li>
 *   <li>a ogni turno tenta l'uscita lanciando i dadi: con un doppio esce subito e si
 *       muove del valore uscito, ma non rilancia;</li>
 *   <li>in alternativa, prima di lanciare, puo' pagare la cauzione di
 *       {@value #BAIL_AMOUNT} e uscire subito;</li>
 *   <li>dopo {@value #MAX_ATTEMPTS} tentativi falliti la cauzione diventa
 *       obbligatoria: la paga, esce e resta sulla casella fino al turno successivo.
 *       Se non riesce a pagarla, fallisce verso la banca.</li>
 * </ol>
 * I tentativi falliti sono tenuti qui, in una mappa, invece che dentro il
 * {@link Player}: sono un dettaglio della regola della prigione, e cosi' il giocatore
 * continua a descrivere solo i propri dati. La mappa usa l'identita' degli oggetti,
 * perche' {@code Player} non ridefinisce {@code equals}.
 */
public class JailManager {

    /** Importo della cauzione da pagare alla banca per uscire di prigione. */
    public static final int BAIL_AMOUNT = 50;

    /** Numero di tentativi (lanci senza doppio) dopo i quali la cauzione diventa obbligatoria. */
    public static final int MAX_ATTEMPTS = 3;

    /** Motivo dell'uscita mostrato dalla view quando si esce con un doppio. */
    private static final String RELEASED_BY_DOUBLE = "doppio";

    /** Motivo dell'uscita mostrato dalla view quando si esce pagando. */
    private static final String RELEASED_BY_BAIL = "cauzione pagata";

    private final EconomyManager economy;
    private final GameEventSupport events;
    private final Map<Player, Integer> failedAttempts;

    /**
     * Crea le regole della prigione di una partita.
     *
     * @param economy le regole economiche, usate per incassare la cauzione
     * @param events  il canale su cui annunciare ingressi e uscite
     * @throws IllegalArgumentException se un parametro e' null
     */
    public JailManager(final EconomyManager economy, final GameEventSupport events) {
        if (economy == null || events == null) {
            throw new IllegalArgumentException("Economia e canale degli eventi non possono essere null");
        }
        this.economy = economy;
        this.events = events;
        this.failedAttempts = new HashMap<>();
    }

    /**
     * Manda un giocatore in prigione: lo sposta sulla casella della prigione, ne
     * aggiorna lo stato e azzera i tentativi di uscita.
     * <p>
     * E' il punto in cui confluiscono sia i tre doppi consecutivi sia la casella
     * "Vai in prigione". Il movimento e' diretto: il giocatore non percorre il
     * tabellone e quindi non passa dal "Via".
     *
     * @param player il giocatore da incarcerare
     * @throws IllegalArgumentException se il giocatore e' null
     */
    public void sendToJail(final Player player) {
        requirePlayer(player);
        player.setPosition(Board.JAIL_POSITION);
        player.setStatus(PlayerStatus.IN_JAIL);
        this.failedAttempts.put(player, 0);
        this.events.fire(listener -> listener.onPlayerSentToJail(player));
    }

    /**
     * Libera un giocatore che ha fatto un doppio: uscita gratuita.
     *
     * @param player il giocatore da liberare
     */
    public void releaseWithDouble(final Player player) {
        this.release(player, RELEASED_BY_DOUBLE);
    }

    /**
     * Fa pagare la cauzione e libera il giocatore.
     * <p>
     * Se il giocatore non riesce a pagarla fallisce verso la banca e resta fuori
     * dalla partita: in quel caso non viene liberato.
     *
     * @param player il giocatore che paga la cauzione
     * @return true se la cauzione e' stata pagata e il giocatore e' libero
     * @throws IllegalArgumentException se il giocatore e' null
     */
    public boolean payBailAndRelease(final Player player) {
        requirePlayer(player);
        if (!this.economy.payToBank(player, "cauzione", BAIL_AMOUNT)) {
            return false;
        }
        this.release(player, RELEASED_BY_BAIL);
        return true;
    }

    /**
     * Registra un tentativo di uscita fallito (lancio senza doppio).
     *
     * @param player il giocatore che ha fallito il tentativo
     * @return il numero di tentativi falliti da quando e' entrato in prigione
     */
    public int registerFailedAttempt(final Player player) {
        requirePlayer(player);
        final int attempts = this.getFailedAttempts(player) + 1;
        this.failedAttempts.put(player, attempts);
        return attempts;
    }

    /**
     * @param player il giocatore da controllare
     * @return quanti tentativi di uscita ha gia' fallito
     */
    public int getFailedAttempts(final Player player) {
        return this.failedAttempts.getOrDefault(player, 0);
    }

    /**
     * @param player il giocatore da controllare
     * @return true se ha esaurito i tentativi e deve pagare la cauzione
     */
    public boolean hasUsedAllAttempts(final Player player) {
        return this.getFailedAttempts(player) >= MAX_ATTEMPTS;
    }

    /**
     * @param player il giocatore da controllare
     * @return true se e' in prigione e ha il denaro per pagare subito la cauzione
     *         (serve alla view per abilitare il comando "paga la cauzione")
     */
    public boolean canPayBail(final Player player) {
        return player != null
                && player.isInJail()
                && this.economy.getBank().canAfford(player, BAIL_AMOUNT);
    }

    /** Riporta il giocatore in gioco e dimentica i suoi tentativi falliti. */
    private void release(final Player player, final String reason) {
        requirePlayer(player);
        player.setStatus(PlayerStatus.PLAYING);
        this.failedAttempts.remove(player);
        this.events.fire(listener -> listener.onPlayerReleasedFromJail(player, reason));
    }

    private static void requirePlayer(final Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Il giocatore non puo' essere null");
        }
    }

    @Override
    public String toString() {
        return "JailManager[bail=" + BAIL_AMOUNT + ", maxAttempts=" + MAX_ATTEMPTS + "]";
    }
}
