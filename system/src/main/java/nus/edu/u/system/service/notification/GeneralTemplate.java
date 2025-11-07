package nus.edu.u.system.service.notification;

import nus.edu.u.system.domain.dto.TemplateRequestDTO;
import nus.edu.u.system.domain.dto.TemplateResponseDTO;
import nus.edu.u.system.provider.template.TemplateClientFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;

@Component("generalTemplate")
public class GeneralTemplate implements TemplateEngineImplementor {

    private final TemplateClientFactory templateClientFactory = new TemplateClientFactory();

    @Override
    public TemplateResponseDTO process(TemplateRequestDTO templateRequestDTO) {
        var variables = templateRequestDTO.getVariables();
        var templateProvider = templateRequestDTO.getTemplateProvider();
        var templateEngine = templateClientFactory.getClient(templateProvider);

        Context context = new Context(templateRequestDTO.getLocale());
        if (variables != null) {
            variables.forEach(context::setVariable);
        }

        return templateEngine.render(templateRequestDTO);
    }
}
