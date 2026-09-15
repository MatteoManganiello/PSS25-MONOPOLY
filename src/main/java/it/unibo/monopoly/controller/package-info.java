/**
 * Controller dell'architettura MVC: coordinamento tra model e view.
 * <p>
 * {@link it.unibo.monopoly.controller.GameEngine} e' l'unico punto di ingresso per chi
 * comanda la partita: fa eseguire i comandi al model e notifica gli eventi agli
 * osservatori ({@link it.unibo.monopoly.controller.GameObserver}), cioe' alle view.
 */
package it.unibo.monopoly.controller;
