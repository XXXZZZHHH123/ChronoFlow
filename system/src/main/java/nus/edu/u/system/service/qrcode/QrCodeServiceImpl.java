package nus.edu.u.system.service.qrcode;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.system.domain.vo.qrcode.QrCodeReqVO;
import nus.edu.u.system.domain.vo.qrcode.QrCodeRespVO;
import nus.edu.u.system.service.qrcode.strategy.QrCodeGenerationStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;


/**
 * QR Code Service Implementation using Strategy Pattern
 *
 * @author Fan Yazhuoting
 * @date 2025-10-02
 */
@Service
@Slf4j
public class QrCodeServiceImpl implements QrCodeService {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * All available QR code generation strategies
     * Automatically injected by Spring from all @Component implementations
     */
    @Resource
    private List<QrCodeGenerationStrategy> strategies;

    /**
     * Default strategy to use when no specific strategy is found
     */
    private QrCodeGenerationStrategy defaultStrategy;

    /**
     * Initialize and sort strategies by priority
     */
    public QrCodeServiceImpl() {
        this.strategies = strategies;

        // Sort strategies by priority (descending)
        this.strategies.sort(Comparator.comparingInt(QrCodeGenerationStrategy::getPriority).reversed());

        // Set first strategy as default (should be StandardQrCodeStrategy)
        this.defaultStrategy = strategies.stream()
                .filter(s -> "STANDARD".equals(s.getStrategyName()))
                .findFirst()
                .orElse(strategies.get(0));

        log.info("Initialized QrCodeService with {} strategies. Default: {}",
                strategies.size(), defaultStrategy.getStrategyName());
    }

    @Override
    public QrCodeRespVO generateQrCode(QrCodeReqVO reqVO) {
        QrCodeGenerationStrategy strategy = selectStrategy(reqVO);

        log.debug("Selected strategy: {} for request type: {}",
                strategy.getStrategyName(), reqVO.getType());

        return strategy.generate(reqVO);
    }

    @Override
    public byte[] generateQrCodeBytes(String content, int size, String format) {
        // For backward compatibility - use standard strategy
        QrCodeReqVO reqVO = QrCodeReqVO.builder()
                .content(content)
                .size(size)
                .format(format)
                .type("STANDARD")
                .build();

        QrCodeRespVO response = generateQrCode(reqVO);
        return java.util.Base64.getDecoder().decode(response.getBase64Image());
    }

    @Override
    public QrCodeRespVO generateEventCheckInQrWithToken(String checkInToken) {
        String url = baseUrl + "/system/attendee/scan?token=" + checkInToken;

        QrCodeReqVO reqVO = QrCodeReqVO.builder()
                .content(url)
                .size(400)
                .format("PNG")
                .type("SECURE")
                .build();

        QrCodeRespVO response = generateQrCode(reqVO);
        log.info("Generated event check-in QR code with SECURE strategy");
        return response;
    }

    /**
     * Select appropriate strategy based on request
     *
     * @param reqVO QR code request
     * @return Selected strategy
     */
    private QrCodeGenerationStrategy selectStrategy(QrCodeReqVO reqVO) {
        if (reqVO.getType() != null && !reqVO.getType().isBlank()) {
            return strategies.stream()
                    .filter(s -> s.supports(reqVO))
                    .findFirst()
                    .orElse(defaultStrategy);
        }

        return strategies.stream()
                .filter(s -> s.supports(reqVO))
                .findFirst()
                .orElse(defaultStrategy);
    }
}