/**
 * View dell'architettura MVC: presentazione della partita all'utente.
 * <p>
 * Le view implementano {@link monopoly.controller.GameObserver} e si aggiornano
 * in risposta agli eventi del {@link monopoly.controller.GameEngine}: leggono i
 * dati dal model e inoltrano i comandi dell'utente al controller, senza contenere
 * regole di gioco. Per ora c'e' solo la view testuale
 * {@link monopoly.view.ConsoleGameObserver}; la GUI arrivera' al Giorno 4.
 */
package monopoly.view;
