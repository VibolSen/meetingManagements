package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.model.Meeting;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final Boolean mailEnabled;
    private final String mailFrom;

    public EmailNotificationService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${mail.enabled:false}") Boolean mailEnabled,
            @Value("${mail.from:noreply@meetinghub.internal}") String mailFrom) {
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.mailFrom = mailFrom;
    }

    private static final DateTimeFormatter ICS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    @Async
    public void sendMeetingInvitation(Meeting meeting, List<String> recipientEmails) {
        if (!Boolean.TRUE.equals(mailEnabled) || mailSender == null || recipientEmails == null || recipientEmails.isEmpty()) {
            log.debug("Email invitation skipped (mailEnabled={}, recipients={})", mailEnabled, recipientEmails);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(recipientEmails.toArray(new String[0]));
            helper.setSubject("🗓️ Meeting Invitation: " + meeting.getTitle());

            String htmlBody = buildInvitationHtml(meeting);
            helper.setText(htmlBody, true);

            // Generate RFC 5545 iCalendar content
            String icsContent = buildIcsContent(meeting, "REQUEST");
            helper.addAttachment(
                    "invite.ics",
                    new ByteArrayResource(icsContent.getBytes(StandardCharsets.UTF_8)),
                    "text/calendar; charset=UTF-8; method=REQUEST"
            );

            mailSender.send(message);
            log.info("Sent email invitations for meeting #{} to {} recipients", meeting.getMeetingId(), recipientEmails.size());
        } catch (Exception ex) {
            log.warn("Failed to dispatch email invitation for meeting #{}: {}", meeting.getMeetingId(), ex.getMessage());
        }
    }

    @Async
    public void sendMeetingCancellation(Meeting meeting, List<String> recipientEmails, String reason) {
        if (!Boolean.TRUE.equals(mailEnabled) || mailSender == null || recipientEmails == null || recipientEmails.isEmpty()) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(recipientEmails.toArray(new String[0]));
            helper.setSubject("🚫 Meeting Cancelled: " + meeting.getTitle());

            String htmlBody = "<div style='font-family:sans-serif;max-width:600px;margin:auto;padding:24px;border:1px solid #e2e8f0;border-radius:12px;background:#fff;'>" +
                    "<h2 style='color:#e11d48;margin-top:0;'>Meeting Cancelled</h2>" +
                    "<p>The following meeting has been cancelled:</p>" +
                    "<p><strong>Title:</strong> " + meeting.getTitle() + "</p>" +
                    "<p><strong>Room:</strong> " + (meeting.getRoom() != null ? meeting.getRoom().getName() : "N/A") + "</p>" +
                    (reason != null ? "<p style='color:#64748b;'><strong>Reason:</strong> " + reason + "</p>" : "") +
                    "<hr style='border:none;border-top:1px solid #e2e8f0;margin:20px 0;'/>" +
                    "<p style='font-size:12px;color:#94a3b8;'>This is an automated notification from Meeting Management Hub.</p>" +
                    "</div>";

            helper.setText(htmlBody, true);

            String icsContent = buildIcsContent(meeting, "CANCEL");
            helper.addAttachment(
                    "cancel.ics",
                    new ByteArrayResource(icsContent.getBytes(StandardCharsets.UTF_8)),
                    "text/calendar; charset=UTF-8; method=CANCEL"
            );

            mailSender.send(message);
            log.info("Sent email cancellation for meeting #{} to {} recipients", meeting.getMeetingId(), recipientEmails.size());
        } catch (Exception ex) {
            log.warn("Failed to dispatch email cancellation for meeting #{}: {}", meeting.getMeetingId(), ex.getMessage());
        }
    }

    private String buildInvitationHtml(Meeting meeting) {
        String roomName = meeting.getRoom() != null ? meeting.getRoom().getName() : "Online / TBA";
        String roomLocation = meeting.getRoom() != null ? meeting.getRoom().getLocation() : "";
        String organizerName = meeting.getOrganizer() != null ? meeting.getOrganizer().getName() : "Organizer";

        return "<div style='font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;max-width:600px;margin:auto;padding:32px;border:1px solid #e2e8f0;border-radius:16px;background:#ffffff;box-shadow:0 4px 6px -1px rgba(0,0,0,0.05);'>" +
                "<div style='border-bottom:2px solid #6366f1;padding-bottom:16px;margin-bottom:20px;'>" +
                "<span style='font-size:12px;font-weight:700;color:#6366f1;text-transform:uppercase;letter-spacing:1px;'>Meeting Hub Enterprise</span>" +
                "<h1 style='font-size:24px;font-weight:800;color:#0f172a;margin:8px 0 0;'>" + meeting.getTitle() + "</h1>" +
                "</div>" +
                "<table style='width:100%;border-collapse:collapse;margin-bottom:24px;font-size:14px;color:#334155;'>" +
                "<tr><td style='padding:8px 0;width:120px;font-weight:600;color:#64748b;'>Date & Time:</td><td style='padding:8px 0;font-weight:600;color:#0f172a;'>" + meeting.getStartTime() + " — " + meeting.getEndTime() + "</td></tr>" +
                "<tr><td style='padding:8px 0;font-weight:600;color:#64748b;'>Room:</td><td style='padding:8px 0;'>" + roomName + " (" + roomLocation + ")</td></tr>" +
                "<tr><td style='padding:8px 0;font-weight:600;color:#64748b;'>Organizer:</td><td style='padding:8px 0;'>" + organizerName + "</td></tr>" +
                (meeting.getPurpose() != null ? "<tr><td style='padding:8px 0;vertical-align:top;font-weight:600;color:#64748b;'>Agenda:</td><td style='padding:8px 0;'>" + meeting.getPurpose() + "</td></tr>" : "") +
                "</table>" +
                "<div style='background:#f8fafc;padding:16px;border-radius:12px;margin-bottom:24px;border:1px solid #f1f5f9;'>" +
                "<p style='margin:0;font-size:13px;color:#475569;'>📎 A calendar invitation file (<strong>invite.ics</strong>) is attached. Open it to sync this event to your Outlook, Google, or Apple Calendar.</p>" +
                "</div>" +
                "<p style='font-size:12px;color:#94a3b8;margin:0;border-top:1px solid #f1f5f9;padding-top:16px;'>Sent via Meeting Management System</p>" +
                "</div>";
    }

    private String buildIcsContent(Meeting meeting, String method) {
        String startIso = meeting.getStartTime().format(ICS_DATE_FORMAT);
        String endIso = meeting.getEndTime().format(ICS_DATE_FORMAT);
        String roomName = meeting.getRoom() != null ? meeting.getRoom().getName() : "TBA";
        String summary = meeting.getTitle().replace("\n", " ");
        String description = meeting.getPurpose() != null ? meeting.getPurpose().replace("\n", "\\n") : "";

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//MeetingHub//MeetingManagementSystem//EN\r\n");
        sb.append("METHOD:").append(method).append("\r\n");
        sb.append("BEGIN:VEVENT\r\n");
        sb.append("UID:meeting-").append(meeting.getMeetingId()).append("@meetinghub.internal\r\n");
        sb.append("DTSTAMP:").append(startIso).append("\r\n");
        sb.append("DTSTART:").append(startIso).append("\r\n");
        sb.append("DTEND:").append(endIso).append("\r\n");
        sb.append("SUMMARY:").append(summary).append("\r\n");
        sb.append("DESCRIPTION:").append(description).append("\r\n");
        sb.append("LOCATION:").append(roomName).append("\r\n");
        sb.append("STATUS:CONFIRMED\r\n");
        sb.append("END:VEVENT\r\n");
        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }
}
