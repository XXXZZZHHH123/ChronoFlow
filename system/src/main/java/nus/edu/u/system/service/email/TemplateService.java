package nus.edu.u.system.service.email;



import nus.edu.u.system.domain.dto.RenderedTemplateDTO;

import java.util.Locale;
import java.util.Map;

public interface TemplateService {
    RenderedTemplateDTO render(String templateId, Map<String, Object> variables, Locale locale);
}