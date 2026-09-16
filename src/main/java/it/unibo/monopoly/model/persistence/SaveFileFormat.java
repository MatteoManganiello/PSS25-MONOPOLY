package it.unibo.monopoly.model.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import it.unibo.monopoly.model.game.GamePhase;
import it.unibo.monopoly.model.game.GameState;
import it.unibo.monopoly.model.persistence.SaveFileException.Problem;
import it.unibo.monopoly.model.player.PlayerStatus;

/**
 * Formato del file di salvataggio: traduce un {@link SaveData} in coppie chiave-valore
 * {@link Properties} e viceversa.
 * <p>
 * <b>Perche' {@code .properties}.</b> E' un formato testuale che il JDK sa gia'
 * leggere e scrivere, quindi non serve nessuna libreria esterna, e {@link Properties}
 * si occupa da solo dei casi scomodi (spazi, accenti, caratteri speciali nei nomi dei
 * giocatori). Il file resta leggibile e modificabile con un editor di testo, cosa utile
 * per capire un salvataggio o prepararne uno a mano. Il prezzo e' che il formato e'
 * piatto, senza strutture annidate: gli elenchi si rappresentano con indici nelle
 * chiavi ({@code player.0.name}, {@code player.1.name}, ...) e ogni valore va convertito
 * e controllato a mano, cosa che questa classe fa in un posto solo.
 * <p>
 * <b>Il formato, versione {@value #VERSION}.</b> Esempio di un file con tre giocatori
 * (solo il primo e' riportato per intero):
 * <pre>
 * format.id=monopoly-pss25
 * format.version=1
 * game.bankBalance=20640
 * game.consecutiveDoubles=0
 * game.currentPlayerIndex=1
 * game.over=false
 * game.phase=ROLL
 * game.playerCount=3
 * player.0.jailAttempts=1
 * player.0.money=1440
 * player.0.name=Alice
 * player.0.position=10
 * player.0.properties=1,3
 * player.0.status=IN_JAIL
 * player.0.token.color=RED
 * player.0.token.name=Car
 * ...
 * </pre>
 * {@link Properties#store} scrive le chiavi in ordine alfabetico: i nomi sono scelti in
 * modo che, in quell'ordine, il file si legga dall'alto in basso (formato, partita,
 * giocatori). Fasi e stati sono scritti con il nome della costante dell'enum, le
 * proprieta' con la loro posizione sul tabellone.
 * <p>
 * <b>Stabilita'.</b> Le chiavi non si rinominano. Se un giorno il significato dei dati
 * dovesse cambiare si aumenta {@link #VERSION}: i file della versione precedente
 * vengono allora riconosciuti come incompatibili, invece di essere letti male.
 * <p>
 * Classe di sola utilita', visibile solo dentro il package: fuori si usano
 * {@link GameStateSaver} e {@link GameStateLoader}.
 */
final class SaveFileFormat {

    /** Identificativo scritto in ogni salvataggio, per riconoscerlo fra altri file di testo. */
    static final String FORMAT_ID = "monopoly-pss25";

    /** Versione del formato scritta e letta da questa versione del gioco. */
    static final int VERSION = 1;

    private static final String FORMAT_ID_KEY = "format.id";
    private static final String FORMAT_VERSION_KEY = "format.version";

    private static final String BANK_BALANCE_KEY = "game.bankBalance";
    private static final String CONSECUTIVE_DOUBLES_KEY = "game.consecutiveDoubles";
    private static final String CURRENT_PLAYER_KEY = "game.currentPlayerIndex";
    private static final String GAME_OVER_KEY = "game.over";
    private static final String PHASE_KEY = "game.phase";
    private static final String PLAYER_COUNT_KEY = "game.playerCount";

    private static final String NAME_FIELD = "name";
    private static final String TOKEN_NAME_FIELD = "token.name";
    private static final String TOKEN_COLOR_FIELD = "token.color";
    private static final String MONEY_FIELD = "money";
    private static final String POSITION_FIELD = "position";
    private static final String STATUS_FIELD = "status";
    private static final String PROPERTIES_FIELD = "properties";
    private static final String JAIL_ATTEMPTS_FIELD = "jailAttempts";

    /** Separatore delle posizioni nell'elenco delle proprieta'. */
    private static final String LIST_SEPARATOR = ",";

    /** Classe di utilita': non deve essere istanziata. */
    private SaveFileFormat() {
    }

    /**
     * Scrive la fotografia della partita come coppie chiave-valore.
     *
     * @param data i dati da salvare
     * @return le proprieta' pronte per essere scritte su file
     */
    static Properties toProperties(final SaveData data) {
        final Properties properties = new Properties();
        properties.setProperty(FORMAT_ID_KEY, FORMAT_ID);
        properties.setProperty(FORMAT_VERSION_KEY, String.valueOf(VERSION));

        properties.setProperty(BANK_BALANCE_KEY, String.valueOf(data.bankBalance()));
        properties.setProperty(CONSECUTIVE_DOUBLES_KEY, String.valueOf(data.consecutiveDoubles()));
        properties.setProperty(CURRENT_PLAYER_KEY, String.valueOf(data.currentPlayerIndex()));
        properties.setProperty(GAME_OVER_KEY, String.valueOf(data.gameOver()));
        properties.setProperty(PHASE_KEY, data.phase().name());
        properties.setProperty(PLAYER_COUNT_KEY, String.valueOf(data.players().size()));

        for (int index = 0; index < data.players().size(); index++) {
            final SaveData.PlayerData player = data.players().get(index);
            properties.setProperty(playerKey(index, NAME_FIELD), player.name());
            properties.setProperty(playerKey(index, TOKEN_NAME_FIELD), player.tokenName());
            properties.setProperty(playerKey(index, TOKEN_COLOR_FIELD), player.tokenColor());
            properties.setProperty(playerKey(index, MONEY_FIELD), String.valueOf(player.money()));
            properties.setProperty(playerKey(index, POSITION_FIELD), String.valueOf(player.position()));
            properties.setProperty(playerKey(index, STATUS_FIELD), player.status().name());
            properties.setProperty(playerKey(index, PROPERTIES_FIELD), player.propertyPositions().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(LIST_SEPARATOR)));
            properties.setProperty(playerKey(index, JAIL_ATTEMPTS_FIELD), String.valueOf(player.failedJailAttempts()));
        }
        return properties;
    }

    /**
     * Legge la fotografia della partita dalle coppie chiave-valore di un file.
     * <p>
     * Prima si controlla che il file sia davvero un salvataggio nella versione giusta
     * (altrimenti e' {@link Problem#INCOMPATIBLE}); poi che ogni dato ci sia e abbia un
     * valore del tipo atteso (altrimenti e' {@link Problem#CORRUPTED}).
     *
     * @param properties le coppie lette dal file
     * @return i dati del salvataggio
     * @throws SaveFileException se il file non e' compatibile o un dato manca o non e' valido
     */
    static SaveData fromProperties(final Properties properties) throws SaveFileException {
        checkFormat(properties);

        final int playerCount = readInt(properties, PLAYER_COUNT_KEY);
        // Il limite serve anche a non tentare di leggere milioni di giocatori da un file alterato.
        if (playerCount < GameState.MIN_PLAYERS || playerCount > GameState.MAX_PLAYERS) {
            throw corrupted("numero di giocatori non valido (" + playerCount + ")");
        }
        final List<SaveData.PlayerData> players = new ArrayList<>(playerCount);
        for (int index = 0; index < playerCount; index++) {
            players.add(readPlayer(properties, index));
        }
        return new SaveData(
                readInt(properties, BANK_BALANCE_KEY),
                readInt(properties, CURRENT_PLAYER_KEY),
                readEnum(properties, PHASE_KEY, GamePhase.class),
                readInt(properties, CONSECUTIVE_DOUBLES_KEY),
                readBoolean(properties, GAME_OVER_KEY),
                players);
    }

    /** Legge i dati del giocatore di indice {@code index}. */
    private static SaveData.PlayerData readPlayer(final Properties properties, final int index)
            throws SaveFileException {
        return new SaveData.PlayerData(
                readText(properties, playerKey(index, NAME_FIELD)),
                readText(properties, playerKey(index, TOKEN_NAME_FIELD)),
                readText(properties, playerKey(index, TOKEN_COLOR_FIELD)),
                readInt(properties, playerKey(index, MONEY_FIELD)),
                readInt(properties, playerKey(index, POSITION_FIELD)),
                readEnum(properties, playerKey(index, STATUS_FIELD), PlayerStatus.class),
                readPositions(properties, playerKey(index, PROPERTIES_FIELD)),
                readInt(properties, playerKey(index, JAIL_ATTEMPTS_FIELD)));
    }

    /** Verifica identificativo e versione del formato. */
    private static void checkFormat(final Properties properties) throws SaveFileException {
        if (!FORMAT_ID.equals(properties.getProperty(FORMAT_ID_KEY))) {
            throw new SaveFileException(Problem.INCOMPATIBLE, "il file non e' un salvataggio di Monopoly");
        }
        final String version = properties.getProperty(FORMAT_VERSION_KEY, "").trim();
        if (!String.valueOf(VERSION).equals(version)) {
            throw new SaveFileException(Problem.INCOMPATIBLE, "versione del formato '" + version
                    + "' non supportata (questo gioco legge la versione " + VERSION + ")");
        }
    }

    /** @return la chiave di un dato del giocatore, per esempio {@code player.0.money} */
    private static String playerKey(final int index, final String field) {
        return "player." + index + "." + field;
    }

    /** @return il valore della chiave, cosi' com'e' scritto */
    private static String readText(final Properties properties, final String key) throws SaveFileException {
        final String value = properties.getProperty(key);
        if (value == null) {
            throw corrupted("manca il dato '" + key + "'");
        }
        return value;
    }

    /** @return il valore della chiave come numero intero */
    private static int readInt(final Properties properties, final String key) throws SaveFileException {
        final String value = readText(properties, key).trim();
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            throw corrupted("il dato '" + key + "' dovrebbe essere un numero, invece vale '" + value + "'");
        }
    }

    /** @return il valore della chiave come booleano; sono accettati solo "true" e "false" */
    private static boolean readBoolean(final Properties properties, final String key) throws SaveFileException {
        final String value = readText(properties, key).trim();
        if (!"true".equals(value) && !"false".equals(value)) {
            throw corrupted("il dato '" + key + "' dovrebbe essere true o false, invece vale '" + value + "'");
        }
        return Boolean.parseBoolean(value);
    }

    /** @return il valore della chiave come costante dell'enum indicato */
    private static <E extends Enum<E>> E readEnum(final Properties properties, final String key,
                                                  final Class<E> type) throws SaveFileException {
        final String value = readText(properties, key).trim();
        try {
            return Enum.valueOf(type, value);
        } catch (final IllegalArgumentException e) {
            throw corrupted("valore sconosciuto per '" + key + "': '" + value + "'");
        }
    }

    /** @return le posizioni elencate nella chiave, separate da virgole; nessuna se il valore e' vuoto */
    private static List<Integer> readPositions(final Properties properties, final String key)
            throws SaveFileException {
        final String value = readText(properties, key).trim();
        if (value.isEmpty()) {
            return List.of();
        }
        final List<Integer> positions = new ArrayList<>();
        for (final String item : value.split(LIST_SEPARATOR, -1)) {
            try {
                positions.add(Integer.parseInt(item.trim()));
            } catch (final NumberFormatException e) {
                throw corrupted("elenco di proprieta' non valido in '" + key + "': '" + value + "'");
            }
        }
        return positions;
    }

    /** @return l'errore di un dato mancante o non valido */
    private static SaveFileException corrupted(final String detail) {
        return new SaveFileException(Problem.CORRUPTED, detail);
    }
}
