package nus.edu.u.system.domain.vo.qrcode;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeReqVO {
    @NotBlank(message = "Content cannot be empty")
    private String content;

    @Min(value = 100, message = "Size must be at least 100")
    @Max(value = 1000, message = "Size cannot exceed 1000")
    @Builder.Default
    private Integer size = 300;

    @Builder.Default private String format = "PNG"; // PNG, JPG, GIF
}
