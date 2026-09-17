package it.unibo.monopoly.model.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import it.unibo.monopoly.model.game.GameState;

/**
 * Test sull'elenco delle pedine disponibili: sono le pedine che la schermata di setup
 * propone, quindi devono bastare per tutti ed essere tutte distinguibili.
 */
class TokenCatalogTest {

    @Test
    void thereIsATokenForEveryPlayerEvenAtAFullTable() {
        assertTrue(TokenCatalog.availableTokens().size() >= GameState.MAX_PLAYERS,
                "servono almeno " + GameState.MAX_PLAYERS + " pedine");
        assertEquals(TokenCatalog.SIZE, TokenCatalog.availableTokens().size());
    }

    @Test
    void everyTokenIsDifferentFromTheOthers() {
        final List<Token> tokens = TokenCatalog.availableTokens();

        assertEquals(tokens.size(), new HashSet<>(tokens).size(), "due pedine uguali");
        // Anche i colori devono essere tutti diversi: sul tabellone si distinguono da quelli.
        assertEquals(tokens.size(), tokens.stream().map(Token::getColor).distinct().count(),
                "due pedine dello stesso colore");
    }

    @Test
    void theProposedTokensAreTheOnesOfTheCatalogInOrder() {
        for (int index = 0; index < TokenCatalog.SIZE; index++) {
            assertEquals(TokenCatalog.availableTokens().get(index), TokenCatalog.defaultTokenFor(index));
        }
    }

    @Test
    void thereIsNoTokenOutsideTheCatalog() {
        assertThrows(IllegalArgumentException.class, () -> TokenCatalog.defaultTokenFor(-1));
        assertThrows(IllegalArgumentException.class, () -> TokenCatalog.defaultTokenFor(TokenCatalog.SIZE));
    }

    @Test
    void theCatalogCannotBeChangedFromOutside() {
        assertThrows(UnsupportedOperationException.class,
                () -> TokenCatalog.availableTokens().add(new Token("Intruso", "PINK")));
    }
}
