package nus.edu.u.system.provider.template;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import nus.edu.u.system.domain.dto.TemplateRequestDTO;
import nus.edu.u.system.domain.dto.TemplateResponseDTO;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public final class ThymeleafTemplateClient implements TemplateClient {
    private final TemplateEngine engine;

    private ThymeleafTemplateClient(TemplateEngine engine) {
        this.engine = engine;
    }

    public static ThymeleafTemplateClient defaultClient() {
        ClassLoaderTemplateResolver r = new ClassLoaderTemplateResolver();
        r.setPrefix("templates/");
        r.setSuffix(".html");
        r.setTemplateMode(TemplateMode.HTML);
        r.setCharacterEncoding(StandardCharsets.UTF_8.name());
        r.setCacheable(true);

        SpringTemplateEngine e = new SpringTemplateEngine();
        e.setTemplateResolver(r);
        return new ThymeleafTemplateClient(e);
    }

    @Override
    public TemplateResponseDTO render(TemplateRequestDTO req) {
        Map<String, Object> vars = req.getVariables();
        Context ctx = new Context(req.getLocale());
        if (vars != null) vars.forEach(ctx::setVariable);
        String html = engine.process(req.getTemplateId(), ctx);
        String subject =
                (vars != null && vars.containsKey("subject"))
                        ? String.valueOf(vars.get("subject"))
                        : "Default Subject";
        return TemplateResponseDTO.builder()
                .subject(subject)
                .body(html)
                .attachments(List.of())
                .build();
    }
}
