/**
 * Persistenza della partita: salvataggio su file e ricaricamento.
 * <p>
 * Sta nel model perche' si occupa solo di tradurre lo stato della partita
 * ({@link it.unibo.monopoly.model.game.GameState}) in dati da scrivere su disco e
 * viceversa: non conosce ne' Swing ne' gli osservatori, e si puo' usare e testare da
 * sola. E' il {@link it.unibo.monopoly.controller.GameEngine GameEngine} a usarla quando
 * la view chiede di salvare o caricare, e a notificare gli osservatori quando una partita
 * caricata prende il posto di quella in corso.
 * <p>
 * Le classi, dal dato al file:
 * <ul>
 *   <li>{@link it.unibo.monopoly.model.persistence.SaveData}: la fotografia della partita,
 *       un record immutabile con i soli dati che servono per ricostruirla;</li>
 *   <li>{@code SaveFileFormat}: come quella fotografia diventa un file di testo
 *       {@code .properties} e viceversa; e' l'unico posto in cui sono scritti i nomi delle
 *       chiavi e la versione del formato;</li>
 *   <li>{@link it.unibo.monopoly.model.persistence.GameStateSaver}: dalla partita al file;</li>
 *   <li>{@link it.unibo.monopoly.model.persistence.GameStateLoader}: dal file a una nuova
 *       partita, controllata e completa;</li>
 *   <li>{@link it.unibo.monopoly.model.persistence.SaveFileException} e
 *       {@link it.unibo.monopoly.model.persistence.PersistenceResult}: come vengono
 *       comunicati gli errori e gli esiti.</li>
 * </ul>
 */
package it.unibo.monopoly.model.persistence;
