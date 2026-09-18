package it.unibo.monopoly.view;

import java.io.PrintStream;

/**
 * View testuale: stampa su console la cronaca della partita.
 * <p>
 * E' un esempio concreto di {@link it.unibo.monopoly.controller.GameObserver GameObserver}:
 * riceve le notifiche dal {@link it.unibo.monopoly.controller.GameEngine GameEngine} e si
 * limita a mostrarle, senza conoscere le regole e senza modificare il model.
 * <p>
 * Tutte le frasi della cronaca vivono in {@link TextGameObserver}, la classe astratta
 * da cui questa deriva: qui resta solo la <em>destinazione</em> del testo, cioe' lo
 * stream su cui scrivere. La GUI usa la stessa classe base per
 * riempire la propria area di log, quindi i due racconti sono identici parola per
 * parola e non c'e' un solo messaggio duplicato.
 * <p>
 * La GUI non sostituisce questa view: le due possono osservare la stessa partita
 * contemporaneamente, ed e' proprio cio' che fa
 * {@link it.unibo.monopoly.MonopolyApp MonopolyApp} registrandole entrambe sul motore.
 */
public class ConsoleGameObserver extends TextGameObserver {

    private final PrintStream out;

    /** Crea una view che scrive sullo standard output. */
    public ConsoleGameObserver() {
        this(System.out);
    }

    /**
     * Crea una view che scrive sullo stream indicato (utile per reindirizzare l'output).
     *
     * @param out lo stream su cui stampare
     * @throws IllegalArgumentException se lo stream e' null
     */
    public ConsoleGameObserver(final PrintStream out) {
        if (out == null) {
            throw new IllegalArgumentException("Lo stream di output non puo' essere null");
        }
        this.out = out;
    }

    /**
     * Manda la riga sullo stream di questa view.
     *
     * @param line la riga da stampare
     */
    @Override
    protected void write(final String line) {
        this.out.println(line);
    }
}
