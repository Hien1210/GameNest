<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.gamenest.model.Game" %>
<%@ page import="com.gamenest.model.GameStatus" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<% request.setAttribute("pageTitle", "Quản lý trò chơi"); %>
<%@ include file="/WEB-INF/admin/layout/header.jsp" %>

<div class="admin-page-header">
    <h1>Quản lý trò chơi</h1>
    <a class="admin-btn" href="${pageContext.request.contextPath}/admin/games/new">+ Thêm trò chơi</a>
</div>

<% if (request.getAttribute("error") != null) { %>
    <p style="color: var(--error-color);"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
<% } %>

<%
    @SuppressWarnings("unchecked")
    List<Game> games = (List<Game>) request.getAttribute("games");
%>
<table class="admin-table">
    <thead>
        <tr>
            <th>ID</th>
            <th>Tên trò chơi</th>
            <th>Trạng thái</th>
            <th>Phát hành</th>
            <th>Hành động</th>
        </tr>
    </thead>
    <tbody>
    <% if (games == null || games.isEmpty()) { %>
        <tr><td colspan="5">Chưa có trò chơi nào.</td></tr>
    <% } else {
        for (Game g : games) {
            boolean isActive = GameStatus.ACTIVE.equals(g.getStatus());
        %>
        <tr>
            <td><%= g.getGameId() %></td>
            <td><%= HtmlUtils.escape(g.getName()) %></td>
            <td class="<%= isActive ? "admin-badge-active" : "admin-badge-inactive" %>"><%= isActive ? "Hoạt động" : "Tạm ẩn" %></td>
            <td><%= g.getReleaseDate() != null ? g.getReleaseDate().toString() : "-" %></td>
            <td class="admin-actions">
                <a class="btn-edit" href="${pageContext.request.contextPath}/admin/games/edit?id=<%= g.getGameId() %>">Sửa</a>
                <form action="${pageContext.request.contextPath}/admin/games/status" method="post">
                    <input type="hidden" name="id" value="<%= g.getGameId() %>">
                    <% if (isActive) { %>
                        <input type="hidden" name="action" value="deactivate">
                        <button type="submit" class="btn-danger">Tạm ẩn</button>
                    <% } else { %>
                        <input type="hidden" name="action" value="activate">
                        <button type="submit" class="btn-success">Kích hoạt</button>
                    <% } %>
                </form>
            </td>
        </tr>
    <% } } %>
    </tbody>
</table>

<%
    int currentPage = request.getAttribute("page") == null ? 1 : (int) request.getAttribute("page");
    int totalPages = request.getAttribute("totalPages") == null ? 1 : (int) request.getAttribute("totalPages");
%>
<% if (totalPages > 1) { %>
    <div class="admin-pagination">
        <% for (int p = 1; p <= totalPages; p++) { %>
            <a class="<%= p == currentPage ? "current" : "" %>"
               href="${pageContext.request.contextPath}/admin/games?page=<%= p %>"><%= p %></a>
        <% } %>
    </div>
<% } %>

<%@ include file="/WEB-INF/admin/layout/footer.jsp" %>
