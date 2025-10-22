package nus.edu.u.system.service.qrcode;

import java.io.IOException;
import nus.edu.u.system.domain.vo.qrcode.QrCodeReqVO;
import nus.edu.u.system.domain.vo.qrcode.QrCodeRespVO;

/**
 * QR Code service interface
 *
 * @author Fan Yahzuoting
 * @date 2025-10-02
 */
public interface QrCodeService {
    /**
     * Generate QR code as Base64 encoded image
     *
     * @param reqVO QR code request parameters
     * @return QR code response with Base64 image
     */
    QrCodeRespVO generateQrCode(QrCodeReqVO reqVO);

    /**
     * Generate QR code as byte array
     *
     * @param content Content to encode
     * @param size Image size
     * @param format Image format (PNG, JPG, etc.)
     * @return QR code image bytes
     */
    byte[] generateQrCodeBytes(String content, int size, String format) throws IOException;

    /**
     * Generate QR code for event check-in with secure token
     *
     * @param checkInToken Unique check-in token for the attendee
     * @return QR code with check-in URL
     */
    QrCodeRespVO generateEventCheckInQrWithToken(String checkInToken);
}
