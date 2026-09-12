package monopoly.model.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Registro degli ascoltatori del model e coda degli eventi ancora da consegnare
 * (ruolo "Subject" del pattern Observer, lato model).
 * <p>
 * Gli eventi non vengono consegnati subito: vengono <em>accodati</em> da
 * {@link #fire(Consumer)} e consegnati tutti insieme quando il controller chiama
 * {@link #publishPending()}, cioe' al termine del comando in corso. Il motivo e'
 * l'ordine del racconto: le caselle producono i loro effetti <em>dentro</em>
 * {@code TurnManager.rollDice()}, quindi una consegna immediata farebbe comparire
 * "Alice compra Vicolo Corto" prima ancora di "Alice lancia i dadi". Accodando,
 * il {@link monopoly.controller.GameEngine GameEngine} pubblica prima il lancio e
 * il movimento e poi gli effetti, nell'ordine in cui un giocatore se li aspetta.
 * <p>
 * Se non c'e' nessun ascoltatore gli eventi non vengono nemmeno accodati: il model
 * resta utilizzabile da solo (per esempio nei test) senza accumulare nulla.
 * <p>
 * L'evento e' rappresentato come un {@link Consumer}: una piccola funzione che dice
 * quale metodo dell'ascoltatore chiamare e con quali argomenti.
 */
public class GameEventSupport {

    private final List<GameEventListener> listeners;
    private final Queue<Consumer<GameEventListener>> pendingEvents;

    /** Crea un registro senza ascoltatori. */
    public GameEventSupport() {
        this.listeners = new ArrayList<>();
        this.pendingEvents = new ArrayDeque<>();
    }

    /**
     * Registra un ascoltatore. Registrare due volte lo stesso oggetto non ha effetto.
     *
     * @param listener l'ascoltatore da registrare
     * @throws IllegalArgumentException se l'ascoltatore e' null
     */
    public void addListener(final GameEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("L'ascoltatore non puo' essere null");
        }
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
    }

    /**
     * Rimuove un ascoltatore, che non ricevera' piu' eventi.
     *
     * @param listener l'ascoltatore da rimuovere
     */
    public void removeListener(final GameEventListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Accoda un evento, che verra' consegnato alla prossima {@link #publishPending()}.
     *
     * @param event il metodo dell'ascoltatore da invocare, con i suoi argomenti
     */
    public void fire(final Consumer<GameEventListener> event) {
        if (event == null) {
            throw new IllegalArgumentException("L'evento non puo' essere null");
        }
        if (!this.listeners.isEmpty()) {
            this.pendingEvents.add(event);
        }
    }

    /**
     * Consegna a tutti gli ascoltatori gli eventi accumulati e svuota la coda.
     * Viene chiamata dal controller al termine di ogni comando.
     */
    public void publishPending() {
        while (!this.pendingEvents.isEmpty()) {
            final Consumer<GameEventListener> event = this.pendingEvents.remove();
            // Copia della lista: un ascoltatore puo' registrarsi o rimuoversi mentre riceve l'evento.
            List.copyOf(this.listeners).forEach(event);
        }
    }

    /** @return true se ci sono eventi in attesa di essere pubblicati */
    public boolean hasPendingEvents() {
        return !this.pendingEvents.isEmpty();
    }

    @Override
    public String toString() {
        return "GameEventSupport[listeners=" + this.listeners.size()
                + ", pending=" + this.pendingEvents.size() + "]";
    }
}
