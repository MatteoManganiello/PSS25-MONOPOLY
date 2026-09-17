/**
 * Controller dell'architettura MVC: coordinamento tra model e view.
 * <p>
 * {@link it.unibo.monopoly.controller.GameEngine} e' l'unico punto di ingresso per chi
 * comanda la partita: fa eseguire i comandi al model e notifica gli eventi agli
 * osservatori ({@link it.unibo.monopoly.controller.GameObserver}), cioe' alle view.
 * <p>
 * Prima ancora della partita c'e' la sua preparazione:
 * {@link it.unibo.monopoly.controller.GameSetup} raccoglie chi gioca e con che pedina
 * ({@link it.unibo.monopoly.controller.PlayerSetup}), controlla che la configurazione
 * stia in piedi ({@link it.unibo.monopoly.controller.SetupProblem}) e costruisce i
 * giocatori da passare al motore. Sta qui, e non nella schermata che lo usa, perche'
 * quelle regole valgono da qualunque interfaccia si avvii una partita e si devono
 * poter provare senza aprire una finestra.
 */
package it.unibo.monopoly.controller;
