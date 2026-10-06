package Vibol.SEN.meetingManagements.service;

import Vibol.SEN.meetingManagements.dto.MeetingAttachmentCreateRequest;
import Vibol.SEN.meetingManagements.dto.MeetingAttachmentDTO;
import Vibol.SEN.meetingManagements.exception.ResourceNotFoundException;
import Vibol.SEN.meetingManagements.model.Meeting;
import Vibol.SEN.meetingManagements.model.MeetingAttachment;
import Vibol.SEN.meetingManagements.model.User;
import Vibol.SEN.meetingManagements.repository.MeetingAttachmentRepository;
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
public class MeetingAttachmentService {

    private final MeetingAttachmentRepository attachmentRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<MeetingAttachmentDTO> getByMeeting(Long meetingId) {
        return attachmentRepository.findByMeeting_MeetingId(meetingId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public MeetingAttachmentDTO addAttachment(Long meetingId, MeetingAttachmentCreateRequest request) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with ID: " + meetingId));

        User uploader = null;
        if (request.getUploadedById() != null) {
            uploader = userRepository.findById(request.getUploadedById()).orElse(meeting.getOrganizer());
        } else {
            uploader = meeting.getOrganizer();
        }

        MeetingAttachment attachment = MeetingAttachment.builder()
                .meeting(meeting)
                .fileName(request.getFileName())
                .fileType(request.getFileType())
                .fileUrl(request.getFileUrl())
                .fileSize(request.getFileSize())
                .uploadedBy(uploader)
                .build();

        MeetingAttachment saved = attachmentRepository.save(attachment);
        return mapToDTO(saved);
    }

    public void deleteAttachment(Long attachmentId) {
        if (!attachmentRepository.existsById(attachmentId)) {
            throw new ResourceNotFoundException("Attachment not found with ID: " + attachmentId);
        }
        attachmentRepository.deleteById(attachmentId);
    }

    private MeetingAttachmentDTO mapToDTO(MeetingAttachment att) {
        return MeetingAttachmentDTO.builder()
                .attachmentId(att.getAttachmentId())
                .meetingId(att.getMeeting().getMeetingId())
                .fileName(att.getFileName())
                .fileType(att.getFileType())
                .fileUrl(att.getFileUrl())
                .fileSize(att.getFileSize())
                .uploadedAt(att.getUploadedAt())
                .uploadedBy(att.getUploadedBy() != null ? userService.mapToDTO(att.getUploadedBy()) : null)
                .build();
    }
}
