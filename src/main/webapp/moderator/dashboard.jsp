<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<%
    // Moderator Dashboard — shell/foundation only. No moderation module
    // exists yet, so this page intentionally shows no statistics, no
    // report counts, no fabricated data — just a valid landing page for
    // future modules to plug into (task spec §9).
    request.setAttribute("pageTitle", "Dashboard");
%>
<%@ include file="/WEB-INF/moderator/layout/header.jsp" %>
<%-- header.jsp already computed modLabel from session displayName/username;
     reused here directly instead of re-declaring the same locals, which
     would collide since <%@ include %> merges both scriptlets into one
     generated servlet method. --%>

<h1>Moderator Dashboard</h1>

<div class="mod-welcome-card">
    <p>Chào mừng, <strong><%= HtmlUtils.escape(modLabel != null ? modLabel : "Moderator") %></strong>.</p>
    <span class="mod-role-badge">MODERATOR</span>
</div>

<div class="mod-placeholder-grid">
    <a class="mod-placeholder-card active" href="${pageContext.request.contextPath}/moderator/reports">
        <span class="material-symbols-outlined">flag</span>
        <h3>Reports</h3>
        <p>Xem, xử lý báo cáo vi phạm đang chờ xử lý.</p>
    </a>
    <a class="mod-placeholder-card active" href="${pageContext.request.contextPath}/moderator/questions">
        <span class="material-symbols-outlined">quiz</span>
        <h3>Questions</h3>
        <p>Xem, kiểm duyệt trạng thái câu hỏi.</p>
    </a>
    <a class="mod-placeholder-card active" href="${pageContext.request.contextPath}/moderator/answers">
        <span class="material-symbols-outlined">forum</span>
        <h3>Answers</h3>
        <p>Xem, kiểm duyệt trạng thái câu trả lời.</p>
    </a>
</div>

<%@ include file="/WEB-INF/moderator/layout/footer.jsp" %>
