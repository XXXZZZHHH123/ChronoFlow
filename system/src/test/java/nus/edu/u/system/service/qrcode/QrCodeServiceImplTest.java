package nus.edu.u.system.service.qrcode;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import nus.edu.u.system.domain.vo.qrcode.QrCodeReqVO;
import nus.edu.u.system.domain.vo.qrcode.QrCodeRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QrCodeServiceImplTest {

    private QrCodeServiceImpl qrCodeService;

    @BeforeEach
    void setUp() {
        qrCodeService = new QrCodeServiceImpl();
    }

    @Test
    void generateQrCode_returnsBase64Payload() {
        QrCodeReqVO req =
                QrCodeReqVO.builder().content("Hello World").size(256).format("PNG").build();

        QrCodeRespVO resp = qrCodeService.generateQrCode(req);

        assertThat(resp.getBase64Image()).isNotBlank();
        assertThat(resp.getContentType()).isEqualTo("image/png");
        assertThat(resp.getSize()).isEqualTo(256);
    }

    @Test
    void generateQrCodeBytes_producesBytes() throws IOException {
        byte[] bytes = qrCodeService.generateQrCodeBytes("payload", 128, "PNG");

        assertThat(bytes.length).isGreaterThan(0);
    }

    @Test
    void generateEventCheckInQrWithToken_usesBaseUrl() {
        QrCodeRespVO resp = qrCodeService.generateEventCheckInQrWithToken("token-xyz");

        assertThat(resp.getBase64Image()).isNotBlank();
        assertThat(resp.getContentType()).isEqualTo("image/png");
    }
}
