package nus.edu.u.system.domain.vo.attendee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendeeInviteReqVO {
    private String toEmail;
    private String attendeeName;
    private String organizationName;
    private MultipartFile qrFile;
}