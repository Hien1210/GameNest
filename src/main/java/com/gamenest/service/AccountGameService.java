package com.gamenest.service;

import com.gamenest.dao.AccountGameDAO;
import com.gamenest.exception.DuplicateAccountGameException;
import com.gamenest.exception.GameNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.AccountGame;
import com.gamenest.model.AccountGameRelationshipType;

import java.sql.SQLException;
import java.util.List;

/**
 * Business rules for the User Profile "Playing Games" / "Favorite Games"
 * feature (AccountGames, schema from db/14_user_foundation.sql). Reuses
 * {@link GameService#getActiveGameDetail} as-is for target validation
 * (existence + ACTIVE check) instead of duplicating that logic here.
 */
public class AccountGameService {

    private final AccountGameDAO accountGameDAO;
    private final GameService gameService;

    public AccountGameService() {
        this.accountGameDAO = new AccountGameDAO();
        this.gameService = new GameService();
    }

    public AccountGameService(AccountGameDAO accountGameDAO, GameService gameService) {
        this.accountGameDAO = accountGameDAO;
        this.gameService = gameService;
    }

    public List<AccountGame> listByAccountAndType(int accountId, String relationshipType) throws SQLException {
        return accountGameDAO.findByAccountAndType(accountId, relationshipType);
    }

    /**
     * accountId must come from the caller's session — this method trusts
     * whatever int it is given, so callers must never source it from a
     * request parameter (task spec §7).
     */
    public void addRelationship(int accountId, int gameId, String relationshipType)
            throws GameNotFoundException, ValidationException, DuplicateAccountGameException, SQLException {

        validateRelationshipType(relationshipType);

        // Throws GameNotFoundException for a missing OR inactive game —
        // same rule the Games module already uses for hiding INACTIVE games.
        gameService.getActiveGameDetail(gameId);

        accountGameDAO.insert(accountId, gameId, relationshipType);
    }

    /**
     * Scoped to (accountId, gameId, relationshipType) at the DAO level —
     * a user can only ever remove their OWN relationship row. Removing a
     * relationship that isn't there is a harmless no-op, not an error.
     */
    public void removeRelationship(int accountId, int gameId, String relationshipType)
            throws ValidationException, SQLException {
        validateRelationshipType(relationshipType);
        accountGameDAO.delete(accountId, gameId, relationshipType);
    }

    private void validateRelationshipType(String relationshipType) throws ValidationException {
        if (!AccountGameRelationshipType.PLAYING.equals(relationshipType)
                && !AccountGameRelationshipType.FAVORITE.equals(relationshipType)) {
            throw new ValidationException("Loại quan hệ không hợp lệ.");
        }
    }
}
