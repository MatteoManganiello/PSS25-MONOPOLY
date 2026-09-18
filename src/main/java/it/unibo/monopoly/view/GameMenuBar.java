package it.unibo.monopoly.view;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import it.unibo.monopoly.controller.GameEngine;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.persistence.PersistenceResult;
import it.unibo.monopoly.model.persistence.SaveFileException;

/**
 * Barra dei menu della finestra, con il menu "Partita": salvataggio e caricamento su
 * file.
 * <p>
 * <b>MVC.</b> Come il {@link ControlPanel}, raccoglie l'intenzione dell'utente (quale
 * file) e la passa al {@link GameEngine}: non sa come e' fatto un salvataggio e non
 * tocca il model. Dopo un caricamento non ridisegna nulla: e' il motore a notificare gli
 * osservatori, e la finestra si aggiorna come dopo qualunque altro comando. Da qui parte
 * solo il feedback all'utente: una finestra di dialogo con l'esito.
 * <p>
 * <b>Thread.</b> Vale la regola di tutta la GUI: i comandi al motore e gli aggiornamenti di
 * Swing avvengono sull'Event Dispatch Thread.
 * <ul>
 *   <li>Il <em>salvataggio</em> resta interamente sull'EDT: la fotografia della partita va
 *       presa fra un comando e l'altro, e i comandi girano proprio sull'EDT. Il file e' di
 *       poche righe, quindi l'interfaccia non si blocca.</li>
 *   <li>Il <em>caricamento</em> e' diviso in due: la lettura del file
 *       ({@link GameEngine#readSavedGame(Path)}) gira in un thread in background, perche'
 *       lavora sul disco e non tocca ne' Swing ne' la partita in corso; la messa in gioco
 *       della partita ({@link GameEngine#resumeGame(GameState)}), che fa partire le
 *       notifiche, e il messaggio all'utente tornano sull'EDT con
 *       {@link SwingUtilities#invokeLater(Runnable)}.</li>
 * </ul>
 * <p>
 * La classe e' {@code final}: e' un componente grafico concreto, non un punto di
 * estensione, e dichiararlo esplicitamente permette al costruttore di configurarsi senza
 * il rischio di chiamare metodi ridefiniti da una sottoclasse non ancora inizializzata.
 */
public final class GameMenuBar extends JMenuBar {

    /** Vedi {@link TilePanel#serialVersionUID}. */
    private static final long serialVersionUID = 1L;

    /** Estensione dei file di salvataggio: sono file di testo nel formato properties. */
    private static final String EXTENSION = "properties";

    /** Nome del thread che legge i salvataggi, utile quando si osservano i thread attivi. */
    private static final String LOADER_THREAD_NAME = "monopoly-load";

    private final transient GameEngine engine;

    /** Componente sopra cui aprire file chooser e finestre di dialogo. */
    private final Component dialogParent;

    /** Unico per tutta la finestra: ricorda l'ultima cartella usata. */
    private final JFileChooser fileChooser;

    private final JMenuItem saveItem;
    private final JMenuItem loadItem;

    /**
     * Crea la barra dei menu collegata al motore della partita.
     *
     * @param engine       il motore a cui inoltrare salvataggi e caricamenti
     * @param dialogParent la finestra sopra cui mostrare file chooser e messaggi
     * @throws IllegalArgumentException se un parametro e' null
     */
    public GameMenuBar(final GameEngine engine, final Component dialogParent) {
        if (engine == null || dialogParent == null) {
            throw new IllegalArgumentException("Motore e finestra non possono essere null");
        }
        this.engine = engine;
        this.dialogParent = dialogParent;
        this.fileChooser = new JFileChooser();
        this.fileChooser.setFileFilter(
                new FileNameExtensionFilter("Partite di Monopoly (*." + EXTENSION + ")", EXTENSION));

        this.saveItem = new JMenuItem("Salva partita...");
        this.loadItem = new JMenuItem("Carica partita...");
        // Cmd+S / Cmd+O su macOS, Ctrl+S / Ctrl+O altrove.
        final int shortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        this.saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcut));
        this.loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcut));
        this.saveItem.addActionListener(event -> this.saveGame());
        this.loadItem.addActionListener(event -> this.loadGame());

        final JMenu gameMenu = new JMenu("Partita");
        gameMenu.add(this.saveItem);
        gameMenu.add(this.loadItem);
        this.add(gameMenu);
    }

    /** Chiede dove salvare e salva, sull'EDT (vedi la documentazione della classe). */
    private void saveGame() {
        this.chooseSaveFile().ifPresent(file -> this.report(this.engine.saveGame(file), "Salvataggio"));
    }

    /** Chiede quale partita caricare e ne avvia la lettura. */
    private void loadGame() {
        if (this.fileChooser.showOpenDialog(this.dialogParent) == JFileChooser.APPROVE_OPTION) {
            this.loadInBackground(this.fileChooser.getSelectedFile().toPath());
        }
    }

    /**
     * Carica la partita senza bloccare l'interfaccia.
     * <p>
     * Il thread in background esegue solo la lettura del file, che non tocca nulla di
     * condiviso; tutto il resto passa da {@link SwingUtilities#invokeLater(Runnable)}.
     * Nel frattempo i comandi del menu restano disabilitati, per non far partire due
     * caricamenti insieme o salvare la partita che sta per essere sostituita.
     */
    private void loadInBackground(final Path file) {
        this.setBusy(true);
        final Thread reader = new Thread(() -> {
            try {
                final GameState loaded = this.engine.readSavedGame(file);
                SwingUtilities.invokeLater(() -> this.completeLoad(loaded, file));
            } catch (final SaveFileException e) {
                SwingUtilities.invokeLater(() -> this.failLoad("Caricamento non riuscito: " + e.getMessage()));
            } catch (final RuntimeException e) {
                // Un errore inatteso non deve lasciare il menu disabilitato per sempre:
                // questo thread e' l'ultimo punto in cui si puo' ancora avvisare l'utente.
                SwingUtilities.invokeLater(() -> this.failLoad("Caricamento non riuscito: errore inatteso (" + e + ")"));
            }
        }, LOADER_THREAD_NAME);
        // Un caricamento in corso non deve impedire all'applicazione di chiudersi.
        reader.setDaemon(true);
        reader.start();
    }

    /** Sull'EDT: mette in gioco la partita letta; la finestra si ridisegna grazie alle notifiche. */
    private void completeLoad(final GameState loaded, final Path file) {
        this.setBusy(false);
        this.engine.resumeGame(loaded);
        this.report(PersistenceResult.success("Partita caricata da " + file.getFileName()), "Caricamento");
    }

    /** Sull'EDT: la partita in corso non e' cambiata, si mostra solo il motivo. */
    private void failLoad(final String message) {
        this.setBusy(false);
        this.report(PersistenceResult.failure(message), "Caricamento");
    }

    /**
     * Mostra il file chooser per il salvataggio.
     * <p>
     * Aggiunge l'estensione se l'utente non l'ha scritta e chiede conferma prima di
     * sovrascrivere un file esistente.
     *
     * @return il file scelto, oppure {@link Optional#empty()} se l'utente ha rinunciato
     */
    private Optional<Path> chooseSaveFile() {
        if (this.fileChooser.showSaveDialog(this.dialogParent) != JFileChooser.APPROVE_OPTION) {
            return Optional.empty();
        }
        final Path file = withExtension(this.fileChooser.getSelectedFile().toPath());
        if (Files.exists(file) && JOptionPane.showConfirmDialog(this.dialogParent,
                "Il file " + file.getFileName() + " esiste gia'. Sovrascriverlo?",
                "Salvataggio", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return Optional.empty();
        }
        return Optional.of(file);
    }

    /** Comunica l'esito all'utente con una finestra di dialogo, perche' non passi inosservato. */
    private void report(final PersistenceResult result, final String title) {
        JOptionPane.showMessageDialog(this.dialogParent, result.message(), title,
                result.successful() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    /** Disabilita i comandi del menu e mostra il cursore di attesa durante un caricamento. */
    private void setBusy(final boolean busy) {
        this.saveItem.setEnabled(!busy);
        this.loadItem.setEnabled(!busy);
        this.dialogParent.setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    /** @return il file con l'estensione dei salvataggi, aggiunta se manca */
    private static Path withExtension(final Path file) {
        final String name = file.getFileName().toString();
        return name.toLowerCase(Locale.ROOT).endsWith("." + EXTENSION)
                ? file
                : file.resolveSibling(name + "." + EXTENSION);
    }
}
