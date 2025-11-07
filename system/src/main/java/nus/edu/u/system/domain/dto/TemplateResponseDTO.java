package nus.edu.u.system.domain.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@AllArgsConstructor
@Data
public class TemplateResponseDTO {
    String subject;
    String body;
    List<AttachmentDTO> attachments;
}
