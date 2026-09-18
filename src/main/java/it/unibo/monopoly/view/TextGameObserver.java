package it.unibo.monopoly.view;

import java.util.Optional;

import it.unibo.monopoly.controller.GameObserver;
import it.unibo.monopoly.model.board.Tile;
import it.unibo.monopoly.model.economy.Property;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.game.RollOutcome;
import it.unibo.monopoly.model.game.RollResult;
import it.unibo.monopoly.model.player.Player;

/**
 * Cronaca testuale della partita: trasforma in righe di testo tutto cio' che il
 * {@link it.unibo.monopoly.controller.GameEngine GameEngine} annuncia.
 * <p>
 * E' una classe astratta perche' sa <em>cosa</em> raccontare ma non <em>dove</em>
 * scriverlo: l'unico metodo non implementato e' {@link #write(String)}, e sono le
 * sottoclassi a deciderne la destinazione. Oggi la destinazione e' la console
 * ({@link ConsoleGameObserver}); la GUI non ha un'area di log, perche' cosa e'
 * successo si vede gia' sul tabellone e nelle schede dei giocatori.
 * <p>
 * E' l'esempio piu' diretto di riuso per ereditarieta' di questo progetto: la
 * formattazione dei messaggi e' scritta una volta sola qui, e aggiungere un'altra
 * destinazione (un file di log, per esempio) costa una sottoclasse di due righe. Gli
 * importi sono scritti in euro, nello stesso formato della GUI
 * ({@link ViewStyle#formatMoney(int)}).
 * <p>
 * Come ogni {@link GameObserver}, riceve gli oggetti del model in sola lettura: si
 * limita a descriverli, non li modifica.
 */
public abstract class TextGameObserver implements GameObserver {

    /**
     * Scrive una riga della cronaca sul supporto scelto dalla sottoclasse.
     * <p>
     * E' il "buco" lasciato dalla classe astratta: qui non si sa se la riga finira'
     * sulla console, in un'area di testo o in un file.
     *
     * @param line la riga da mostrare, gia' formattata
     */
    protected abstract void write(String line);

    @Override
    public void onGameStarted(final GameState state) {
        this.write("=== Partita iniziata con " + state.getPlayers().size() + " giocatori ===");
        for (final Player player : state.getPlayers()) {
            this.write("  " + player.getName() + " (pedina: " + player.getToken().getName() + ")");
        }
    }

    /**
     * Racconta una partita caricata: a differenza di un avvio, i giocatori hanno gia'
     * denaro, proprieta' e uno stato, e vale la pena ricordarli.
     *
     * @param state lo stato della partita caricata
     */
    @Override
    public void onGameLoaded(final GameState state) {
        this.write("");
        this.write("=== Partita caricata con " + state.getPlayers().size() + " giocatori ===");
        for (final Player player : state.getPlayers()) {
            this.write("  " + player.getName() + " (pedina: " + player.getToken().getName() + "): "
                    + ViewStyle.formatMoney(player.getMoney()) + ", proprieta': " + player.getProperties().size()
                    + ", " + ViewStyle.describe(player.getStatus()));
        }
    }

    @Override
    public void onTurnStarted(final Player player) {
        this.write("");
        this.write("--- Turno di " + player.getName() + (player.isInJail() ? " (in prigione)" : "") + " ---");
    }

    @Override
    public void onDiceRolled(final RollResult result) {
        this.write(result.player().getName() + " lancia i dadi: "
                + result.firstDie() + " + " + result.secondDie() + " = " + result.total()
                + describe(result.outcome()));
        if (result.passedGo()) {
            this.write("  ...e passa dal Via!");
        }
    }

    @Override
    public void onPlayerMoved(final Player player, final Tile from, final Tile to) {
        this.write("  " + player.getName() + " si sposta da \"" + from.getName()
                + "\" a \"" + to.getName() + "\"");
    }

    @Override
    public void onPlayerSentToJail(final Player player) {
        this.write("  " + player.getName() + " va in prigione!");
    }

    @Override
    public void onPurchaseOffered(final Player player, final Property property, final int price) {
        this.write("  " + player.getName() + " puo' comprare \"" + property.getName()
                + "\" per " + ViewStyle.formatMoney(price) + ": deve decidere");
    }

    @Override
    public void onPurchaseResolved(final Player player, final Property property, final boolean bought) {
        if (!bought) {
            this.write("  " + player.getName() + " lascia \"" + property.getName() + "\": resta libera");
        }
    }

    @Override
    public void onPropertyBought(final Player buyer, final Property property, final int price) {
        this.write("  " + buyer.getName() + " compra \"" + property.getName()
                + "\" per " + ViewStyle.formatMoney(price)
                + " (gli restano " + ViewStyle.formatMoney(buyer.getMoney()) + ")");
    }

    @Override
    public void onPropertySold(final Player seller, final Property property, final int price) {
        this.write("  " + seller.getName() + " rivende \"" + property.getName()
                + "\" alla banca per " + ViewStyle.formatMoney(price));
    }

    @Override
    public void onRentPaid(final Player tenant, final Player owner, final Property property, final int amount) {
        this.write("  " + tenant.getName() + " paga " + ViewStyle.formatMoney(amount) + " di affitto a "
                + owner.getName() + " per \"" + property.getName() + "\"");
    }

    @Override
    public void onMoneyPaidToBank(final Player player, final String reason, final int amount) {
        this.write("  " + player.getName() + " paga " + ViewStyle.formatMoney(amount) + " alla banca (" + reason + ")");
    }

    @Override
    public void onMoneyReceivedFromBank(final Player player, final String reason, final int amount) {
        this.write("  " + player.getName() + " incassa " + ViewStyle.formatMoney(amount) + " dalla banca (" + reason + ")");
    }

    @Override
    public void onPlayerReleasedFromJail(final Player player, final String reason) {
        this.write("  " + player.getName() + " esce di prigione (" + reason + ")");
    }

    @Override
    public void onPlayerBankrupt(final Player player, final Optional<Player> creditor) {
        this.write("  " + player.getName() + " e' fallito: cede tutto "
                + creditor.map(owner -> "a " + owner.getName()).orElse("alla banca") + " ed esce dalla partita");
    }

    @Override
    public void onGameOver(final Player winner) {
        this.write("");
        this.write("=== Partita finita: vince " + winner.getName() + " ===");
    }

    /**
     * Scrive la situazione patrimoniale di tutti i giocatori.
     * <p>
     * Non e' un evento: e' un riepilogo che l'applicazione puo' chiedere quando vuole
     * (per esempio a fine demo testuale). Lo stato dei
     * giocatori e' scritto con le stesse parole del pannello grafico
     * ({@link ViewStyle#describe(it.unibo.monopoly.model.player.PlayerStatus)}), non con il nome
     * della costante dell'enum.
     *
     * @param state lo stato della partita da riassumere
     */
    public void printStandings(final GameState state) {
        this.write("");
        this.write("=== Situazione ===");
        for (final Player player : state.getPlayers()) {
            this.write(String.format("  %-6s %7s  proprieta': %2d  stato: %s",
                    player.getName(), ViewStyle.formatMoney(player.getMoney()), player.getProperties().size(),
                    ViewStyle.describe(player.getStatus())));
        }
        this.write("  Cassa della banca: "
                + ViewStyle.formatMoney(state.getContext().getBank().getBalance()));
    }

    /**
     * Lo switch sull'enum e' esaustivo: se in futuro si aggiunge un nuovo esito,
     * il compilatore obbliga ad aggiornare anche questo metodo.
     */
    private static String describe(final RollOutcome outcome) {
        return switch (outcome) {
            case MOVED -> "";
            case ROLL_AGAIN -> " -> doppio, lancia di nuovo";
            case SENT_TO_JAIL -> " -> terzo doppio consecutivo";
            case RELEASED_FROM_JAIL -> " -> doppio, esce di prigione";
            case STAYED_IN_JAIL -> " -> niente doppio, resta in prigione";
            case RELEASED_ON_BAIL -> " -> tentativi esauriti, paga la cauzione";
        };
    }
}
