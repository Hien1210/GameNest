package com.gamenest.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Server-side gate for pages that require an authenticated session.
 * Client-side checks (hidden links, JS) are never sufficient on their own.
 */
@WebFilter(urlPatterns = {
        "/account/home.jsp",
        "/account/profile",
        "/account/profile/games",
        "/account/settings",
        "/account/settings/*",
        "/questions/new",
        "/questions/edit",
        "/questions/delete",
        "/answers/*",
        "/reports/create",
        "/lfg/new",
        "/lfg/join",
        "/lfg/leave",
        "/lfg/edit",
        "/lfg/close",
        "/lfg/delete",
        "/account/notifications",
        "/account/notifications/count",
        "/account/notifications/read",
        "/account/notifications/read-all",
        "/account/notifications/open",
        "/account/follow",
        "/account/unfollow",
        "/account/friend/request",
        "/account/friend/accept",
        "/account/friend/reject",
        "/account/friend/cancel",
        "/account/friend/unfriend",
        "/account/friends",
        "/account/friends/requests",
        "/account/block",
        "/account/unblock",
        "/account/blocked",
        "/account/teams",
        "/account/teams/invitations",
        "/account/team/detail",
        "/account/team/create",
        "/account/team/invite",
        "/account/team/invite/accept",
        "/account/team/invite/reject",
        "/account/team/invite/cancel",
        "/account/team/leave",
        "/account/team/remove-member",
        "/account/team/transfer-owner",
        "/account/team/delete",
        "/account/chats",
        "/account/chat/direct",
        "/account/chat/team",
        "/account/chat/detail",
        "/account/chat/send",
        "/account/chat/edit",
        "/account/chat/delete",
        "/account/chat/read"
})
public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }

        chain.doFilter(req, res);
    }
}
