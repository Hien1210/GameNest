<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.gamenest.model.Game" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <%
        String mode = (String) request.getAttribute("mode");
        boolean isEdit = "edit".equals(mode);

        @SuppressWarnings("unchecked")
        List<Game> allActiveGames = (List<Game>) request.getAttribute("allActiveGames");

        Object gameIdAttr = request.getAttribute("gameId");
        String fGameId = gameIdAttr == null ? "" : String.valueOf(gameIdAttr);
        String fGameName = (String) request.getAttribute("gameName");
        String fTitle = (String) request.getAttribute("title");
        String fDescription = (String) request.getAttribute("description");
        Object maxPlayersAttr = request.getAttribute("maxPlayers");
        String fMaxPlayers = maxPlayersAttr == null ? "" : String.valueOf(maxPlayersAttr);
        Object lfgIdAttr = request.getAttribute("lfgId");
        String formAction = isEdit
                ? request.getContextPath() + "/lfg/edit?id=" + lfgIdAttr
                : request.getContextPath() + "/lfg/new";
        String backHref = isEdit
                ? request.getContextPath() + "/lfg/detail?id=" + lfgIdAttr
                : request.getContextPath() + "/lfg";
    %>
    <title><%= isEdit ? "Chỉnh sửa nhóm" : "Tạo nhóm mới" %> - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 640px; margin: 0 auto; }
        .form-card { background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 12px; padding: 26px; }
        .form-group { margin-bottom: 18px; }
        .form-group label { display: block; margin-bottom: 6px; font-size: 0.88rem; color: var(--text-secondary); }
        .form-group input, .form-group select, .form-group textarea {
            width: 100%; padding: 10px 12px; border-radius: 8px; border: 1px solid var(--border-color);
            background: var(--bg-primary); color: var(--text-primary); font-family: inherit; font-size: 0.92rem;
        }
        .form-group textarea { min-height: 120px; resize: vertical; }
        .form-static { padding: 10px 12px; border-radius: 8px; background: var(--bg-primary); color: var(--text-secondary); font-size: 0.92rem; }
        .btn-submit { padding: 11px 22px; border-radius: 8px; border: none; background: var(--accent-purple); color: #fff; font-weight: 600; cursor: pointer; }
        .error-msg { color: var(--error-color); margin-bottom: 16px; }
    </style>
</head>
<body>
<div class="page-wrap">
    <h1><%= isEdit ? "Chỉnh sửa nhóm" : "Tạo nhóm mới" %></h1>

    <% if (request.getAttribute("error") != null) { %>
        <p class="error-msg"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
    <% } %>

    <div class="form-card">
        <form method="post" action="<%= formAction %>">
            <% if (isEdit) { %>
            <div class="form-group">
                <label>Game</label>
                <div class="form-static"><%= HtmlUtils.escape(fGameName == null ? "" : fGameName) %></div>
            </div>
            <% } else { %>
            <div class="form-group">
                <label for="gameId">Game</label>
                <select name="gameId" id="gameId" required>
                    <option value="">-- Chọn game --</option>
                    <% if (allActiveGames != null) { for (Game g : allActiveGames) { %>
                    <option value="<%= g.getGameId() %>" <%= String.valueOf(g.getGameId()).equals(fGameId) ? "selected" : "" %>><%= HtmlUtils.escape(g.getName()) %></option>
                    <% } } %>
                </select>
            </div>
            <% } %>
            <div class="form-group">
                <label for="title">Tiêu đề</label>
                <input type="text" name="title" id="title" maxlength="250" required value="<%= HtmlUtils.escape(fTitle == null ? "" : fTitle) %>">
            </div>
            <div class="form-group">
                <label for="description">Mô tả</label>
                <textarea name="description" id="description" maxlength="5000"><%= HtmlUtils.escape(fDescription == null ? "" : fDescription) %></textarea>
            </div>
            <div class="form-group">
                <label for="maxPlayers">Số lượng thành viên tối đa</label>
                <input type="number" name="maxPlayers" id="maxPlayers" min="1" max="100" required value="<%= HtmlUtils.escape(fMaxPlayers) %>">
            </div>
            <button type="submit" class="btn-submit"><%= isEdit ? "Lưu thay đổi" : "Tạo nhóm" %></button>
        </form>
    </div>

    <p style="margin-top: 20px;"><a href="<%= backHref %>" style="color: var(--text-secondary);">&larr; Quay lại</a></p>
</div>
</body>
</html>
