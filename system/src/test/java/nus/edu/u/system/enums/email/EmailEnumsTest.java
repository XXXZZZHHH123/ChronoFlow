package nus.edu.u.system.enums.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class EmailEnumsTest {

    @Test
    void emailType_containsTextAndHtml() {
        assertThat(EnumSet.allOf(EmailType.class))
                .containsExactlyInAnyOrder(EmailType.TEXT, EmailType.HTML);
    }

    @Test
    void notificationChannel_containsEmailAndPush() {
        assertThat(EnumSet.allOf(NotificationChannel.class))
                .containsExactlyInAnyOrder(NotificationChannel.EMAIL, NotificationChannel.PUSH);
    }

    @Test
    void emailStatus_hasLifecycleStates() {
        assertThat(EnumSet.allOf(EmailStatus.class))
                .containsExactlyInAnyOrder(
                        EmailStatus.PENDING, EmailStatus.SENT, EmailStatus.FAILED);
    }

    @Test
    void emailProvider_exposesAwsSes() {
        assertThat(EnumSet.allOf(EmailProvider.class)).containsExactly(EmailProvider.AWS_SES);
    }
}
