<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="com.gamenest.model.Report" %>
<%@ page import="com.gamenest.model.ReportStatus" %>
<%@ page import="com.gamenest.model.ReportReason" %>
<%@ page import="com.gamenest.model.ReportTargetType" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<% request.setAttribute("pageTitle", "Reports"); %>
<%@ include file="/WEB-INF/moderator/layout/header.jsp" %>

<div class="mod-page-header">
    <h1>Reports</h1>
</div>

<% if (request.getAttribute("error") != null) { %>
    <div class="alert alert-error" style="margin-bottom: 16px;">
        <span class="material-symbols-outlined alert-icon">error</span>
        <span><%= HtmlUtils.escape((String) request.getAttribute("error")) %></span>
    </div>
<% } %>

<%
    String fStatus = (String) request.getAttribute("status");
    String fReason = (String) request.getAttribute("reason");
    String fTargetType = (String) request.getAttribute("targetType");
    String fReporterUsername = (String) request.getAttribute("reporterUsername");
    String fDateFrom = (String) request.getAttribute("dateFrom");
    String fDateTo = (String) request.getAttribute("dateTo");
    if (fStatus == null) fStatus = "";
    if (fReason == null) fReason = "";
    if (fTargetType == null) fTargetType = "";
    if (fReporterUsername == null) fReporterUsername = "";
    if (fDateFrom == null) fDateFrom = "";
    if (fDateTo == null) fDateTo = "";
%>
<form class="mod-filter-bar" action="${pageContext.request.contextPath}/moderator/reports" method="get">
    <div class="mod-filter-field">
        <label for="status">Trạng thái</label>
        <select id="status" name="status">
            <option value="">Tất cả</option>
            <option value="<%= ReportStatus.PENDING %>" <%= ReportStatus.PENDING.equals(fStatus) ? "selected" : "" %>>Chờ xử lý</option>
            <option value="<%= ReportStatus.RESOLVED %>" <%= ReportStatus.RESOLVED.equals(fStatus) ? "selected" : "" %>>Đã giải quyết</option>
            <option value="<%= ReportStatus.REJECTED %>" <%= ReportStatus.REJECTED.equals(fStatus) ? "selected" : "" %>>Đã từ chối</option>
        </select>
    </div>
    <div class="mod-filter-field">
        <label for="reason">Lý do</label>
        <select id="reason" name="reason">
            <option value="">Tất cả</option>
            <option value="<%= ReportReason.SPAM %>" <%= ReportReason.SPAM.equals(fReason) ? "selected" : "" %>>Spam / Rác</option>
            <option value="<%= ReportReason.HARASSMENT %>" <%= ReportReason.HARASSMENT.equals(fReason) ? "selected" : "" %>>Quấy rối / Xúc phạm</option>
            <option value="<%= ReportReason.INAPPROPRIATE_CONTENT %>" <%= ReportReason.INAPPROPRIATE_CONTENT.equals(fReason) ? "selected" : "" %>>Nội dung không phù hợp</option>
            <option value="<%= ReportReason.HATE_SPEECH %>" <%= ReportReason.HATE_SPEECH.equals(fReason) ? "selected" : "" %>>Ngôn từ thù ghét</option>
            <option value="<%= ReportReason.MISINFORMATION %>" <%= ReportReason.MISINFORMATION.equals(fReason) ? "selected" : "" %>>Thông tin sai lệch</option>
            <option value="<%= ReportReason.CHEATING %>" <%= ReportReason.CHEATING.equals(fReason) ? "selected" : "" %>>Gian lận</option>
            <option value="<%= ReportReason.OTHER %>" <%= ReportReason.OTHER.equals(fReason) ? "selected" : "" %>>Khác</option>
        </select>
    </div>
    <div class="mod-filter-field">
        <label for="targetType">Loại mục tiêu</label>
        <select id="targetType" name="targetType">
            <option value="">Tất cả</option>
            <option value="<%= ReportTargetType.ACCOUNT %>" <%= ReportTargetType.ACCOUNT.equals(fTargetType) ? "selected" : "" %>>Tài khoản</option>
            <option value="<%= ReportTargetType.QUESTION %>" <%= ReportTargetType.QUESTION.equals(fTargetType) ? "selected" : "" %>>Câu hỏi</option>
            <option value="<%= ReportTargetType.ANSWER %>" <%= ReportTargetType.ANSWER.equals(fTargetType) ? "selected" : "" %>>Câu trả lời</option>
        </select>
    </div>
    <div class="mod-filter-field">
        <label for="reporterUsername">Người báo cáo</label>
        <input type="text" id="reporterUsername" name="reporterUsername" value="<%= HtmlUtils.escape(fReporterUsername) %>" placeholder="Tên tài khoản">
    </div>
    <div class="mod-filter-field">
        <label for="dateFrom">Từ ngày</label>
        <input type="date" id="dateFrom" name="dateFrom" value="<%= HtmlUtils.escape(fDateFrom) %>">
    </div>
    <div class="mod-filter-field">
        <label for="dateTo">Đến ngày</label>
        <input type="date" id="dateTo" name="dateTo" value="<%= HtmlUtils.escape(fDateTo) %>">
    </div>
    <div class="mod-filter-actions">
        <button type="submit" class="mod-btn">
            <span class="material-symbols-outlined" style="font-size: 18px;">filter_alt</span>
            <span>Lọc</span>
        </button>
        <a class="mod-btn-secondary" href="${pageContext.request.contextPath}/moderator/reports?status=">
            <span class="material-symbols-outlined" style="font-size: 18px;">restart_alt</span>
            <span>Xóa lọc</span>
        </a>
    </div>
</form>

<%
    @SuppressWarnings("unchecked")
    List<Report> reports = (List<Report>) request.getAttribute("reports");
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
%>
<div style="overflow-x: auto;">
<table class="mod-table">
    <thead>
        <tr>
            <th>ID</th>
            <th>Người báo cáo</th>
            <th>Mục tiêu</th>
            <th>Lý do</th>
            <th>Trạng thái</th>
            <th>Thời gian</th>
            <th>Thao tác</th>
        </tr>
    </thead>
    <tbody>
    <% if (reports == null || reports.isEmpty()) { %>
        <tr><td colspan="7">Không có báo cáo nào phù hợp.</td></tr>
    <% } else {
        for (Report r : reports) {
            String badgeClass = "mod-badge-muted";
            String statusText = r.getStatus();
            if (ReportStatus.PENDING.equals(r.getStatus())) {
                badgeClass = "mod-badge-warning";
                statusText = "Chờ xử lý";
            } else if (ReportStatus.RESOLVED.equals(r.getStatus())) {
                badgeClass = "mod-badge-success";
                statusText = "Đã giải quyết";
            } else if (ReportStatus.REJECTED.equals(r.getStatus())) {
                badgeClass = "mod-badge-danger";
                statusText = "Đã từ chối";
            }

            String targetTypeText = r.getTargetType();
            if (ReportTargetType.ACCOUNT.equals(targetTypeText)) targetTypeText = "Tài khoản";
            else if (ReportTargetType.QUESTION.equals(targetTypeText)) targetTypeText = "Câu hỏi";
            else if (ReportTargetType.ANSWER.equals(targetTypeText)) targetTypeText = "Câu trả lời";

            String reasonText = r.getReason();
            if (ReportReason.SPAM.equals(reasonText)) reasonText = "Spam / Rác";
            else if (ReportReason.HARASSMENT.equals(reasonText)) reasonText = "Quấy rối / Xúc phạm";
            else if (ReportReason.INAPPROPRIATE_CONTENT.equals(reasonText)) reasonText = "Nội dung không phù hợp";
            else if (ReportReason.HATE_SPEECH.equals(reasonText)) reasonText = "Ngôn từ thù ghét";
            else if (ReportReason.MISINFORMATION.equals(reasonText)) reasonText = "Thông tin sai lệch";
            else if (ReportReason.CHEATING.equals(reasonText)) reasonText = "Gian lận";
            else if (ReportReason.OTHER.equals(reasonText)) reasonText = "Khác";
    %>
        <tr>
            <td>#<%= r.getReportId() %></td>
            <td><%= HtmlUtils.escape(r.getReporterUsername()) %></td>
            <td><%= HtmlUtils.escape(targetTypeText) %> #<%= r.getTargetId() %></td>
            <td><%= HtmlUtils.escape(reasonText) %></td>
            <td><span class="mod-badge <%= badgeClass %>"><%= statusText %></span></td>
            <td><%= r.getCreatedAt() != null ? r.getCreatedAt().format(fmt) : "-" %></td>
            <td class="mod-actions">
                <a href="${pageContext.request.contextPath}/moderator/reports/detail?id=<%= r.getReportId() %>">Xem</a>
            </td>
        </tr>
    <% } } %>
    </tbody>
</table>
</div>

<%
    int currentPage = request.getAttribute("page") == null ? 1 : (int) request.getAttribute("page");
    int totalPages = request.getAttribute("totalPages") == null ? 1 : (int) request.getAttribute("totalPages");

    StringBuilder qs = new StringBuilder();
    qs.append("&status=").append(URLEncoder.encode(fStatus, StandardCharsets.UTF_8));
    if (!fReason.isEmpty()) qs.append("&reason=").append(URLEncoder.encode(fReason, StandardCharsets.UTF_8));
    if (!fTargetType.isEmpty()) qs.append("&targetType=").append(URLEncoder.encode(fTargetType, StandardCharsets.UTF_8));
    if (!fReporterUsername.isEmpty()) qs.append("&reporterUsername=").append(URLEncoder.encode(fReporterUsername, StandardCharsets.UTF_8));
    if (!fDateFrom.isEmpty()) qs.append("&dateFrom=").append(URLEncoder.encode(fDateFrom, StandardCharsets.UTF_8));
    if (!fDateTo.isEmpty()) qs.append("&dateTo=").append(URLEncoder.encode(fDateTo, StandardCharsets.UTF_8));
    String qParam = qs.toString();
%>
<% if (totalPages > 1) { %>
    <div class="mod-pagination">
        <% for (int p = 1; p <= totalPages; p++) { %>
            <a class="<%= p == currentPage ? "current" : "" %>"
               href="${pageContext.request.contextPath}/moderator/reports?page=<%= p %><%= qParam %>"><%= p %></a>
        <% } %>
    </div>
<% } %>

<%@ include file="/WEB-INF/moderator/layout/footer.jsp" %>
