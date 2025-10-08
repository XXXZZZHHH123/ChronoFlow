package nus.edu.u.framework.notification.idempotency;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencyKeyUtil {

    private final IdempotencyPropertiesConfig props;

    public String buildEmailKey(String to, String subject, String content) {
        return props.getEmailPrefix() + to + ":" + subject + ":" + sha256(content);
    }

    public String buildPushKey(String userId, String payload) {
        return props.getPushPrefix() + userId + ":" + sha256(payload);
    }

    private static String sha256(String input) {
        return Hashing.sha256().hashString(input, StandardCharsets.UTF_8).toString();
    }
}
