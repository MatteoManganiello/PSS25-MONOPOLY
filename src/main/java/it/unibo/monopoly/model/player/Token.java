package it.unibo.monopoly.model.player;

import java.util.Objects;

/**
 * La pedina di un giocatore, cioe' quella che si muove sul tabellone.
 * <p>
 * Non si puo' modificare: i campi sono {@code final} e non ci sono setter. Una
 * pedina non cambia mai nome o colore, quindi tanto vale bloccarla e non rischiare
 * errori.
 */
public class Token {

    private final String name;
    private final String color;

    /**
     * Crea una pedina.
     *
     * @param name  nome della pedina
     * @param color colore della pedina, lo usa la view per disegnarla
     * @throws IllegalArgumentException se nome o colore sono null o vuoti
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

    public String getName() {
        return this.name;
    }

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
