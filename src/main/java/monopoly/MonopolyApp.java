package monopoly;

import java.util.Arrays;

import monopoly.model.board.Board;
import monopoly.model.board.Tile;
import monopoly.model.economy.Bank;
import monopoly.model.economy.Property;
import monopoly.model.game.Dice;
import monopoly.model.game.GamePhase;
import monopoly.model.player.Player;
import monopoly.model.player.Token;

/**
 * Punto di ingresso dell'applicazione.
 * <p>
 * GIORNO 1: non esistono ancora ne' la view ne' il ciclo dei turni, quindi il
 * metodo main si limita a costruire il modello e a stamparlo su console, come
 * verifica rapida che le classi del dominio funzionino insieme.
 */
public final class MonopolyApp {

    /** Classe di utilita': non deve essere istanziata. */
    private MonopolyApp() {
    }

    /**
     * Avvia una dimostrazione testuale del modello.
     *
     * @param args argomenti da riga di comando (non utilizzati)
     */
    public static void main(final String[] args) {
        final Board board = new Board();
        final Bank bank = new Bank();
        final Dice dice = new Dice();

        final Player alice = new Player("Alice", new Token("Car", "RED"));
        final Player bob = new Player("Bob", new Token("Dog", "BLUE"));

        System.out.println("=== Monopoly - Giorno 1: modello del dominio ===");
        System.out.println("Caselle sul tabellone: " + board.getSize());
        System.out.println("Prima casella: " + board.getTileAt(Board.START_POSITION));
        System.out.println("Prigione:      " + board.getTileAt(Board.JAIL_POSITION));
        System.out.println("Fasi del turno previste: " + Arrays.toString(GamePhase.values()));
        System.out.println();

        System.out.println(alice);
        System.out.println(bob);
        System.out.println(bank);
        System.out.println();

        // Movimento di prova: lancio dei dadi e spostamento sul tabellone.
        final int roll = dice.roll();
        System.out.println("Alice lancia i dadi: " + dice + " -> avanza di " + roll + " caselle");
        // Il tabellone e' circolare: dopo la casella 39 si torna al "Via".
        alice.setPosition((alice.getPosition() + roll) % Board.SIZE);

        // Polimorfismo: si chiama onLand senza sapere di quale casella si tratti.
        final Tile landedTile = board.getTileAt(alice.getPosition());
        landedTile.onLand(alice);
        System.out.println("Alice arriva su: " + landedTile);
        System.out.println();

        // Esempio di acquisto di una proprieta' (le regole vere arriveranno dopo).
        final Property park = new Property("Parco della Vittoria", 39, 400, 50);
        if (bank.charge(bob, park.getPrice())) {
            bob.addProperty(park);
        }
        System.out.println("Dopo l'acquisto: " + bob);
        System.out.println("Proprieta' di Bob: " + bob.getProperties());
        System.out.println("Banca: " + bank);
    }
}
