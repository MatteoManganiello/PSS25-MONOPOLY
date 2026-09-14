/**
 * View dell'architettura MVC: presentazione della partita all'utente.
 * <p>
 * Le view implementano {@link monopoly.controller.GameObserver} e si aggiornano in
 * risposta agli eventi del {@link monopoly.controller.GameEngine}: leggono i dati dal
 * model e inoltrano i comandi dell'utente al controller, senza contenere regole di
 * gioco e senza modificare direttamente il model.
 * <p>
 * Dal Giorno 4 il pacchetto contiene due view, che possono anche osservare la stessa
 * partita insieme:
 * <ul>
 *   <li>la <b>GUI Swing</b>, composta dalla finestra {@link monopoly.view.MainWindow}
 *       e dai suoi tre pannelli: {@link monopoly.view.BoardPanel} (il tabellone, fatto
 *       di {@link monopoly.view.TilePanel}), {@link monopoly.view.PlayerInfoPanel}
 *       (la situazione dei giocatori) e {@link monopoly.view.ControlPanel} (i comandi,
 *       i dadi e il log);</li>
 *   <li>la <b>view testuale</b> {@link monopoly.view.ConsoleGameObserver}, che stampa
 *       la cronaca sul terminale.</li>
 * </ul>
 * Le frasi della cronaca sono scritte una volta sola in
 * {@link monopoly.view.TextGameObserver}, la classe astratta da cui derivano sia la
 * view su console sia il log della GUI; {@link monopoly.view.ViewStyle} raccoglie
 * invece colori, caratteri e formattazione, cioe' la traduzione dei dati del model in
 * qualcosa di disegnabile.
 */
package monopoly.view;
