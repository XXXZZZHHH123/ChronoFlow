package nus.edu.u.system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@AllArgsConstructor
@Data
public class TemplateResponseDTO {
    String subject;
    String body;
    List<AttachmentDTO> attachments;
}
