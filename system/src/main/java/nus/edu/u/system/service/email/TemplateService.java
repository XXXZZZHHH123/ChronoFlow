package nus.edu.u.system.service.email;

import java.util.Locale;
import java.util.Map;
import nus.edu.u.system.domain.dto.RenderedTemplateDTO;

public interface TemplateService {
    RenderedTemplateDTO render(String templateId, Map<String, Object> variables, Locale locale);
}
