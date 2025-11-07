package nus.edu.u.system.domain.dto;

import lombok.*;
import nus.edu.u.system.enums.email.EmailProvider;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailRequestDTO {
    private EmailProvider provider;
    private String to;
    private String subject;
    private String html;
    private List<AttachmentDTO> attachments;
}
