package nus.edu.u.system.service.qrcode;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.system.domain.vo.qrcode.QrCodeReqVO;
import nus.edu.u.system.domain.vo.qrcode.QrCodeRespVO;
import nus.edu.u.system.service.qrcode.strategy.QrCodeGenerationStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * QR Code Service Implementation using Strategy Pattern
 *
 * <p>Ensures injected strategy list is copied into a mutable list before sorting or modification,
 * avoiding ImmutableCollections UnsupportedOperationException in test / runtime environments.
 */
@Service
@Slf4j
public class QrCodeServiceImpl implements QrCodeService {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /** All available QR code generation strategies automatically injected by Spring */
    @Resource private List<QrCodeGenerationStrategy> strategies;

    /** Default strategy to use when no specific strategy is found */
    private QrCodeGenerationStrategy defaultStrategy;

    @PostConstruct
    public void init() {
        if (strategies == null || strategies.isEmpty()) {
            log.warn(
                    "No QrCodeGenerationStrategy implementations found. Service may not work properly.");
            return;
        }

        // Make a mutable copy to avoid UnsupportedOperationException when original list is
        // immutable
        this.strategies = new ArrayList<>(this.strategies);

        // Sort strategies by priority (descending)
        strategies.sort(Comparator.comparingInt(QrCodeGenerationStrategy::getPriority).reversed());

        // Set default strategy (prefer STANDARD if present)
        this.defaultStrategy =
                strategies.stream()
                        .filter(s -> "STANDARD".equals(s.getStrategyName()))
                        .findFirst()
                        .orElse(strategies.get(0));

        log.info(
                "Initialized QrCodeService with {} strategies. Default: {}",
                strategies.size(),
                defaultStrategy == null ? "null" : defaultStrategy.getStrategyName());
    }

    @Override
    public QrCodeRespVO generateQrCode(QrCodeReqVO reqVO) {
        QrCodeGenerationStrategy strategy = selectStrategy(reqVO);

        log.debug(
                "Selected strategy: {} for request type: {}",
                strategy == null ? "null" : strategy.getStrategyName(),
                reqVO == null ? "null" : reqVO.getType());

        if (strategy == null) {
            throw new IllegalStateException("No QR code strategy available");
        }

        return strategy.generate(reqVO);
    }

    @Override
    public byte[] generateQrCodeBytes(String content, int size, String format) {
        QrCodeReqVO reqVO =
                QrCodeReqVO.builder()
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

        QrCodeReqVO reqVO =
                QrCodeReqVO.builder().content(url).size(400).format("PNG").type("SECURE").build();

        QrCodeRespVO response = generateQrCode(reqVO);
        log.info("Generated event check-in QR code with selected strategy");
        return response;
    }

    private QrCodeGenerationStrategy selectStrategy(QrCodeReqVO reqVO) {
        if (strategies == null || strategies.isEmpty()) {
            return defaultStrategy;
        }

        if (reqVO != null && reqVO.getType() != null && !reqVO.getType().isBlank()) {
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
