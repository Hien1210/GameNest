<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.model.Report" %>
<%@ page import="com.gamenest.model.ReportStatus" %>
<%@ page import="com.gamenest.model.ReportReason" %>
<%@ page import="com.gamenest.model.ReportTargetType" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<% request.setAttribute("pageTitle", "Chi tiết báo cáo"); %>
<%@ include file="/WEB-INF/admin/layout/header.jsp" %>

<div class="admin-page-header">
    <h1>Chi tiết báo cáo</h1>
</div>

<% if (request.getAttribute("error") != null) { %>
    <p style="color: var(--error-color);"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
<% } %>

<%
    Report report = (Report) request.getAttribute("report");
    String targetLabel = (String) request.getAttribute("targetLabel");
%>
<% if (report == null) { %>
    <p>Báo cáo không tồn tại.</p>
<% } else {
    String bClass = "admin-badge-inactive";
    String statusText = report.getStatus();
    if (ReportStatus.PENDING.equals(report.getStatus())) {
        bClass = "admin-badge-suspended";
        statusText = "Chờ xử lý";
    } else if (ReportStatus.RESOLVED.equals(report.getStatus())) {
        bClass = "admin-badge-active";
        statusText = "Đã giải quyết";
    } else if (ReportStatus.REJECTED.equals(report.getStatus())) {
        bClass = "admin-badge-deleted";
        statusText = "Đã từ chối";
    }

    String targetTypeText = report.getTargetType();
    if (ReportTargetType.ACCOUNT.equals(targetTypeText)) targetTypeText = "Tài khoản";
    else if (ReportTargetType.QUESTION.equals(targetTypeText)) targetTypeText = "Câu hỏi";
    else if (ReportTargetType.ANSWER.equals(targetTypeText)) targetTypeText = "Câu trả lời";

    String reasonText = report.getReason();
    if (ReportReason.SPAM.equals(reasonText)) reasonText = "Spam / Rác";
    else if (ReportReason.HARASSMENT.equals(reasonText)) reasonText = "Quấy rối / Xúc phạm";
    else if (ReportReason.INAPPROPRIATE_CONTENT.equals(reasonText)) reasonText = "Nội dung không phù hợp";
    else if (ReportReason.HATE_SPEECH.equals(reasonText)) reasonText = "Ngôn từ thù ghét";
    else if (ReportReason.MISINFORMATION.equals(reasonText)) reasonText = "Thông tin sai lệch";
    else if (ReportReason.CHEATING.equals(reasonText)) reasonText = "Gian lận";
    else if (ReportReason.OTHER.equals(reasonText)) reasonText = "Khác";
%>
    <div class="admin-detail-card">
        <dl>
            <dt>Mã báo cáo</dt><dd>#<%= report.getReportId() %></dd>
            <dt>Người báo cáo</dt><dd><%= HtmlUtils.escape(report.getReporterUsername()) %></dd>
            <dt>Loại mục tiêu</dt><dd><%= HtmlUtils.escape(targetTypeText) %></dd>
            <dt>Mã mục tiêu</dt>
            <dd>#<%= report.getTargetId() %><%= targetLabel != null ? " — " + HtmlUtils.escape(targetLabel) : "" %></dd>
            <dt>Lý do</dt><dd><%= HtmlUtils.escape(reasonText) %></dd>
            <dt>Mô tả</dt>
            <dd><%= report.getDescription() != null ? HtmlUtils.escape(report.getDescription()) : "-" %></dd>
            <dt>Trạng thái</dt><dd class="<%= bClass %>"><%= statusText %></dd>
            <dt>Thời gian tạo</dt><dd><%= report.getCreatedAt() != null ? report.getCreatedAt().toString() : "-" %></dd>
            <dt>Xét duyệt bởi</dt>
            <dd><%= report.getReviewedByUsername() != null ? HtmlUtils.escape(report.getReviewedByUsername()) : "-" %></dd>
            <dt>Thời gian xét duyệt</dt>
            <dd><%= report.getReviewedAt() != null ? report.getReviewedAt().toString() : "-" %></dd>
            <dt>Ghi chú xử lý</dt>
            <dd><%= report.getResolutionNote() != null ? HtmlUtils.escape(report.getResolutionNote()) : "-" %></dd>
        </dl>

        <% if (ReportStatus.PENDING.equals(report.getStatus())) { %>
            <div class="admin-form-group">
                <form action="${pageContext.request.contextPath}/admin/reports/resolve" method="post">
                    <input type="hidden" name="id" value="<%= report.getReportId() %>">
                    <label for="resolutionNote">Ghi chú xử lý (tùy chọn)</label>
                    <textarea id="resolutionNote" name="resolutionNote" maxlength="1000"></textarea>
                    <div class="admin-detail-actions" style="margin-top: 12px;">
                        <button type="submit" name="action" value="resolve" class="btn-success">Giải quyết</button>
                        <button type="submit" name="action" value="reject" class="btn-danger">Từ chối</button>
                    </div>
                </form>
            </div>
        <% } else { %>
            <p style="color: var(--text-secondary); margin-top: 16px;">Báo cáo này đã được xử lý.</p>
        <% } %>
    </div>
<% } %>

<div class="admin-back-link">
    <a href="${pageContext.request.contextPath}/admin/reports">&larr; Quay lại danh sách</a>
</div>

<%@ include file="/WEB-INF/admin/layout/footer.jsp" %>
