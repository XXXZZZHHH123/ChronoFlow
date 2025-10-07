package nus.edu.u.system.domain.dto;

public record AttachmentDTO(
        String filename,
        String contentType,
        byte[] bytes,
        String url,
        boolean inline,
        String contentId
) {}
