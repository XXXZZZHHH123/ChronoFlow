package nus.edu.u.system.domain.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record RenderedTemplateDTO(
        String subject,
        String bodyHtml,
        List<AttachmentDTO> attachments
) {}