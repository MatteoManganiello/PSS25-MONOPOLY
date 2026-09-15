package it.unibo.monopoly.model.persistence;

import java.util.List;

import it.unibo.monopoly.model.game.GamePhase;
import it.unibo.monopoly.model.player.PlayerStatus;

/**
 * Fotografia serializzabile di una partita: tutto e solo cio' che serve per
 * ricostruirla.
 * <p>
 * <b>Cosa contiene.</b> I dati che cambiano durante la partita. Per ogni giocatore:
 * nome, pedina, denaro, posizione, stato, proprieta' possedute e tentativi falliti di
 * uscire di prigione. Per la partita: il giocatore di turno, la fase del turno, i
 * doppi consecutivi, la fine della partita e la cassa della banca.
 * <p>
 * <b>Cosa non contiene, e perche'.</b>
 * <ul>
 *   <li>Il tabellone: e' sempre quello standard, e il caricamento lo ricostruisce con
 *       {@link it.unibo.monopoly.model.board.BoardFactory BoardFactory}. Delle caselle
 *       basta sapere chi possiede cosa, e le proprieta' sono indicate con la loro
 *       posizione sul tabellone: un numero stabile, a differenza del nome, che e' solo
 *       un testo da mostrare.</li>
 *   <li>I dadi: le regole leggono i dadi solo subito dopo il lancio, e cio' che del
 *       lancio conta anche dopo (i doppi consecutivi, la fase del turno) e' gia' qui.
 *       Lo stato interno del generatore casuale, invece, non ha senso conservarlo.</li>
 * </ul>
 * <p>
 * E' un {@code record}: immutabile e con {@code equals} generato dal compilatore, quindi
 * due fotografie della stessa situazione risultano uguali (i test lo sfruttano). Il
 * costruttore controlla solo che i dati obbligatori ci siano; la coerenza fra i dati,
 * per esempio che una proprieta' non abbia due proprietari, la verifica
 * {@link GameStateLoader} quando ricostruisce la partita.
 *
 * @param bankBalance        denaro in cassa alla banca
 * @param currentPlayerIndex indice, in {@code players}, del giocatore di turno
 * @param phase              fase del turno corrente
 * @param consecutiveDoubles doppi consecutivi del giocatore di turno
 * @param gameOver           true se la partita e' terminata
 * @param players            i giocatori, nell'ordine di turno
 */
public record SaveData(int bankBalance, int currentPlayerIndex, GamePhase phase,
                       int consecutiveDoubles, boolean gameOver, List<PlayerData> players) {

    /**
     * Controlla i dati obbligatori e rende immutabile l'elenco dei giocatori.
     *
     * @throws IllegalArgumentException se la fase o l'elenco dei giocatori sono null
     */
    public SaveData {
        if (phase == null || players == null) {
            throw new IllegalArgumentException("La fase del turno e l'elenco dei giocatori sono obbligatori");
        }
        players = List.copyOf(players);
    }

    /**
     * Dati salvati di un singolo giocatore.
     *
     * @param name               nome del giocatore
     * @param tokenName          nome della pedina
     * @param tokenColor         colore della pedina, come stringa
     *                           (vedi {@link it.unibo.monopoly.model.player.Token#getColor()})
     * @param money              denaro posseduto
     * @param position           posizione sul tabellone
     * @param status             stato del giocatore (in gioco, in prigione, fallito)
     * @param propertyPositions  posizioni sul tabellone delle proprieta' possedute,
     *                           nell'ordine in cui il giocatore le ha ottenute
     * @param failedJailAttempts tentativi falliti di uscire di prigione; 0 per chi non
     *                           e' in prigione
     */
    public record PlayerData(String name, String tokenName, String tokenColor, int money, int position,
                             PlayerStatus status, List<Integer> propertyPositions, int failedJailAttempts) {

        /**
         * Controlla i dati obbligatori e rende immutabile l'elenco delle proprieta'.
         *
         * @throws IllegalArgumentException se nome, pedina, stato o proprieta' sono null
         */
        public PlayerData {
            if (name == null || tokenName == null || tokenColor == null
                    || status == null || propertyPositions == null) {
                throw new IllegalArgumentException(
                        "Nome, pedina, stato e proprieta' di un giocatore salvato sono obbligatori");
            }
            propertyPositions = List.copyOf(propertyPositions);
        }
    }
}
