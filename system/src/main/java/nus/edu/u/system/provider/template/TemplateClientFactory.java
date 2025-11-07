package nus.edu.u.system.provider.template;

import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import nus.edu.u.system.enums.email.TemplateProvider;

@RequiredArgsConstructor
public final class TemplateClientFactory {

    private final Map<TemplateProvider, TemplateClient> cache =
            new EnumMap<>(TemplateProvider.class);

    public TemplateClient getClient(TemplateProvider p) {
        return cache.computeIfAbsent(
                p,
                k ->
                        switch (k) {
                            case Thymeleaf -> ThymeleafTemplateClient.defaultClient();
                        });
    }
}
