<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.model.GameStatus" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<%
    boolean isEdit = "edit".equals(request.getAttribute("mode"));
    request.setAttribute("pageTitle", isEdit ? "Sửa trò chơi" : "Thêm trò chơi");
%>
<%@ include file="/WEB-INF/admin/layout/header.jsp" %>

<%
    String actionUrl = isEdit
        ? request.getContextPath() + "/admin/games/edit?id=" + request.getAttribute("gameId")
        : request.getContextPath() + "/admin/games/new";
%>
<h1><%= isEdit ? "Sửa trò chơi" : "Thêm trò chơi" %></h1>

<% if (request.getAttribute("error") != null) { %>
    <p style="color: var(--error-color);"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
<% } %>

<form action="<%= actionUrl %>" method="post">
    <div class="admin-form-group">
        <label for="name">Tên trò chơi</label>
        <input type="text" id="name" name="name"
               value="<%= HtmlUtils.escape((String) request.getAttribute("name")) %>" required maxlength="150">
    </div>

    <div class="admin-form-group">
        <label for="description">Mô tả</label>
        <textarea id="description" name="description"><%= HtmlUtils.escape((String) request.getAttribute("description")) %></textarea>
    </div>

    <div class="admin-form-group">
        <label for="coverImageUrl">URL ảnh bìa</label>
        <input type="text" id="coverImageUrl" name="coverImageUrl"
               value="<%= HtmlUtils.escape((String) request.getAttribute("coverImageUrl")) %>" maxlength="500" placeholder="https://...">
    </div>

    <div class="admin-form-group">
        <label for="releaseDate">Ngày phát hành</label>
        <input type="date" id="releaseDate" name="releaseDate"
               value="<%= HtmlUtils.escape((String) request.getAttribute("releaseDate")) %>">
    </div>

    <% if (!isEdit) { %>
    <div class="admin-form-group">
        <label for="status">Trạng thái ban đầu</label>
        <select id="status" name="status">
            <option value="<%= GameStatus.ACTIVE %>">Hoạt động</option>
            <option value="<%= GameStatus.INACTIVE %>">Tạm ẩn</option>
        </select>
    </div>
    <% } %>

    <button type="submit" class="admin-btn"><%= isEdit ? "Lưu thay đổi" : "Tạo trò chơi" %></button>
</form>

<p class="admin-back-link"><a href="${pageContext.request.contextPath}/admin/games">&larr; Quay lại danh sách</a></p>

<%@ include file="/WEB-INF/admin/layout/footer.jsp" %>
