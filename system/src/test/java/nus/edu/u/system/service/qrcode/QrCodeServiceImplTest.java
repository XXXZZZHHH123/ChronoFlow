package nus.edu.u.system.service.qrcode;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import nus.edu.u.system.domain.vo.qrcode.QrCodeReqVO;
import nus.edu.u.system.domain.vo.qrcode.QrCodeRespVO;
import nus.edu.u.system.service.qrcode.strategy.QrCodeGenerationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for QrCodeServiceImpl.
 *
 * <p>Uses reflection to inject stub strategies and baseUrl, then calls init().
 */
class QrCodeServiceImplTest {

    private QrCodeServiceImpl qrCodeService;

    @BeforeEach
    void setUp() throws Exception {
        qrCodeService = new QrCodeServiceImpl();

        // Inject a mutable list of stub strategies (avoid immutable list issues)
        List<QrCodeGenerationStrategy> stubs =
                new ArrayList<>(
                        List.of(new StubStrategy("STANDARD", 100), new StubStrategy("SECURE", 90)));
        setField("strategies", stubs);
        setField("baseUrl", "http://test-host");

        // call init() to perform post-construct initialization (sort + default selection)
        qrCodeService.init();
    }

    private void setField(String name, Object value) throws Exception {
        Field f = QrCodeServiceImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(qrCodeService, value);
    }

    @Test
    void generateQrCode_returnsBase64Payload() {
        QrCodeReqVO req =
                QrCodeReqVO.builder()
                        .content("Hello World")
                        .size(256)
                        .format("PNG")
                        .type("STANDARD")
                        .build();

        QrCodeRespVO resp = qrCodeService.generateQrCode(req);

        assertThat(resp.getBase64Image()).isNotBlank();
        assertThat(resp.getContentType()).isEqualTo("image/png");
        assertThat(resp.getSize()).isEqualTo(256);

        // verify base64 decodes back to content prefix
        String decoded = new String(Base64.getDecoder().decode(resp.getBase64Image()));
        assertThat(decoded).contains("Hello World");
    }

    @Test
    void generateQrCodeBytes_producesBytes() {
        byte[] bytes = qrCodeService.generateQrCodeBytes("payload", 128, "PNG");

        assertThat(bytes.length).isGreaterThan(0);
    }

    @Test
    void generateEventCheckInQrWithToken_usesBaseUrl_and_secureStrategy() {
        QrCodeRespVO resp = qrCodeService.generateEventCheckInQrWithToken("token-xyz");

        assertThat(resp.getBase64Image()).isNotBlank();
        assertThat(resp.getContentType()).isEqualTo("image/png");

        String decoded = new String(Base64.getDecoder().decode(resp.getBase64Image()));
        // Should include the baseUrl + path
        assertThat(decoded).contains("http://test-host/system/attendee/scan?token=token-xyz");
    }

    /** Simple stub strategy for testing */
    private static final class StubStrategy implements QrCodeGenerationStrategy {
        private final String name;
        private final int priority;

        private StubStrategy(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        @Override
        public boolean supports(nus.edu.u.system.domain.vo.qrcode.QrCodeReqVO reqVO) {
            if (reqVO == null) return false;
            String type = reqVO.getType();
            if (type == null || type.isBlank()) {
                // treat null as supported only by STANDARD stub
                return "STANDARD".equalsIgnoreCase(this.name);
            }
            return this.name.equalsIgnoreCase(type);
        }

        @Override
        public QrCodeRespVO generate(nus.edu.u.system.domain.vo.qrcode.QrCodeReqVO reqVO) {
            String content = reqVO.getContent();
            if (content == null) content = "empty";
            String base64 = Base64.getEncoder().encodeToString(content.getBytes());
            return QrCodeRespVO.builder()
                    .base64Image(base64)
                    .contentType("image/png")
                    .size(reqVO.getSize())
                    .build();
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public String getStrategyName() {
            return name;
        }
    }
}
