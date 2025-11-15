package nus.edu.u.system.service.qrcode.strategy;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.vo.qrcode.QrCodeReqVO;
import nus.edu.u.system.domain.vo.qrcode.QrCodeRespVO;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static nus.edu.u.system.enums.ErrorCodeConstants.QRCODE_GENERATION_FAILED;

/**
 * Standard QR Code Generation Strategy
 * Uses default settings suitable for most use cases
 *
 * @author Fan Yazhuoting
 * @date 2025-10-15
 */
@Component
@Slf4j
public class StandardQrCodeStrategy implements QrCodeGenerationStrategy {

    private static final int DEFAULT_SIZE = 300;
    private static final String DEFAULT_FORMAT = "PNG";
    private static final ErrorCorrectionLevel ERROR_CORRECTION = ErrorCorrectionLevel.M;

    @Override
    public QrCodeRespVO generate(QrCodeReqVO reqVO) {
        try {
            String content = reqVO.getContent();
            int size = Optional.ofNullable(reqVO.getSize()).orElse(DEFAULT_SIZE);
            String format = Optional.ofNullable(reqVO.getFormat()).orElse(DEFAULT_FORMAT);

            byte[] qrCodeBytes = generateQrCodeBytes(content, size, format);
            String base64Image = Base64.getEncoder().encodeToString(qrCodeBytes);

            log.info("Generated standard QR code: size={}, format={}", size, format);

            return QrCodeRespVO.builder()
                    .base64Image(base64Image)
                    .contentType("image/" + format.toLowerCase())
                    .size(size)
                    .build();

        } catch (IOException e) {
            log.error("Failed to generate standard QR code", e);
            throw new ServiceException(
                    QRCODE_GENERATION_FAILED.getCode(),
                    "Failed to generate QR code: " + e.getMessage());
        }
    }

    private byte[] generateQrCodeBytes(String content, int size, String format) throws IOException {
        try {
            // Configure QR code parameters
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, ERROR_CORRECTION);
            hints.put(EncodeHintType.MARGIN, 1);

            // Generate QR code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    content, BarcodeFormat.QR_CODE, size, size, hints);

            // Convert to image bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, format, outputStream);

            return outputStream.toByteArray();

        } catch (WriterException e) {
            throw new IOException("Failed to encode QR code", e);
        }
    }

    @Override
    public boolean supports(QrCodeReqVO reqVO) {
        // Standard strategy supports requests without explicit type or with "STANDARD" type
        return reqVO.getType() == null ||
                reqVO.getType().isBlank() ||
                "STANDARD".equalsIgnoreCase(reqVO.getType());
    }

    @Override
    public String getStrategyName() {
        return "STANDARD";
    }

    @Override
    public int getPriority() {
        return 1; // Lower priority, used as fallback
    }
}