package it.unibo.monopoly.controller;

import it.unibo.monopoly.model.player.Token;

/**
 * Quello che l'utente ha scritto e scelto per un giocatore nella schermata iniziale:
 * un nome e una pedina.
 * <p>
 * Non e' ancora un {@link it.unibo.monopoly.model.player.Player Player}, ed e' voluto. Un {@code Player} e' un giocatore
 * vero e non accetta dati sbagliati (nome vuoto, pedina mancante); questo invece e'
 * il modulo appena compilato, che <em>puo'</em> essere incompleto: il nome puo'
 * essere vuoto e la pedina puo' mancare, perche' e' esattamente la situazione in cui
 * si trova una riga della schermata prima che l'utente finisca di riempirla. A dire
 * se i dati vanno bene ci pensa {@link GameSetup}, e solo dopo nascono i giocatori.
 * <p>
 * E' un {@code record} perche' non fa altro che tenere insieme due valori: il
 * compilatore scrive da solo costruttore, accessori, {@code equals} e {@code toString},
 * e i campi non si possono modificare.
 *
 * @param name  il nome scritto dall'utente, anche vuoto o fatto di soli spazi
 * @param token la pedina scelta, oppure {@code null} se non ne ha ancora scelta una
 */
public record PlayerSetup(String name, Token token) {
}
