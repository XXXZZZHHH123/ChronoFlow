package nus.edu.u.system.provider.template;

import nus.edu.u.system.domain.dto.TemplateRequestDTO;
import nus.edu.u.system.domain.dto.TemplateResponseDTO;

public interface TemplateClient {

    TemplateResponseDTO render(TemplateRequestDTO templateRequestDTO);
}
