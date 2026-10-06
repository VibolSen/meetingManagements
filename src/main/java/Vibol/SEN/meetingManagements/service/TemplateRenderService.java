package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.model.Meeting;
import Vibol.SEN.meetingManagements.model.enums.NotificationType;
import Vibol.SEN.meetingManagements.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateRenderService {

    private final NotificationTemplateRepository templateRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a");

    public String renderTemplate(NotificationType type, Map<String, String> variables) {
        String templateContent = templateRepository.findByType(type)
                .map(t -> t != null ? t.getContent() : null)
                .filter(content -> content != null && !content.trim().isEmpty())
                .orElseGet(() -> getDefaultTemplate(type));

        String rendered = templateContent;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String token = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue() : "-";
            rendered = rendered.replace(token, value);
        }

        // Clean any leftover unreplaced tokens
        rendered = rendered.replaceAll("\\{[a-zA-Z0-9_]+\\}", "-");
        return rendered;
    }

    public Map<String, String> buildVariables(Meeting meeting, Integer leadMinutes, String reason) {
        Map<String, String> vars = new HashMap<>();
        if (meeting == null) {
            return vars;
        }

        vars.put("title", meeting.getTitle() != null ? meeting.getTitle() : "Untitled Meeting");
        vars.put("room", meeting.getRoom() != null ? meeting.getRoom().getName() + " (" + meeting.getRoom().getLocation() + ")" : "TBD");
        
        if (meeting.getStartTime() != null) {
            vars.put("startTime", meeting.getStartTime().format(DATE_TIME_FORMATTER));
        } else {
            vars.put("startTime", "-");
        }

        if (meeting.getEndTime() != null) {
            vars.put("endTime", meeting.getEndTime().format(TIME_FORMATTER));
        } else {
            vars.put("endTime", "-");
        }

        vars.put("organizer", meeting.getOrganizer() != null ? meeting.getOrganizer().getName() : "System");
        vars.put("purpose", meeting.getPurpose() != null && !meeting.getPurpose().isBlank() ? meeting.getPurpose() : "General Business Discussion");

        if (meeting.getAttendees() != null && !meeting.getAttendees().isEmpty()) {
            String attendeesStr = meeting.getAttendees().stream()
                    .filter(a -> a.getUser() != null)
                    .map(a -> a.getUser().getName())
                    .collect(Collectors.joining(", "));
            vars.put("attendees", attendeesStr.isEmpty() ? "None specified" : attendeesStr);
        } else {
            vars.put("attendees", "None specified");
        }

        if (meeting.getMaterials() != null && !meeting.getMaterials().isEmpty()) {
            String matsStr = meeting.getMaterials().stream()
                    .filter(m -> m.getMaterial() != null)
                    .map(m -> m.getMaterial().getName() + " (" + m.getQuantityRequested() + ")")
                    .collect(Collectors.joining(", "));
            vars.put("materials", matsStr.isEmpty() ? "None requested" : matsStr);
        } else {
            vars.put("materials", "None requested");
        }

        vars.put("leadMinutes", leadMinutes != null ? String.valueOf(leadMinutes) : "10");
        vars.put("reason", reason != null && !reason.isBlank() ? reason : "Administrative Schedule Update");

        return vars;
    }

    public String getDefaultTemplate(NotificationType type) {
        switch (type) {
            case REMINDER:
                return "⏰ *UPCOMING MEETING REMINDER (Starts in {leadMinutes} Minutes)*\n\n" +
                        "📌 *Meeting:* {title}\n" +
                        "🏢 *Room:* {room}\n" +
                        "🕒 *Time:* {startTime} – {endTime}\n" +
                        "👤 *Organizer:* {organizer}\n" +
                        "📋 *Purpose:* {purpose}\n" +
                        "👥 *Attendees:* {attendees}\n" +
                        "📦 *Materials:* {materials}\n\n" +
                        "⚠️ *Please proceed to the conference room shortly.*";
            case CONFIRMATION:
                return "📅 *NEW MEETING SCHEDULED*\n\n" +
                        "📌 *Meeting:* {title}\n" +
                        "🏢 *Room:* {room}\n" +
                        "🕒 *Time:* {startTime} – {endTime}\n" +
                        "👤 *Organizer:* {organizer}\n" +
                        "📋 *Purpose:* {purpose}\n" +
                        "👥 *Attendees:* {attendees}\n" +
                        "📊 *Status:* Confirmed";
            case CHANGE:
                return "✅ *MEETING APPROVED & CONFIRMED*\n\n" +
                        "📌 *Meeting:* {title}\n" +
                        "🏢 *Room:* {room}\n" +
                        "🕒 *Time:* {startTime} – {endTime}\n" +
                        "👤 *Organizer:* {organizer}\n" +
                        "📋 *Purpose:* {purpose}\n" +
                        "📊 *Status:* Approved by Administrator";
            case CANCELLATION:
                return "❌ *MEETING CANCELLED*\n\n" +
                        "📌 *Meeting:* {title}\n" +
                        "🏢 *Room:* {room} (Released)\n" +
                        "🕒 *Originally Scheduled:* {startTime}\n" +
                        "📝 *Reason:* {reason}\n" +
                        "📦 *Inventory:* Reserved materials returned to stock";
            default:
                return "🔔 *MEETING NOTIFICATION*\n\n" +
                        "📌 *Meeting:* {title}\n" +
                        "🏢 *Room:* {room}\n" +
                        "🕒 *Time:* {startTime} – {endTime}\n" +
                        "👤 *Organizer:* {organizer}";
        }
    }
}
