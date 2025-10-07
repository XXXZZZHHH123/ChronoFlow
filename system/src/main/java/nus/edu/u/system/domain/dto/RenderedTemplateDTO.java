package nus.edu.u.system.domain.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record RenderedTemplateDTO(
        String subject, String bodyHtml, List<AttachmentDTO> attachments) {}
