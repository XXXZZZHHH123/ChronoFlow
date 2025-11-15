package nus.edu.u.system.service.qrcode.strategy;

import nus.edu.u.system.domain.vo.qrcode.QrCodeReqVO;
import nus.edu.u.system.domain.vo.qrcode.QrCodeRespVO;

/**
 * QR Code Generation Strategy Interface
 * Defines the contract for different QR code generation algorithms
 *
 * @author Fan Yazhuoting
 * @date 2025-10-15
 */
public interface QrCodeGenerationStrategy {

    /**
     * Generate QR code using this strategy
     *
     * @param reqVO QR code request parameters
     * @return QR code response with Base64 image
     */
    QrCodeRespVO generate(QrCodeReqVO reqVO);

    /**
     * Check if this strategy supports the given request
     *
     * @param reqVO QR code request parameters
     * @return true if this strategy can handle the request
     */
    boolean supports(QrCodeReqVO reqVO);

    /**
     * Get the strategy name identifier
     *
     * @return Strategy name (e.g., "STANDARD", "SECURE", "STYLED")
     */
    String getStrategyName();

    /**
     * Get strategy priority for automatic selection
     * Higher value = higher priority
     *
     * @return Priority value
     */
    default int getPriority() {
        return 0;
    }
}