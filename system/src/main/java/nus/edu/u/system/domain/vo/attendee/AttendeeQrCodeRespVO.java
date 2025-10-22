package nus.edu.u.system.domain.vo.attendee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Single attendee's QR code response */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttendeeQrCodeRespVO {

    private Long id;

    private String attendeeEmail;

    private String attendeeName;

    private String attendeeMobile;

    private String checkInToken;

    private String qrCodeBase64;

    private String qrCodeUrl;

    private Integer checkInStatus;
}
