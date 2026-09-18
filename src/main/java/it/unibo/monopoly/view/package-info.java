/**
 * View dell'architettura MVC: presentazione della partita all'utente.
 * <p>
 * Le view implementano {@link it.unibo.monopoly.controller.GameObserver} e si aggiornano in
 * risposta agli eventi del {@link it.unibo.monopoly.controller.GameEngine}: leggono i dati dal
 * model e inoltrano i comandi dell'utente al controller, senza contenere regole di
 * gioco e senza modificare direttamente il model.
 * <p>
 * Il pacchetto contiene due view, che possono anche osservare la stessa
 * partita insieme:
 * <ul>
 *   <li>la <b>GUI Swing</b>, composta dalla finestra {@link it.unibo.monopoly.view.MainWindow}
 *       e dai suoi tre pannelli: {@link it.unibo.monopoly.view.BoardPanel} (il tabellone, fatto
 *       di {@link it.unibo.monopoly.view.TilePanel}), {@link it.unibo.monopoly.view.PlayerInfoPanel}
 *       (la situazione dei giocatori) e {@link it.unibo.monopoly.view.ControlPanel} (i comandi,
 *       i dadi e il log);</li>
 *   <li>la <b>view testuale</b> {@link it.unibo.monopoly.view.ConsoleGameObserver}, che stampa
 *       la cronaca sul terminale.</li>
 * </ul>
 * Prima della partita c'e' la {@link it.unibo.monopoly.view.SetupWindow}, la schermata
 * in cui si sceglie chi gioca: mostra una {@link it.unibo.monopoly.view.PlayerSetupRow}
 * per giocatore, non contiene nessuna regola (le chiede a
 * {@link it.unibo.monopoly.controller.GameSetup}) e alla conferma consegna i giocatori
 * a chi avvia il gioco.
 * <p>
 * Le frasi della cronaca sono scritte una volta sola in
 * {@link it.unibo.monopoly.view.TextGameObserver}, la classe astratta da cui derivano sia la
 * view su console sia il log della GUI; {@link it.unibo.monopoly.view.ViewStyle} raccoglie
 * invece colori, caratteri e formattazione, cioe' la traduzione dei dati del model in
 * qualcosa di disegnabile.
 */
package it.unibo.monopoly.view;
