package monopoly.model.economy;

import java.util.List;
import java.util.Optional;

import monopoly.model.game.GameEventSupport;
import monopoly.model.player.Player;
import monopoly.model.player.PlayerStatus;

/**
 * Regole economiche della partita: acquisto e vendita di proprieta', affitti,
 * tasse, stipendi e fallimento.
 * <p>
 * Divisione dei compiti rispetto alla {@link Bank}:
 * <ul>
 *   <li>la {@code Bank} e' la cassa: sa solo <em>spostare</em> denaro
 *       ({@link Bank#pay}, {@link Bank#charge}) e controllare che un importo sia
 *       positivo e i fondi sufficienti;</li>
 *   <li>l'{@code EconomyManager} conosce le <em>regole del gioco</em> sul denaro:
 *       chi paga chi, quanto, cosa succede se non ci sono abbastanza soldi.</li>
 * </ul>
 * Le caselle non toccano mai il denaro direttamente: chiamano un metodo di questa
 * classe. Cosi' la regola economica e' scritta una volta sola e la stessa regola
 * vale per tutte le caselle che ne hanno bisogno.
 * <p>
 * <b>Regola del fallimento</b>: se un giocatore deve pagare piu' di quanto possiede,
 * non va in rosso. Consegna al creditore (l'altro giocatore, oppure la banca se il
 * debito era verso di lei) <em>tutto</em> il contante che gli resta e <em>tutte</em>
 * le sue proprieta', poi passa nello stato {@link PlayerStatus#BANKRUPT} ed esce
 * dalla partita: da quel momento {@link monopoly.model.game.GameState#advanceToNextPlayer()
 * advanceToNextPlayer()} lo salta. Le proprieta' cedute alla banca tornano in vendita.
 * <p>
 * Ogni movimento che cambia lo stato della partita viene annunciato tramite
 * {@link GameEventSupport}, cosi' la view puo' mostrarlo senza che il model la conosca.
 */
public class EconomyManager {

    private final Bank bank;
    private final GameEventSupport events;

    /**
     * Crea le regole economiche di una partita.
     *
     * @param bank   la cassa da usare per tutti i movimenti di denaro
     * @param events il canale su cui annunciare i movimenti
     * @throws IllegalArgumentException se un parametro e' null
     */
    public EconomyManager(final Bank bank, final GameEventSupport events) {
        if (bank == null || events == null) {
            throw new IllegalArgumentException("Banca e canale degli eventi non possono essere null");
        }
        this.bank = bank;
        this.events = events;
    }

    /** @return la banca su cui si appoggiano tutti i movimenti di denaro */
    public Bank getBank() {
        return this.bank;
    }

    // ------------------------------------------------------------------
    // Proprieta'
    // ------------------------------------------------------------------

    /**
     * Fa comprare al giocatore una proprieta' ancora libera: addebita il prezzo e
     * lo registra come nuovo proprietario.
     * <p>
     * Non poter pagare non e' un fallimento: semplicemente l'acquisto non avviene e
     * la casella resta in vendita.
     *
     * @param buyer    il giocatore che compra
     * @param property la proprieta' da comprare
     * @return true se l'acquisto e' andato a buon fine
     * @throws IllegalArgumentException se un parametro e' null
     */
    public boolean buyProperty(final Player buyer, final Property property) {
        checkNotNull(buyer, property);
        final int price = property.getPrice();
        if (!property.isAvailable() || !this.bank.charge(buyer, price)) {
            return false;
        }
        // addProperty tiene allineati i due lati della relazione (lista del giocatore e owner).
        buyer.addProperty(property);
        this.events.fire(listener -> listener.onPropertyBought(buyer, property, price));
        return true;
    }

    /**
     * Rivende alla banca una proprieta' posseduta, che torna disponibile per tutti.
     * <p>
     * Semplificazione rispetto al regolamento (che prevede l'ipoteca): la banca
     * riacquista al prezzo di listino. Serve a dare al giocatore un modo per fare
     * cassa prima di rischiare il fallimento.
     *
     * @param seller   il proprietario che vende
     * @param property la proprieta' da vendere
     * @return l'importo incassato, oppure 0 se la proprieta' non era sua
     * @throws IllegalArgumentException se un parametro e' null
     */
    public int sellPropertyToBank(final Player seller, final Property property) {
        checkNotNull(seller, property);
        if (!property.isOwnedBy(seller)) {
            return 0;
        }
        final int price = property.getPrice();
        seller.removeProperty(property);
        this.bank.pay(seller, price);
        this.events.fire(listener -> listener.onPropertySold(seller, property, price));
        return price;
    }

    /**
     * Fa pagare a un giocatore l'affitto della proprieta' su cui si e' fermato.
     * <p>
     * L'importo e' chiesto a {@link Property#getRent()}: e' una chiamata polimorfica,
     * quindi una futura sottoclasse (terreno con case, stazione, societa') cambiera'
     * il calcolo dell'affitto senza che questo metodo debba essere modificato.
     * Se il denaro non basta, il giocatore fallisce verso il proprietario.
     *
     * @param tenant   il giocatore fermatosi sulla casella
     * @param property la proprieta' su cui si e' fermato, con un proprietario diverso da lui
     * @return true se l'affitto e' stato pagato per intero, false se ha causato il fallimento
     * @throws IllegalArgumentException se un parametro e' null o la proprieta' non ha proprietario
     */
    public boolean payRent(final Player tenant, final Property property) {
        checkNotNull(tenant, property);
        final Player owner = property.getOwner()
                .orElseThrow(() -> new IllegalArgumentException("La proprieta' non ha un proprietario"));
        if (owner.equals(tenant)) {
            // Nessuno paga l'affitto a se' stesso: la casella lo esclude gia', qui e' solo una difesa.
            return true;
        }
        final int rent = property.getRent();
        if (!this.bank.canAfford(tenant, rent)) {
            this.declareBankruptcy(tenant, owner);
            return false;
        }
        this.transfer(tenant, owner, rent);
        this.events.fire(listener -> listener.onRentPaid(tenant, owner, property, rent));
        return true;
    }

    // ------------------------------------------------------------------
    // Movimenti da e verso la banca
    // ------------------------------------------------------------------

    /**
     * Fa versare un importo alla banca (tassa, cauzione, multa).
     * Se il giocatore non ha abbastanza denaro fallisce verso la banca.
     *
     * @param player il giocatore che paga
     * @param reason motivo del pagamento, usato nei messaggi della view
     * @param amount importo dovuto, strettamente positivo
     * @return true se il pagamento e' stato eseguito per intero, false se ha causato il fallimento
     * @throws IllegalArgumentException se il giocatore e' null o l'importo non e' positivo
     */
    public boolean payToBank(final Player player, final String reason, final int amount) {
        // charge esegue il pagamento solo se i fondi bastano, quindi non serve un controllo a parte.
        if (!this.bank.charge(player, amount)) {
            this.declareBankruptcy(player, null);
            return false;
        }
        this.events.fire(listener -> listener.onMoneyPaidToBank(player, reason, amount));
        return true;
    }

    /**
     * Fa incassare al giocatore un importo dalla banca (stipendio del "Via", jackpot).
     *
     * @param player il giocatore che incassa
     * @param reason motivo dell'accredito, usato nei messaggi della view
     * @param amount importo da accreditare, strettamente positivo
     * @throws IllegalArgumentException se il giocatore e' null o l'importo non e' positivo
     */
    public void receiveFromBank(final Player player, final String reason, final int amount) {
        this.bank.pay(player, amount);
        this.events.fire(listener -> listener.onMoneyReceivedFromBank(player, reason, amount));
    }

    // ------------------------------------------------------------------
    // Fallimento
    // ------------------------------------------------------------------

    /**
     * Dichiara fallito un giocatore che non riesce a saldare un debito.
     * <p>
     * Applica la regola descritta nella documentazione della classe: contante e
     * proprieta' passano al creditore (o tornano alla banca) e il giocatore esce
     * dalla partita.
     *
     * @param debtor   il giocatore che non riesce a pagare
     * @param creditor il giocatore creditore, oppure {@code null} se il debito era verso la banca
     * @throws IllegalArgumentException se il debitore e' null
     */
    public void declareBankruptcy(final Player debtor, final Player creditor) {
        if (debtor == null) {
            throw new IllegalArgumentException("Il giocatore fallito non puo' essere null");
        }
        final int remainingCash = debtor.getMoney();
        if (remainingCash > 0) {
            if (creditor == null) {
                this.bank.charge(debtor, remainingCash);
            } else {
                this.transfer(debtor, creditor, remainingCash);
            }
        }
        // Copia della lista: removeProperty modifica proprio la lista su cui si itera.
        for (final Property property : List.copyOf(debtor.getProperties())) {
            debtor.removeProperty(property);
            if (creditor != null) {
                creditor.addProperty(property);
            }
            // Senza creditore la proprieta' resta senza proprietario: torna in vendita.
        }
        debtor.setStatus(PlayerStatus.BANKRUPT);
        this.events.fire(listener -> listener.onPlayerBankrupt(debtor, Optional.ofNullable(creditor)));
    }

    // ------------------------------------------------------------------
    // Metodi di supporto
    // ------------------------------------------------------------------

    /**
     * Sposta denaro da un giocatore a un altro passando dalla banca: e' il modo di
     * riusare i controlli gia' presenti in {@link Bank} (importo positivo, fondi
     * sufficienti). Il saldo della banca resta invariato, perche' incassa e riversa
     * lo stesso importo.
     */
    private void transfer(final Player from, final Player to, final int amount) {
        if (this.bank.charge(from, amount)) {
            this.bank.pay(to, amount);
        }
    }

    /** Controllo comune a tutte le operazioni su una proprieta'. */
    private static void checkNotNull(final Player player, final Property property) {
        if (player == null || property == null) {
            throw new IllegalArgumentException("Giocatore e proprieta' non possono essere null");
        }
    }

    @Override
    public String toString() {
        return "EconomyManager[" + this.bank + "]";
    }
}
