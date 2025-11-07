package nus.edu.u.system.provider.email;

import nus.edu.u.system.domain.dto.EmailRequestDTO;

public interface EmailClient {
    void sendEmail(EmailRequestDTO emailRequestDTO);
}