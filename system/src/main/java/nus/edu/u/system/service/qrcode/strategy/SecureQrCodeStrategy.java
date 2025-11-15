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
 * Secure QR Code Generation Strategy
 * Uses high error correction level and larger size for better reliability
 * Suitable for critical use cases like event check-in
 *
 * @author Fan Yazhuoting
 * @date 2025-10-15
 */
@Component
@Slf4j
public class SecureQrCodeStrategy implements QrCodeGenerationStrategy {

    private static final int SECURE_SIZE = 400;
    private static final String SECURE_FORMAT = "PNG";
    private static final ErrorCorrectionLevel SECURE_ERROR_CORRECTION = ErrorCorrectionLevel.H;
    private static final int SECURE_MARGIN = 2;

    @Override
    public QrCodeRespVO generate(QrCodeReqVO reqVO) {
        try {
            String content = reqVO.getContent();
            int size = Optional.ofNullable(reqVO.getSize()).orElse(SECURE_SIZE);
            String format = Optional.ofNullable(reqVO.getFormat()).orElse(SECURE_FORMAT);

            byte[] qrCodeBytes = generateSecureQrCodeBytes(content, size, format);
            String base64Image = Base64.getEncoder().encodeToString(qrCodeBytes);

            log.info("Generated secure QR code with high error correction: size={}, format={}",
                    size, format);

            return QrCodeRespVO.builder()
                    .base64Image(base64Image)
                    .contentType("image/" + format.toLowerCase())
                    .size(size)
                    .build();

        } catch (IOException e) {
            log.error("Failed to generate secure QR code", e);
            throw new ServiceException(
                    QRCODE_GENERATION_FAILED.getCode(),
                    "Failed to generate secure QR code: " + e.getMessage());
        }
    }

    private byte[] generateSecureQrCodeBytes(String content, int size, String format)
            throws IOException {
        try {
            // Configure QR code parameters with high security settings
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, SECURE_ERROR_CORRECTION); // High error correction
            hints.put(EncodeHintType.MARGIN, SECURE_MARGIN); // Larger margin for better scanning

            // Generate QR code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    content, BarcodeFormat.QR_CODE, size, size, hints);

            // Convert to image bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, format, outputStream);

            return outputStream.toByteArray();

        } catch (WriterException e) {
            throw new IOException("Failed to encode secure QR code", e);
        }
    }

    @Override
    public boolean supports(QrCodeReqVO reqVO) {
        // Secure strategy supports requests with "SECURE" type
        return "SECURE".equalsIgnoreCase(reqVO.getType());
    }

    @Override
    public String getStrategyName() {
        return "SECURE";
    }

    @Override
    public int getPriority() {
        return 10; // Higher priority for secure use cases
    }
}