package nus.edu.u.system.service.qrcode;

import static nus.edu.u.system.enums.ErrorCodeConstants.QRCODE_GENERATION_FAILED;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.vo.qrcode.QrCodeReqVO;
import nus.edu.u.system.domain.vo.qrcode.QrCodeRespVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QrCodeServiceImpl implements QrCodeService {
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final int DEFAULT_SIZE = 300;
    private static final String DEFAULT_FORMAT = "PNG";

    @Override
    public QrCodeRespVO generateQrCode(QrCodeReqVO reqVO) {
        try {
            String content = reqVO.getContent();
            int size = Optional.ofNullable(reqVO.getSize()).orElse(DEFAULT_SIZE);
            String format = Optional.ofNullable(reqVO.getFormat()).orElse(DEFAULT_FORMAT);

            byte[] qrCodeBytes = generateQrCodeBytes(content, size, format);
            String base64Image = Base64.getEncoder().encodeToString(qrCodeBytes);

            return QrCodeRespVO.builder()
                    .base64Image(base64Image)
                    .contentType("image/" + format.toLowerCase())
                    .size(size)
                    .build();

        } catch (IOException e) {
            log.error("Failed to generate QR code", e);
            throw new ServiceException(
                    QRCODE_GENERATION_FAILED.getCode(),
                    "Failed to generate QR code: " + e.getMessage());
        }
    }

    @Override
    public byte[] generateQrCodeBytes(String content, int size, String format) throws IOException {
        try {
            // Configure QR code parameters
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);

            // Generate QR code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix =
                    qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

            // Convert to image bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, format, outputStream);

            return outputStream.toByteArray();

        } catch (WriterException e) {
            throw new IOException("Failed to encode QR code", e);
        }
    }

    @Override
    public QrCodeRespVO generateEventCheckInQrWithToken(String checkInToken) {
        // Build secure check-in URL with token
        String url = baseUrl + "/system/checkin/scan?token=" + checkInToken;

        QrCodeReqVO reqVO = QrCodeReqVO.builder().content(url).size(400).format("PNG").build();

        QrCodeRespVO response = generateQrCode(reqVO);
        log.info("Generated event check-in QR code with token");
        return response;
    }
}
