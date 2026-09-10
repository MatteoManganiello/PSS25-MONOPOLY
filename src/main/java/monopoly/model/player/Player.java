package monopoly.model.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import monopoly.model.board.Board;
import monopoly.model.economy.Bank;
import monopoly.model.economy.Property;

/**
 * Giocatore della partita.
 * <p>
 * Contiene i dati che descrivono lo stato di un partecipante: nome, pedina,
 * denaro, posizione sul tabellone, proprieta' possedute e stato di gioco.
 * La classe e' volutamente "passiva": conosce i propri dati e li mantiene
 * coerenti, ma non decide le regole (chi le applica sono la {@link Bank},
 * le sottoclassi di {@link monopoly.model.board.Tile Tile} e, dai prossimi giorni, il controller).
 * <p>
 * L'identita' di un giocatore e' l'oggetto stesso: non si ridefinisce
 * {@code equals}, cosi' due giocatori con lo stesso nome restano distinti.
 */
public class Player {

    private final String name;
    private final Token token;

    /** Proprieta' possedute: lista privata, esposta all'esterno solo in sola lettura. */
    private final List<Property> properties;

    private int money;
    private int position;
    private PlayerStatus status;

    /**
     * Crea un giocatore con il capitale iniziale standard.
     *
     * @param name  nome del giocatore
     * @param token pedina assegnata al giocatore
     */
    public Player(final String name, final Token token) {
        this(name, token, Bank.STARTING_BALANCE);
    }

    /**
     * Crea un giocatore specificando il capitale iniziale.
     *
     * @param name          nome del giocatore
     * @param token         pedina assegnata al giocatore
     * @param initialMoney  denaro iniziale, non negativo
     * @throws IllegalArgumentException se i parametri non sono validi
     */
    public Player(final String name, final Token token, final int initialMoney) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Il nome del giocatore non puo' essere vuoto");
        }
        if (token == null) {
            throw new IllegalArgumentException("Il giocatore deve avere una pedina");
        }
        if (initialMoney < 0) {
            throw new IllegalArgumentException("Il denaro iniziale non puo' essere negativo");
        }
        this.name = name;
        this.token = token;
        this.money = initialMoney;
        this.position = Board.START_POSITION;
        this.status = PlayerStatus.PLAYING;
        this.properties = new ArrayList<>();
    }

    /** @return il nome del giocatore */
    public String getName() {
        return this.name;
    }

    /** @return la pedina del giocatore */
    public Token getToken() {
        return this.token;
    }

    /** @return il denaro attualmente posseduto */
    public int getMoney() {
        return this.money;
    }

    /**
     * Imposta il denaro del giocatore.
     * <p>
     * Da usare tramite la {@link Bank}, che e' l'unico oggetto responsabile dei
     * movimenti di denaro: qui si controlla solo che il valore resti valido.
     *
     * @param money nuovo importo, non negativo
     * @throws IllegalArgumentException se l'importo e' negativo
     */
    public void setMoney(final int money) {
        if (money < 0) {
            throw new IllegalArgumentException("Il denaro di un giocatore non puo' diventare negativo");
        }
        this.money = money;
    }

    /** @return la posizione attuale sul tabellone */
    public int getPosition() {
        return this.position;
    }

    /**
     * Sposta il giocatore su una casella specifica.
     *
     * @param position indice della casella, tra 0 e {@link Board#SIZE} - 1
     * @throws IllegalArgumentException se la posizione non appartiene al tabellone
     */
    public void setPosition(final int position) {
        if (position < 0 || position >= Board.SIZE) {
            throw new IllegalArgumentException("Posizione non valida sul tabellone: " + position);
        }
        this.position = position;
    }

    /** @return lo stato attuale del giocatore */
    public PlayerStatus getStatus() {
        return this.status;
    }

    /**
     * Aggiorna lo stato del giocatore (in gioco, in prigione, fallito).
     *
     * @param status il nuovo stato, non nullo
     */
    public void setStatus(final PlayerStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Lo stato del giocatore non puo' essere null");
        }
        this.status = status;
    }

    /** @return true se il giocatore e' ancora in partita */
    public boolean isPlaying() {
        return this.status == PlayerStatus.PLAYING;
    }

    /** @return true se il giocatore e' in prigione */
    public boolean isInJail() {
        return this.status == PlayerStatus.IN_JAIL;
    }

    /** @return true se il giocatore e' fallito */
    public boolean isBankrupt() {
        return this.status == PlayerStatus.BANKRUPT;
    }

    /**
     * @return le proprieta' possedute, in sola lettura: la lista interna non e'
     *         modificabile dall'esterno, si passa da {@link #addProperty(Property)}
     *         e {@link #removeProperty(Property)}
     */
    public List<Property> getProperties() {
        return Collections.unmodifiableList(this.properties);
    }

    /**
     * Aggiunge una proprieta' al patrimonio del giocatore e ne aggiorna il proprietario,
     * mantenendo coerenti i due lati della relazione.
     *
     * @param property la proprieta' acquisita
     */
    public void addProperty(final Property property) {
        if (property == null) {
            throw new IllegalArgumentException("La proprieta' da aggiungere non puo' essere null");
        }
        if (!this.properties.contains(property)) {
            this.properties.add(property);
            property.setOwner(this);
        }
    }

    /**
     * Rimuove una proprieta' dal patrimonio del giocatore e la riporta alla banca.
     *
     * @param property la proprieta' da rimuovere
     * @return true se la proprieta' era effettivamente posseduta
     */
    public boolean removeProperty(final Property property) {
        final boolean removed = this.properties.remove(property);
        if (removed) {
            property.releaseOwner();
        }
        return removed;
    }

    @Override
    public String toString() {
        return "Player[name=" + this.name
                + ", token=" + this.token.getName()
                + ", money=" + this.money
                + ", position=" + this.position
                + ", properties=" + this.properties.size()
                + ", status=" + this.status
                + "]";
    }
}
