package monopoly.model.board;

import monopoly.model.economy.Bank;
import monopoly.model.economy.EconomyManager;
import monopoly.model.player.Player;

/**
 * Casella di partenza, il "Via": la banca versa lo stipendio a chi ci passa sopra
 * o ci si ferma.
 * <p>
 * E' l'unica casella che ridefinisce sia {@link #onLand(Player)} sia
 * {@link #onPass(Player)}, e serve a far vedere la differenza tra i due: l'effetto
 * e' lo stesso (lo stipendio), ma nasce da due situazioni diverse, il giro completo
 * del tabellone e la sosta esatta sulla casella.
 */
public class StartTile extends Tile {

    /** Motivo mostrato dalla view quando viene accreditato lo stipendio. */
    private static final String REASON = "stipendio del Via";

    private final int salary;
    private final EconomyManager economy;

    /**
     * Crea la casella "Via" con lo stipendio standard di {@link Bank#GO_SALARY}.
     *
     * @param name     nome della casella (es. "Via")
     * @param position posizione sul tabellone, normalmente {@link Board#START_POSITION}
     * @param economy  le regole economiche della partita
     */
    public StartTile(final String name, final int position, final EconomyManager economy) {
        this(name, position, Bank.GO_SALARY, economy);
    }

    /**
     * Crea la casella "Via" specificando lo stipendio.
     *
     * @param name     nome della casella
     * @param position posizione sul tabellone
     * @param salary   somma versata dalla banca, strettamente positiva
     * @param economy  le regole economiche della partita
     * @throws IllegalArgumentException se lo stipendio non e' positivo o l'economia e' null
     */
    public StartTile(final String name, final int position, final int salary, final EconomyManager economy) {
        super(name, position);
        if (salary <= 0) {
            throw new IllegalArgumentException("Lo stipendio del Via deve essere positivo");
        }
        if (economy == null) {
            throw new IllegalArgumentException("Le regole economiche non possono essere null");
        }
        this.salary = salary;
        this.economy = economy;
    }

    /** @return lo stipendio versato dalla banca */
    public int getSalary() {
        return this.salary;
    }

    /**
     * Accredita lo stipendio a chi si ferma esattamente sul "Via".
     *
     * @param player il giocatore che si e' fermato sulla casella
     */
    @Override
    public void onLand(final Player player) {
        this.paySalary(player);
    }

    /**
     * Accredita lo stipendio a chi completa il giro passando dal "Via".
     *
     * @param player il giocatore che sta attraversando la casella
     */
    @Override
    public void onPass(final Player player) {
        this.paySalary(player);
    }

    /** Unico punto in cui viene versato lo stipendio: la regola e' scritta una volta sola. */
    private void paySalary(final Player player) {
        this.economy.receiveFromBank(player, REASON, this.salary);
    }
}
