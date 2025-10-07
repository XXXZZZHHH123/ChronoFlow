package nus.edu.u.system.service.email;


import lombok.RequiredArgsConstructor;
import nus.edu.u.system.domain.dto.NotificationRequestDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final List<NotificationSender> senders;

    @Override
    public String send(NotificationRequestDTO request) {
        var sender = senders.stream()
                .filter(s -> s.supports(request.channel()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No sender for channel: " + request.channel()));
        return sender.send(request);
    }
}