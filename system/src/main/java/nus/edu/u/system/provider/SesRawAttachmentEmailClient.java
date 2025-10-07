package nus.edu.u.system.provider;

import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import nus.edu.u.framework.notification.email.EmailProviderPropertiesConfig;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import nus.edu.u.system.domain.dto.EmailSendResultDTO;
import nus.edu.u.system.enums.email.EmailProvider;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.RawMessage;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class SesRawAttachmentEmailClient implements EmailClient {

    private final SesV2Client ses;
    private final EmailProviderPropertiesConfig props;

    @Override
    public EmailSendResultDTO sendEmail(String to, String subject, String html, List<AttachmentDTO> attachments) {
        try {
            // 1) Mail session & message shell
            Session session = Session.getInstance(new Properties());
            MimeMessage mime = new MimeMessage(session);
            mime.setFrom(new InternetAddress(props.getFrom()));
            mime.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            mime.setSubject(subject, StandardCharsets.UTF_8.name());

            MimeMultipart mixed = new MimeMultipart("mixed");

            // 2a) related (html + inlines)
            MimeBodyPart relatedContainer = new MimeBodyPart();
            MimeMultipart related = new MimeMultipart("related");

            // HTML body
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setText(html, StandardCharsets.UTF_8.name(), "html");
            related.addBodyPart(htmlPart);

            // Inline parts (inline == true)
            if (attachments != null) {
                for (AttachmentDTO a : attachments) {
                    if (Boolean.TRUE.equals(a.inline())) {
                        MimeBodyPart inlinePart = new MimeBodyPart();
                        // Data
                        var contentType = safeContentType(a.contentType());
                        inlinePart.setDataHandler(new DataHandler(new ByteArrayDataSource(a.bytes(), contentType)));
                        String cid = a.contentId() != null ? a.contentId() : deriveCidFrom(a.filename());
                        inlinePart.setHeader("Content-ID", "<" + cid + ">");
                        inlinePart.setHeader("Content-Transfer-Encoding", "base64");
                        inlinePart.setDisposition("inline");
                        if (a.filename() != null) inlinePart.setFileName(a.filename());

                        related.addBodyPart(inlinePart);
                    }
                }
            }

            relatedContainer.setContent(related);
            mixed.addBodyPart(relatedContainer);

            //regular attachments (inline == false)
            if (attachments != null) {
                for (AttachmentDTO a : attachments) {
                    if (!Boolean.TRUE.equals(a.inline())) {
                        MimeBodyPart attachPart = new MimeBodyPart();
                        var contentType = safeContentType(a.contentType());
                        attachPart.setDataHandler(new DataHandler(new ByteArrayDataSource(a.bytes(), contentType)));
                        attachPart.setFileName(a.filename() != null ? a.filename() : "attachment");
                        attachPart.setDisposition("attachment");
                        attachPart.setHeader("Content-Transfer-Encoding", "base64");
                        mixed.addBodyPart(attachPart);
                    }
                }
            }

            // 3) Set content and convert to RawMessage
            mime.setContent(mixed);
            mime.saveChanges();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            mime.writeTo(out);

            RawMessage raw = RawMessage.builder()
                    .data(SdkBytes.fromByteArray(out.toByteArray()))
                    .build();

            SendEmailRequest req = SendEmailRequest.builder()
                    .fromEmailAddress(props.getFrom())
                    .destination(b -> b.toAddresses(to))
                    .content(EmailContent.builder().raw(raw).build())
                    .build();

            SendEmailResponse resp = ses.sendEmail(req);
            return new EmailSendResultDTO(EmailProvider.AWS_SES, resp.messageId());

        } catch (Exception e) {
            throw new RuntimeException("Failed to send email with attachments", e);
        }
    }

    private static String safeContentType(String ct) {
        return (ct == null || ct.isBlank()) ? "application/octet-stream" : ct;
    }

    private static String deriveCidFrom(String filename) {
        if (filename == null || filename.isBlank()) return "inline-" + System.nanoTime();
        String just = filename.replaceAll("[^A-Za-z0-9]", "");
        return (just.isEmpty() ? "inline" : just) + "-" + System.nanoTime();
    }
}