package nus.edu.u.system.service.email;


import lombok.RequiredArgsConstructor;
import nus.edu.u.system.domain.dto.RenderedTemplateDTO;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateEngine templateEngine;

    @Override
    public RenderedTemplateDTO render(String templateId, Map<String, Object> variables, Locale locale) {
        Context context = new Context(locale);
        if (variables != null) {
            variables.forEach(context::setVariable);
        }

        String html = templateEngine.process(templateId, context);

        // Subject can come from variables or i18n resource bundles
        String subject = variables != null && variables.containsKey("subject")
                ? variables.get("subject").toString()
                : "Default Subject";

        return new RenderedTemplateDTO(subject, html, List.of());
    }
}