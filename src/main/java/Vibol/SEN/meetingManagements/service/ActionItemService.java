package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.ActionItemCreateRequest;
import Vibol.SEN.meetingManagements.dto.ActionItemDTO;
import Vibol.SEN.meetingManagements.exception.ResourceNotFoundException;
import Vibol.SEN.meetingManagements.model.ActionItem;
import Vibol.SEN.meetingManagements.model.Meeting;
import Vibol.SEN.meetingManagements.model.User;
import Vibol.SEN.meetingManagements.model.enums.ActionItemStatus;
import Vibol.SEN.meetingManagements.model.enums.NotificationType;
import Vibol.SEN.meetingManagements.repository.ActionItemRepository;
import Vibol.SEN.meetingManagements.repository.MeetingRepository;
import Vibol.SEN.meetingManagements.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ActionItemService {

    private final ActionItemRepository actionItemRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<ActionItemDTO> getByMeeting(Long meetingId) {
        return actionItemRepository.findByMeeting_MeetingId(meetingId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActionItemDTO> getByUser(Long userId, ActionItemStatus status) {
        List<ActionItem> items = (status != null)
                ? actionItemRepository.findByAssignee_UserIdAndStatus(userId, status)
                : actionItemRepository.findByAssignee_UserId(userId);
        return items.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public ActionItemDTO createActionItem(Long meetingId, ActionItemCreateRequest request) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with ID: " + meetingId));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId()).orElse(null);
        }

        ActionItem actionItem = ActionItem.builder()
                .meeting(meeting)
                .assignee(assignee)
                .taskDescription(request.getTaskDescription())
                .dueDate(request.getDueDate())
                .status(ActionItemStatus.PENDING)
                .build();

        ActionItem saved = actionItemRepository.save(actionItem);

        // Notify assignee
        if (assignee != null) {
            notificationService.createNotification(
                    meeting,
                    assignee,
                    NotificationType.REMINDER,
                    "New Action Item assigned to you for '" + meeting.getTitle() + "': " + request.getTaskDescription()
            );
        }

        return mapToDTO(saved);
    }

    public ActionItemDTO updateStatus(Long itemId, ActionItemStatus status) {
        ActionItem item = actionItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Action item not found with ID: " + itemId));

        item.setStatus(status);
        ActionItem saved = actionItemRepository.save(item);
        return mapToDTO(saved);
    }

    public void deleteActionItem(Long itemId) {
        if (!actionItemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Action item not found with ID: " + itemId);
        }
        actionItemRepository.deleteById(itemId);
    }

    private ActionItemDTO mapToDTO(ActionItem item) {
        return ActionItemDTO.builder()
                .itemId(item.getItemId())
                .meetingId(item.getMeeting().getMeetingId())
                .meetingTitle(item.getMeeting().getTitle())
                .assignee(item.getAssignee() != null ? userService.mapToDTO(item.getAssignee()) : null)
                .taskDescription(item.getTaskDescription())
                .dueDate(item.getDueDate())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
