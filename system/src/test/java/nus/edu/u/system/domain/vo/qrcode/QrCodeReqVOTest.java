package nus.edu.u.system.domain.vo.qrcode;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QrCodeReqVOTest {

    @Test
    void builderAppliesDefaultsWhenNotOverridden() {
        QrCodeReqVO reqVO = QrCodeReqVO.builder().content("ticket:123").build();

        assertThat(reqVO.getContent()).isEqualTo("ticket:123");
        assertThat(reqVO.getSize()).isEqualTo(300);
        assertThat(reqVO.getFormat()).isEqualTo("PNG");

        QrCodeReqVO overridden =
                QrCodeReqVO.builder().content("ticket:456").size(600).format("JPG").build();

        assertThat(overridden.getSize()).isEqualTo(600);
        assertThat(overridden.getFormat()).isEqualTo("JPG");
    }
}
