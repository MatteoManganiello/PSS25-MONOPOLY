/**
 * Controller dell'architettura MVC: coordinamento tra model e view.
 * <p>
 * {@link monopoly.controller.GameEngine} e' l'unico punto di ingresso per chi
 * comanda la partita: fa eseguire i comandi al model e notifica gli eventi agli
 * osservatori ({@link monopoly.controller.GameObserver}), cioe' alle view.
 */
package monopoly.controller;
