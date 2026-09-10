package monopoly.model.player;

import java.util.Objects;

/**
 * Pedina di un giocatore: e' l'oggetto fisico che si muove sul tabellone.
 * <p>
 * La classe e' immutabile (campi {@code final}, nessun setter): una pedina non
 * cambia mai nome o colore dopo essere stata assegnata, quindi renderla
 * immutabile evita errori e la rende sicura da condividere.
 */
public class Token {

    private final String name;
    private final String color;

    /**
     * Crea una nuova pedina.
     *
     * @param name  nome della pedina (es. "Car", "Dog")
     * @param color colore associato alla pedina (es. "RED"); sara' usato dalla view
     * @throws IllegalArgumentException se nome o colore sono nulli o vuoti
     */
    public Token(final String name, final String color) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Il nome della pedina non puo' essere vuoto");
        }
        if (color == null || color.isBlank()) {
            throw new IllegalArgumentException("Il colore della pedina non puo' essere vuoto");
        }
        this.name = name;
        this.color = color;
    }

    /** @return il nome della pedina */
    public String getName() {
        return this.name;
    }

    /** @return il colore della pedina */
    public String getColor() {
        return this.color;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Token)) {
            return false;
        }
        final Token token = (Token) other;
        return this.name.equals(token.name) && this.color.equals(token.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.color);
    }

    @Override
    public String toString() {
        return "Token[name=" + this.name + ", color=" + this.color + "]";
    }
}
