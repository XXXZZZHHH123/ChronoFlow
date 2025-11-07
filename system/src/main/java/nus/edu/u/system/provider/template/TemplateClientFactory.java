package nus.edu.u.system.provider.template;

import lombok.RequiredArgsConstructor;
import nus.edu.u.system.enums.email.TemplateProvider;

import java.util.EnumMap;
import java.util.Map;

@RequiredArgsConstructor
public final class TemplateClientFactory {

    private final Map<TemplateProvider, TemplateClient> cache = new EnumMap<>(TemplateProvider.class);

    public TemplateClient getClient(TemplateProvider p) {
        return cache.computeIfAbsent(p, k -> switch (k) {
            case Thymeleaf -> ThymeleafTemplateClient.defaultClient();
        });
    }
}
