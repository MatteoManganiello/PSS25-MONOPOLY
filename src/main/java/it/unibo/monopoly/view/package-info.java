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
 * a chi avvia il gioco. A partita finita la finestra di gioco si chiude e si torna qui,
 * pronti per la partita successiva.
 * <p>
 * Le frasi della cronaca sono scritte una volta sola in
 * {@link it.unibo.monopoly.view.TextGameObserver}, la classe astratta da cui derivano sia la
 * view su console sia il log della GUI. L'aspetto sta in due classi di utilita':
 * {@link it.unibo.monopoly.view.Theme} contiene la tavolozza, i caratteri, i bordi e lo
 * stile dei pulsanti, e nessun altro componente scrive un colore a mano;
 * {@link it.unibo.monopoly.view.ViewStyle} traduce i dati del model (categorie di
 * casella, gruppi di colore, pedine, stati dei giocatori, importi) in quei colori e in
 * testo da mostrare. Le superfici crema con il bordo grigio - schede, riquadri, righe
 * del setup - sono tutte {@link it.unibo.monopoly.view.CardPanel}.
 */
package it.unibo.monopoly.view;
