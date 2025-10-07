package nus.edu.u.system.domain.vo.qrcode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QrCodeRespVO {
    private String base64Image;

    private String contentType;

    private Integer size;
}
