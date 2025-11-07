package nus.edu.u.system.domain.dto;

import java.util.Locale;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import nus.edu.u.system.enums.email.TemplateProvider;

@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class TemplateRequestDTO {
    private String templateId;
    private Map<String, Object> variables;
    private Locale locale;
    private TemplateProvider templateProvider;
}
