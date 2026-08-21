package com.gamenest.service;

import com.gamenest.dao.ConversationDAO;
import com.gamenest.dao.ConversationMemberDAO;
import com.gamenest.dao.MessageDAO;
import com.gamenest.dao.MessageReactionDAO;
import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.ConversationNotFoundException;
import com.gamenest.exception.DuplicateConversationException;
import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.MessageNotFoundException;
import com.gamenest.exception.TeamNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Account;
import com.gamenest.model.AccountFriendship;
import com.gamenest.model.Conversation;
import com.gamenest.model.ConversationMember;
import com.gamenest.model.ConversationStatus;
import com.gamenest.model.ConversationType;
import com.gamenest.model.FriendshipStatus;
import com.gamenest.model.Message;
import com.gamenest.model.MessageReactionSummary;
import com.gamenest.model.ReactionResult;
import com.gamenest.model.Team;
import com.gamenest.model.TeamMember;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Chat core logic — the single authority for every Chat business decision,
 * called by both the HTTP Servlets (ChatSendServlet etc.) and the WebSocket
 * realtime transport ({@code com.gamenest.websocket.ChatWebSocketEndpoint}).
 * One Conversation architecture for both DIRECT and TEAM chat, exactly as
 * specified — no separate DirectMessage/TeamMessage tables. Reuses
 * {@link AccountFriendService#findActiveBetween}/{@link AccountBlockService#isBlockedBetween}
 * and {@link TeamService#getTeam}/{@link TeamService#isMember} as-is for
 * every Friend/Block/Team rule — no relationship logic is duplicated here.
 * The WebSocket layer never re-implements any of this: it authenticates the
 * connection and calls exactly the same methods as the Servlets.
 */
public class ChatService {

    private static final int PAGE_SIZE = 20;
    private static final int MESSAGE_MAX_LENGTH = 2000;

    // Must match db/22_chat_reaction.sql's CK_MessageReactions_emoji
    // allowlist exactly — the DB CHECK constraint is the real defense
    // (CLAUDE.md §9), this is a fast-fail before ever hitting the DB.
    private static final Set<String> REACTION_EMOJI_ALLOWLIST = new HashSet<>(
            Arrays.asList("👍", "❤️", "😂", "😮", "😢", "😡"));

    private final ConversationDAO conversationDAO;
    private final ConversationMemberDAO conversationMemberDAO;
    private final MessageDAO messageDAO;
    private final MessageReactionDAO messageReactionDAO;
    private final AccountService accountService;
    private final AccountFriendService accountFriendService;
    private final AccountBlockService accountBlockService;
    private final TeamService teamService;

    public ChatService() {
        this.conversationDAO = new ConversationDAO();
        this.conversationMemberDAO = new ConversationMemberDAO();
        this.messageDAO = new MessageDAO();
        this.messageReactionDAO = new MessageReactionDAO();
        this.accountService = new AccountService();
        this.accountFriendService = new AccountFriendService();
        this.accountBlockService = new AccountBlockService();
        this.teamService = new TeamService();
    }

    // ---- Conversation resolution / access ----

    /** Excludes non-ACTIVE conversations (mirrors TeamService#getTeam/LFGService#getLfg). */
    public Conversation getConversation(int conversationId) throws ConversationNotFoundException, SQLException {
        Conversation conversation = conversationDAO.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Cuộc trò chuyện không tồn tại."));
        if (!ConversationStatus.ACTIVE.equals(conversation.getStatus())) {
            throw new ConversationNotFoundException("Cuộc trò chuyện không tồn tại.");
        }
        return conversation;
    }

    /**
     * Full access check (task spec §13/§22): conversation exists+ACTIVE,
     * caller is a current ConversationMember, and for TEAM conversations
     * the parent Team is re-verified ACTIVE and the caller re-verified as a
     * current TeamMember (defense in depth — never trusts the
     * ConversationMembers row alone for TEAM access).
     */
    public Conversation getAccessibleConversation(int conversationId, int accountId)
            throws ConversationNotFoundException, ForbiddenException, SQLException {

        Conversation conversation = getConversation(conversationId);
        conversationMemberDAO.findByConversationAndAccount(conversationId, accountId)
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập cuộc trò chuyện này."));

        if (ConversationType.TEAM.equals(conversation.getType())) {
            try {
                teamService.getTeam(conversation.getTeamId());
            } catch (TeamNotFoundException e) {
                throw new ConversationNotFoundException("Nhóm không còn tồn tại.");
            }
            if (!teamService.isMember(conversation.getTeamId(), accountId)) {
                throw new ForbiddenException("Bạn không phải thành viên của nhóm này.");
            }
        }
        return conversation;
    }

    /**
     * Direct Chat open (task spec §3/§4/§21): resolves target by username
     * (exists+ACTIVE, same as Follow/Friend/Block/Team's own convention),
     * self-chat denied, Friend required (ACCEPTED), Block denied (either
     * direction, higher priority than Friend). The canonical direct_key
     * (min:max account_id) plus the UNIQUE constraint on it is the actual
     * race guard — a concurrent duplicate create attempt fails with
     * {@link DuplicateConversationException} and this method re-fetches
     * the winner instead of erroring, so A→open and B→open racing each
     * other both resolve to the same Conversation.
     */
    public Conversation openDirectChat(int accountId, String targetUsername)
            throws ValidationException, AccountNotFoundException, SQLException {

        Account target = accountService.getPublicProfile(targetUsername);
        int targetAccountId = target.getAccountId();
        if (targetAccountId == accountId) {
            throw new ValidationException("Bạn không thể trò chuyện với chính mình.");
        }

        requireFriendAndNotBlocked(accountId, targetAccountId);

        String directKey = canonicalDirectKey(accountId, targetAccountId);
        Optional<Conversation> existing = conversationDAO.findByDirectKey(directKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Conversation conversation = conversationDAO.insertDirect(conn, directKey);
                conversationMemberDAO.insert(conn, conversation.getConversationId(), accountId);
                conversationMemberDAO.insert(conn, conversation.getConversationId(), targetAccountId);
                conn.commit();
                return conversation;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } catch (DuplicateConversationException e) {
                conn.rollback();
                // Race: the other side (or a concurrent duplicate request)
                // created it first between our SELECT and INSERT — the
                // UNIQUE constraint on direct_key is what actually
                // prevented the duplicate; just return the winner.
                return conversationDAO.findByDirectKey(directKey)
                        .orElseThrow(() -> new SQLException("Duplicate direct conversation race but no row found", e));
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Team Chat open (task spec §5/§22/§23): caller must be a current
     * TeamMember of an ACTIVE Team. Self-heals a missing Conversation
     * (only possible for a Team created before Chat existed) by creating
     * it and backfilling ConversationMembers for every CURRENT TeamMembers
     * row, atomically — never creates a second Conversation for a Team
     * that already has one (UQ_Conversations_team is the actual race
     * guard; a concurrent duplicate self-heal fails with
     * {@link DuplicateConversationException} and this re-fetches the
     * winner, same pattern as {@link #openDirectChat}).
     */
    public Conversation openTeamChat(int teamId, int accountId) throws TeamNotFoundException, ForbiddenException, SQLException {
        teamService.getTeam(teamId);
        if (!teamService.isMember(teamId, accountId)) {
            throw new ForbiddenException("Bạn không phải thành viên của nhóm này.");
        }

        Optional<Conversation> existing = conversationDAO.findByTeamId(teamId);
        if (existing.isPresent()) {
            return existing.get();
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Conversation conversation = conversationDAO.insertTeam(conn, teamId);
                for (TeamMember member : teamService.listMembersForChatBackfill(conn, teamId)) {
                    conversationMemberDAO.insert(conn, conversation.getConversationId(), member.getAccountId());
                }
                conn.commit();
                return conversation;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } catch (DuplicateConversationException e) {
                conn.rollback();
                return conversationDAO.findByTeamId(teamId)
                        .orElseThrow(() -> new SQLException("Duplicate team conversation race but no row found", e));
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ---- Messages ----

    /**
     * Send flow (task spec §13/§14): conversation exists+ACTIVE, caller is
     * a current member, then DIRECT→Friend+Block check or TEAM→Team
     * ACTIVE+membership check, then content validation, then INSERT. Used
     * identically by {@code ChatSendServlet} (HTTP) and
     * {@code ChatWebSocketEndpoint} (WebSocket) — the sole authorization
     * chain for sending a message, regardless of transport.
     * <p>
     * The returned {@link Message} is re-fetched by id after insert (one
     * extra indexed PK lookup) so it carries sender_username/display_name/
     * avatar_url and created_at — {@code messageDAO.insert} alone doesn't
     * populate those. This is a pure enrichment of the return value, not a
     * signature change: {@code ChatSendServlet} has always discarded the
     * return value, so HTTP behavior is unaffected; the WebSocket endpoint
     * is the first caller that actually needs the full row, to build the
     * MESSAGE_CREATED broadcast payload.
     * <p>
     * Reply feature (task spec §7/§8): {@code replyToMessageId} is null for
     * a normal message. When non-null it is validated here — the sole
     * authorization chain for both HTTP and WebSocket — after the existing
     * membership/Friend-Block/Team checks and before content validation, so
     * a caller who isn't even allowed to send to this conversation never
     * reaches the reply-target check. The target must exist and belong to
     * this exact conversation; cross-conversation targets are rejected.
     * Self-reply (replying to your own prior message) is explicitly valid —
     * no sender-mismatch check. A soft-deleted target is still a valid
     * reply target (the row still exists via the FK); the UI renders a
     * "deleted" placeholder from {@code replyToDeletedAt} instead of
     * rejecting the reply.
     */
    public Message sendMessage(int conversationId, int accountId, String content, Integer replyToMessageId)
            throws ConversationNotFoundException, ForbiddenException, ValidationException, SQLException {

        Conversation conversation = requireSendAccess(conversationId, accountId);

        if (replyToMessageId != null) {
            Message replyTarget = messageDAO.findById(replyToMessageId)
                    .orElseThrow(() -> new ValidationException("Tin nhắn được trả lời không tồn tại."));
            if (replyTarget.getConversationId() != conversationId) {
                throw new ValidationException("Tin nhắn được trả lời không thuộc cuộc trò chuyện này.");
            }
        }

        String normalizedContent = content == null ? null : content.trim();
        validateContent(normalizedContent);

        Message inserted = messageDAO.insert(conversationId, accountId, normalizedContent, replyToMessageId);
        return messageDAO.findById(inserted.getMessageId())
                .orElseThrow(() -> new SQLException("Message not found immediately after insert: " + inserted.getMessageId()));
    }

    /**
     * The exact authorization chain a caller must pass to write into a
     * conversation — conversation exists+ACTIVE, caller is a current
     * member, then DIRECT→Friend+Block check or TEAM→Team ACTIVE+membership
     * check. Extracted out of {@link #sendMessage} (Message Reaction task,
     * Decision 5) so {@link #toggleReaction} can require the exact same
     * access as sending a message — a single source of truth instead of two
     * copies that could silently drift apart. {@link #sendMessage}'s
     * observable behavior/exception types/ordering are unchanged: this is
     * the same code that used to be inline, only extracted.
     */
    private Conversation requireSendAccess(int conversationId, int accountId)
            throws ConversationNotFoundException, ForbiddenException, ValidationException, SQLException {

        Conversation conversation = getConversation(conversationId);
        conversationMemberDAO.findByConversationAndAccount(conversationId, accountId)
                .orElseThrow(() -> new ForbiddenException("Bạn không phải thành viên của cuộc trò chuyện này."));

        if (ConversationType.DIRECT.equals(conversation.getType())) {
            Account other = conversationMemberDAO.findOtherDirectParticipant(conversationId, accountId)
                    .orElseThrow(() -> new ConversationNotFoundException("Cuộc trò chuyện không hợp lệ."));
            requireFriendAndNotBlocked(accountId, other.getAccountId());
        } else {
            try {
                teamService.getTeam(conversation.getTeamId());
            } catch (TeamNotFoundException e) {
                throw new ConversationNotFoundException("Nhóm không còn tồn tại.");
            }
            if (!teamService.isMember(conversation.getTeamId(), accountId)) {
                throw new ForbiddenException("Bạn không phải thành viên của nhóm này.");
            }
        }
        return conversation;
    }

    /**
     * Toggle accountId's reaction on messageId to emoji (Message Reaction
     * task, Decision 3/4/5): ADD if the account has no current reaction,
     * CHANGE if it has a different one, REMOVE if it already equals emoji —
     * decided solely from the DB's current state via
     * {@link MessageReactionDAO#toggle}, never from a client-declared
     * intent. Reuses {@link #requireSendAccess} so reacting requires exactly
     * the same access as sending a message to the same conversation (same
     * membership + Friend/Block + Team checks, single source of truth).
     * <p>
     * Rejects with {@link ValidationException} for a soft-deleted target
     * message (Decision 4) — no new reaction, no change, no removal once
     * {@code deleted_at} is set; any reactions already on that message stay
     * untouched in the DB (only hidden from the UI, per the JSP/JS layer).
     * <p>
     * The whole read-current/mutate/re-aggregate sequence runs on one
     * transaction so the returned aggregate is always consistent with the
     * mutation that was just applied.
     */
    public ReactionResult toggleReaction(int messageId, int accountId, String emoji)
            throws MessageNotFoundException, ConversationNotFoundException, ForbiddenException, ValidationException, SQLException {

        if (emoji == null || !REACTION_EMOJI_ALLOWLIST.contains(emoji)) {
            throw new ValidationException("Biểu tượng cảm xúc không hợp lệ.");
        }

        Message target = messageDAO.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Tin nhắn không tồn tại."));

        requireSendAccess(target.getConversationId(), accountId);

        if (target.getDeletedAt() != null) {
            throw new ValidationException("Không thể thả cảm xúc lên tin nhắn đã bị xóa.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String myReaction = messageReactionDAO.toggle(conn, messageId, accountId, emoji);
                List<MessageReactionSummary> reactions = messageReactionDAO.aggregateForMessage(conn, messageId);
                conn.commit();
                return new ReactionResult(target.getConversationId(), messageId, myReaction, reactions);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * One recipient's own current reaction on a message — used by the
     * WebSocket layer to build each REACTION_UPDATED broadcast target's
     * individual {@code myReaction} (Decision 3): unlike the shared
     * {@code reactions} aggregate, this value is per-recipient by design,
     * since each account may have a different (or no) reaction on the same
     * message. No access check here — the caller already resolved this
     * recipient list from an authorized broadcast (see
     * {@code ChatWebSocketEndpoint}), same trust boundary as every other
     * broadcast-building step in that class.
     */
    public String getMyReaction(int messageId, int accountId) throws SQLException {
        return messageReactionDAO.findEmoji(messageId, accountId);
    }

    /** Sender-only, guarded UPDATE (task spec §15) — never re-checks Friend/Block/Team membership, matching Answer edit's own-content-only precedent. */
    public void editMessage(int messageId, int accountId, String newContent) throws MessageNotFoundException, ValidationException, SQLException {
        String normalizedContent = newContent == null ? null : newContent.trim();
        validateContent(normalizedContent);
        int updated = messageDAO.editIfSender(messageId, accountId, normalizedContent);
        if (updated == 0) {
            throw new MessageNotFoundException("Tin nhắn không tồn tại, không thuộc về bạn, hoặc đã bị xóa.");
        }
    }

    /** Sender-only, guarded soft-delete UPDATE (task spec §16) — never a hard DELETE. */
    public void deleteMessage(int messageId, int accountId) throws MessageNotFoundException, SQLException {
        int updated = messageDAO.softDeleteIfSender(messageId, accountId);
        if (updated == 0) {
            throw new MessageNotFoundException("Tin nhắn không tồn tại, không thuộc về bạn, hoặc đã bị xóa.");
        }
    }

    public List<Message> listMessages(int conversationId, int accountId, int page)
            throws ConversationNotFoundException, ForbiddenException, SQLException {
        getAccessibleConversation(conversationId, accountId);
        int offset = (clampPage(page) - 1) * PAGE_SIZE;
        List<Message> messages = messageDAO.listByConversation(conversationId, offset, PAGE_SIZE);
        enrichWithReactions(messages, accountId);
        return messages;
    }

    /**
     * Batch-attaches {@code reactions}/{@code myReaction} onto a page of
     * messages (Message Reaction task) — two queries total (aggregate +
     * "my reaction"), never one query per message. Reactions are not part
     * of {@link MessageDAO}'s own SELECT/JOIN because the relationship is
     * 1-to-many per message (a GROUP BY join would multiply message rows),
     * unlike the 1:1 sender/reply-target joins MessageDAO already performs.
     */
    private void enrichWithReactions(List<Message> messages, int accountId) throws SQLException {
        if (messages.isEmpty()) {
            return;
        }
        List<Integer> messageIds = new ArrayList<>(messages.size());
        for (Message message : messages) {
            messageIds.add(message.getMessageId());
        }
        Map<Integer, List<MessageReactionSummary>> reactionsByMessage = messageReactionDAO.aggregateForMessages(messageIds);
        Map<Integer, String> myReactionByMessage = messageReactionDAO.myReactionForMessages(messageIds, accountId);
        for (Message message : messages) {
            message.setReactions(reactionsByMessage.getOrDefault(message.getMessageId(), List.of()));
            message.setMyReaction(myReactionByMessage.get(message.getMessageId()));
        }
    }

    public int countMessages(int conversationId, int accountId)
            throws ConversationNotFoundException, ForbiddenException, SQLException {
        getAccessibleConversation(conversationId, accountId);
        return messageDAO.countByConversation(conversationId);
    }

    /**
     * Mark read (task spec §17, Chat Core; return value added for Realtime
     * Read/Seen task §7): the real access check (membership,
     * Team-still-ACTIVE) is a hard failure; the messageId/regression guard
     * inside {@link ConversationMemberDAO#markRead} is a silent no-op on 0
     * rows ("fail safely", never an error) since an invalid or stale
     * messageId here is low-stakes (only affects the unread badge).
     * <p>
     * Returns the DAO's affected-row count (0 or 1) so a caller can tell
     * whether the read state actually advanced — used by
     * {@code ChatWebSocketEndpoint} to decide whether a READ_UPDATED
     * broadcast is warranted. This is a pure return-value enrichment, not a
     * new method: {@code ChatReadServlet} already calls this as a bare
     * statement and discards the result, so the change from {@code void} is
     * source- and behavior-compatible with the existing HTTP caller.
     */
    public int markAsRead(int conversationId, int accountId, int messageId)
            throws ConversationNotFoundException, ForbiddenException, SQLException {
        getAccessibleConversation(conversationId, accountId);
        return conversationMemberDAO.markRead(conversationId, accountId, messageId);
    }

    /** Display helper for Chat Detail's header (task spec §17/§26 — "tên người/team") — the counterpart of a DIRECT conversation, public columns only. */
    public Optional<Account> getOtherDirectParticipant(int conversationId, int accountId) throws SQLException {
        return conversationMemberDAO.findOtherDirectParticipant(conversationId, accountId);
    }

    /**
     * Realtime broadcast targets (WebSocket transport layer only — a
     * delivery-list lookup, not a new authorization decision). For DIRECT,
     * the conversation's other participant; for TEAM, every CURRENT
     * TeamMember — reuses {@link TeamService#listMembers} as-is, which
     * re-verifies the Team is ACTIVE and {@code accountId} is still a
     * member, so nobody who has Left/been Removed is ever returned and no
     * Team authorization logic is duplicated here.
     */
    public List<Integer> getRecipientAccountIds(Conversation conversation, int accountId)
            throws TeamNotFoundException, ForbiddenException, SQLException {
        if (ConversationType.DIRECT.equals(conversation.getType())) {
            Optional<Account> other = conversationMemberDAO.findOtherDirectParticipant(conversation.getConversationId(), accountId);
            if (other.isEmpty()) {
                return List.of();
            }
            return List.of(other.get().getAccountId());
        }
        List<TeamMember> members = teamService.listMembers(conversation.getTeamId(), accountId);
        List<Integer> accountIds = new ArrayList<>(members.size());
        for (TeamMember member : members) {
            accountIds.add(member.getAccountId());
        }
        return accountIds;
    }

    /**
     * Typing Indicator recipient resolution (Typing Indicator task spec
     * §6/§7/§11) — deliberately not a bare "conversationId → find
     * recipient → broadcast": goes through the exact same authorization
     * chain as every other Chat operation. Reuses
     * {@link #getAccessibleConversation} for existence/ACTIVE/current
     * membership (and, for TEAM, Team-ACTIVE+membership), then the same
     * private {@link #requireFriendAndNotBlocked} helper {@link #sendMessage}
     * itself uses — no Friend/Block rule is duplicated. Does not touch
     * {@link #sendMessage} or change its behavior in any way.
     * <p>
     * TEAM conversations are out of scope for Typing Indicator by design
     * (task spec §3/§16): returns {@code null} rather than throwing, so the
     * caller can silently ignore a TYPING_START/STOP for a TEAM conversation
     * — not an error, just "no recipient to notify."
     *
     * @return the single DIRECT counterpart's account id to notify, or
     * {@code null} if the conversation is TEAM (out of scope) or has no
     * resolvable other participant.
     * @throws ConversationNotFoundException conversation missing/inactive
     * @throws ForbiddenException caller is not a current member
     * @throws ValidationException caller and the counterpart are not
     * ACCEPTED friends, or either has blocked the other
     */
    public Integer getTypingRecipient(int conversationId, int accountId)
            throws ConversationNotFoundException, ForbiddenException, ValidationException, SQLException {

        Conversation conversation = getAccessibleConversation(conversationId, accountId);
        if (!ConversationType.DIRECT.equals(conversation.getType())) {
            return null;
        }

        Optional<Account> other = conversationMemberDAO.findOtherDirectParticipant(conversationId, accountId);
        if (other.isEmpty()) {
            return null;
        }

        requireFriendAndNotBlocked(accountId, other.get().getAccountId());
        return other.get().getAccountId();
    }

    // ---- Chat list ----

    public List<Conversation> listMyConversations(int accountId, int page) throws SQLException {
        int offset = (clampPage(page) - 1) * PAGE_SIZE;
        return conversationDAO.listMyConversations(accountId, offset, PAGE_SIZE);
    }

    public int countMyConversations(int accountId) throws SQLException {
        return conversationDAO.countMyConversations(accountId);
    }

    public int countUnreadConversations(int accountId) throws SQLException {
        return conversationDAO.countUnreadConversations(accountId);
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    // ---- Helpers ----

    /** Block has priority over Friend (task spec §3) — checked after Friend so the ValidationException message never has to disambiguate which failed first, matching the generic non-leaking style already used by Team's own invite guard. */
    private void requireFriendAndNotBlocked(int accountId, int otherAccountId) throws ValidationException, SQLException {
        Optional<AccountFriendship> friendship = accountFriendService.findActiveBetween(accountId, otherAccountId);
        if (friendship.isEmpty() || !FriendshipStatus.ACCEPTED.equals(friendship.get().getStatus())) {
            throw new ValidationException("Bạn chỉ có thể nhắn tin với bạn bè (Friend).");
        }
        if (accountBlockService.isBlockedBetween(accountId, otherAccountId)) {
            throw new ValidationException("Không thể nhắn tin với người dùng này.");
        }
    }

    private String canonicalDirectKey(int accountA, int accountB) {
        int lo = Math.min(accountA, accountB);
        int hi = Math.max(accountA, accountB);
        return lo + ":" + hi;
    }

    private void validateContent(String content) throws ValidationException {
        if (content == null || content.isEmpty()) {
            throw new ValidationException("Nội dung tin nhắn không được để trống.");
        }
        if (content.length() > MESSAGE_MAX_LENGTH) {
            throw new ValidationException("Nội dung tin nhắn không được vượt quá " + MESSAGE_MAX_LENGTH + " ký tự.");
        }
    }

    private int clampPage(int page) {
        return Math.max(page, 1);
    }
}
