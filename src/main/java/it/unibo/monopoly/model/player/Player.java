package it.unibo.monopoly.model.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.unibo.monopoly.model.board.Board;
import it.unibo.monopoly.model.economy.Bank;
import it.unibo.monopoly.model.economy.Property;

/**
 * Un giocatore della partita.
 * <p>
 * Tiene i suoi dati: nome, pedina, soldi, posizione sul tabellone, proprieta' e
 * stato. E' una classe passiva apposta: si limita a tenere i dati in ordine, le
 * regole le applicano altri, cioe' la {@link Bank}, le sottoclassi di
 * {@link it.unibo.monopoly.model.board.Tile Tile} e il controller.
 * <p>
 * Non riscriviamo {@code equals}: ogni giocatore e' se' stesso, quindi due giocatori
 * con lo stesso nome restano due giocatori diversi.
 */
public class Player {

    private final String name;
    private final Token token;

    /** Le proprieta' che possiede: la lista e' privata, da fuori si vede in sola lettura. */
    private final List<Property> properties;

    private int money;
    private int position;
    private PlayerStatus status;

    /**
     * Crea un giocatore con i soldi iniziali standard.
     *
     * @param name  nome del giocatore
     * @param token la sua pedina
     */
    public Player(final String name, final Token token) {
        this(name, token, Bank.STARTING_BALANCE);
    }

    /**
     * Crea un giocatore scegliendo quanti soldi ha all'inizio.
     *
     * @param name          nome del giocatore
     * @param token         la sua pedina
     * @param initialMoney  soldi iniziali, non possono essere negativi
     * @throws IllegalArgumentException se i parametri non vanno bene
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

    public String getName() {
        return this.name;
    }

    public Token getToken() {
        return this.token;
    }

    public int getMoney() {
        return this.money;
    }

    /**
     * Cambia i soldi del giocatore.
     * <p>
     * Va chiamato passando dalla {@link Bank}, che e' l'unica che deve muovere soldi:
     * qui controlliamo solo che il valore sia valido.
     *
     * @param money i nuovi soldi, non negativi
     * @throws IllegalArgumentException se l'importo e' negativo
     */
    public void setMoney(final int money) {
        if (money < 0) {
            throw new IllegalArgumentException("Il denaro di un giocatore non puo' diventare negativo");
        }
        this.money = money;
    }

    public int getPosition() {
        return this.position;
    }

    /**
     * Sposta il giocatore su una casella precisa.
     *
     * @param position la casella, tra 0 e {@link Board#SIZE} - 1
     * @throws IllegalArgumentException se la posizione non esiste sul tabellone
     */
    public void setPosition(final int position) {
        if (position < 0 || position >= Board.SIZE) {
            throw new IllegalArgumentException("Posizione non valida sul tabellone: " + position);
        }
        this.position = position;
    }

    public PlayerStatus getStatus() {
        return this.status;
    }

    /**
     * Cambia lo stato del giocatore: in gioco, in prigione o fallito.
     *
     * @param status il nuovo stato, non puo' essere null
     */
    public void setStatus(final PlayerStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Lo stato del giocatore non puo' essere null");
        }
        this.status = status;
    }

    /** @return true se sta ancora giocando */
    public boolean isPlaying() {
        return this.status == PlayerStatus.PLAYING;
    }

    /** @return true se e' in prigione */
    public boolean isInJail() {
        return this.status == PlayerStatus.IN_JAIL;
    }

    /** @return true se e' fallito */
    public boolean isBankrupt() {
        return this.status == PlayerStatus.BANKRUPT;
    }

    /**
     * @return le sue proprieta', in sola lettura. Per cambiarle bisogna usare
     *         {@link #addProperty(Property)} e {@link #removeProperty(Property)}
     */
    public List<Property> getProperties() {
        return Collections.unmodifiableList(this.properties);
    }

    /**
     * Aggiunge una proprieta' al giocatore e segna lui come proprietario, cosi' le due
     * informazioni restano d'accordo.
     *
     * @param property la proprieta' appena presa
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
     * Toglie una proprieta' al giocatore e la fa tornare alla banca.
     *
     * @param property la proprieta' da togliere
     * @return true se ce l'aveva davvero
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
