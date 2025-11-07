package nus.edu.u.system.domain.dto;

import java.util.List;
import lombok.*;
import nus.edu.u.system.enums.email.EmailProvider;

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
