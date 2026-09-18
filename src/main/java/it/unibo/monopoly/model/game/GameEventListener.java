package it.unibo.monopoly.model.game;

import java.util.Optional;

import it.unibo.monopoly.model.economy.Property;
import it.unibo.monopoly.model.player.Player;

/**
 * Ascoltatore dei fatti concreti che accadono dentro il model: passaggi di denaro,
 * cambi di proprietario, ingressi e uscite di prigione, fallimenti.
 * <p>
 * E' il lato "model" del pattern Observer usato dalle view: le caselle, la
 * {@link it.unibo.monopoly.model.economy.EconomyManager EconomyManager} e il
 * {@link JailManager} devono poter raccontare cosa hanno fatto, ma non devono
 * conoscere ne' la view ne' il controller. Per questo l'interfaccia sta nel model
 * e viene estesa da {@link it.unibo.monopoly.controller.GameObserver GameObserver}, che vi
 * aggiunge gli eventi di svolgimento della partita (inizio, dadi, movimento, fine).
 * Una view, implementando {@code GameObserver}, riceve quindi entrambi i gruppi di
 * eventi con una sola registrazione.
 * <p>
 * Tutti i metodi hanno un corpo vuoto di default: ogni ascoltatore ridefinisce solo
 * gli eventi che gli interessano.
 * <p>
 * Gli oggetti ricevuti come parametro vanno usati in sola lettura.
 */
public interface GameEventListener {

    /**
     * A un giocatore viene chiesto se vuole comprare la proprieta' libera su cui si e'
     * fermato.
     * <p>
     * La partita si ferma qui e aspetta: nessuno ha ancora pagato niente e la proprieta'
     * e' ancora libera. La risposta arriva dai comandi
     * {@link it.unibo.monopoly.controller.GameEngine#buyOfferedProperty() buyOfferedProperty()}
     * e {@link it.unibo.monopoly.controller.GameEngine#declineOfferedProperty()
     * declineOfferedProperty()}, quindi una view che riceve questo evento deve solo
     * mostrare la domanda, non rispondere da sola.
     *
     * @param player   il giocatore a cui viene fatta l'offerta
     * @param property la proprieta' che puo' comprare
     * @param price    quanto costa
     */
    default void onPurchaseOffered(final Player player, final Property property, final int price) {
    }

    /**
     * Un giocatore ha comprato una proprieta' dalla banca.
     *
     * @param buyer    l'acquirente, gia' registrato come nuovo proprietario
     * @param property la proprieta' acquistata
     * @param price    il prezzo pagato
     */
    default void onPropertyBought(final Player buyer, final Property property, final int price) {
    }

    /**
     * Un giocatore ha rivenduto una proprieta' alla banca.
     *
     * @param seller   il venditore
     * @param property la proprieta' tornata in vendita
     * @param price    l'importo incassato
     */
    default void onPropertySold(final Player seller, final Property property, final int price) {
    }

    /**
     * Un giocatore ha pagato l'affitto al proprietario di una casella.
     *
     * @param tenant   chi si e' fermato sulla casella e ha pagato
     * @param owner    il proprietario che ha incassato
     * @param property la proprieta' su cui e' stato pagato l'affitto
     * @param amount   l'importo dell'affitto
     */
    default void onRentPaid(final Player tenant, final Player owner, final Property property, final int amount) {
    }

    /**
     * Un giocatore ha versato denaro alla banca (tassa, cauzione, multa).
     *
     * @param player il giocatore che ha pagato
     * @param reason motivo del pagamento, gia' pronto per essere mostrato
     * @param amount l'importo versato
     */
    default void onMoneyPaidToBank(final Player player, final String reason, final int amount) {
    }

    /**
     * Un giocatore ha ricevuto denaro dalla banca (stipendio del "Via").
     *
     * @param player il giocatore che ha incassato
     * @param reason motivo dell'accredito, gia' pronto per essere mostrato
     * @param amount l'importo ricevuto
     */
    default void onMoneyReceivedFromBank(final Player player, final String reason, final int amount) {
    }

    /**
     * Un giocatore e' finito in prigione (tre doppi consecutivi o casella "Vai in prigione").
     *
     * @param player il giocatore finito in prigione
     */
    default void onPlayerSentToJail(final Player player) {
    }

    /**
     * Un giocatore e' uscito di prigione.
     *
     * @param player il giocatore liberato
     * @param reason come e' uscito (doppio o cauzione), gia' pronto per essere mostrato
     */
    default void onPlayerReleasedFromJail(final Player player, final String reason) {
    }

    /**
     * Un giocatore e' fallito ed e' uscito dalla partita.
     *
     * @param player   il giocatore fallito
     * @param creditor il creditore che ha incassato quanto restava, oppure
     *                 {@link Optional#empty()} se il debito era verso la banca
     */
    default void onPlayerBankrupt(final Player player, final Optional<Player> creditor) {
    }
}
