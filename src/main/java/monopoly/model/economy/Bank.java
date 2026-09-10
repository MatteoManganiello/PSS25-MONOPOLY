package monopoly.model.economy;

import monopoly.model.player.Player;

/**
 * Banca del gioco: unico punto del modello in cui il denaro viene creato,
 * distrutto o trasferito da e verso i giocatori.
 * <p>
 * Centralizzare qui i movimenti di denaro evita che ogni casella o ogni schermata
 * manipoli direttamente il saldo dei giocatori: le regole di validazione (importi
 * positivi, fondi sufficienti) stanno tutte in un posto solo.
 * <p>
 * Come nel gioco reale, la banca non puo' fallire: il saldo viene tenuto solo per
 * tenere traccia del denaro in circolazione, ma un pagamento verso un giocatore
 * viene sempre onorato.
 */
public class Bank {

    /** Capitale iniziale assegnato a ogni giocatore. */
    public static final int STARTING_BALANCE = 1500;

    /** Somma che la banca versa a chi passa dal "Via". */
    public static final int GO_SALARY = 200;

    /** Fondi iniziali della banca (valore convenzionale, solo per contabilita'). */
    private static final int INITIAL_FUNDS = 20_580;

    private int balance;

    /** Crea la banca con i fondi iniziali di default. */
    public Bank() {
        this.balance = INITIAL_FUNDS;
    }

    /** @return il denaro attualmente in cassa alla banca */
    public int getBalance() {
        return this.balance;
    }

    /**
     * Versa denaro dalla banca a un giocatore (stipendio del "Via", vincite, ...).
     *
     * @param player il giocatore che riceve il denaro
     * @param amount importo da versare, strettamente positivo
     * @throws IllegalArgumentException se il giocatore e' null o l'importo non e' positivo
     */
    public void pay(final Player player, final int amount) {
        this.checkArguments(player, amount);
        player.setMoney(player.getMoney() + amount);
        this.balance -= amount;
    }

    /**
     * Preleva denaro da un giocatore e lo versa alla banca (tasse, acquisti, multe).
     * <p>
     * Se il giocatore non ha fondi sufficienti l'operazione non viene eseguita e il
     * metodo restituisce {@code false}: la gestione del fallimento (vendita di case,
     * ipoteche, uscita dalla partita) sara' responsabilita' del controller.
     *
     * @param player il giocatore che paga
     * @param amount importo da prelevare, strettamente positivo
     * @return true se il pagamento e' andato a buon fine
     * @throws IllegalArgumentException se il giocatore e' null o l'importo non e' positivo
     */
    public boolean charge(final Player player, final int amount) {
        this.checkArguments(player, amount);
        if (!this.canAfford(player, amount)) {
            return false;
        }
        player.setMoney(player.getMoney() - amount);
        this.balance += amount;
        return true;
    }

    /**
     * @param player il giocatore da controllare
     * @param amount l'importo richiesto
     * @return true se il giocatore ha abbastanza denaro liquido per pagare
     */
    public boolean canAfford(final Player player, final int amount) {
        if (player == null) {
            throw new IllegalArgumentException("Il giocatore non puo' essere null");
        }
        return player.getMoney() >= amount;
    }

    /** Controlli comuni a tutti i movimenti di denaro. */
    private void checkArguments(final Player player, final int amount) {
        if (player == null) {
            throw new IllegalArgumentException("Il giocatore non puo' essere null");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("L'importo di un'operazione bancaria deve essere positivo");
        }
    }

    @Override
    public String toString() {
        return "Bank[balance=" + this.balance + "]";
    }
}
