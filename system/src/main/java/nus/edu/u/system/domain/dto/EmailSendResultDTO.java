package nus.edu.u.system.domain.dto;


import nus.edu.u.system.enums.email.EmailProvider;

public record EmailSendResultDTO(EmailProvider provider, String providerMessageId) {}
