package nus.edu.u.system.domain.vo.file;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FileResultVOTest {

    @Test
    void builderAssignsAllFields() {
        FileResultVO vo =
                FileResultVO.builder()
                        .objectName("event/1/report.pdf")
                        .name("report.pdf")
                        .contentType("application/pdf")
                        .size(2048L)
                        .signedUrl("https://example.com/files/report.pdf")
                        .build();

        assertThat(vo.getObjectName()).isEqualTo("event/1/report.pdf");
        assertThat(vo.getName()).isEqualTo("report.pdf");
        assertThat(vo.getContentType()).isEqualTo("application/pdf");
        assertThat(vo.getSize()).isEqualTo(2048L);
        assertThat(vo.getSignedUrl()).isEqualTo("https://example.com/files/report.pdf");
    }
}
