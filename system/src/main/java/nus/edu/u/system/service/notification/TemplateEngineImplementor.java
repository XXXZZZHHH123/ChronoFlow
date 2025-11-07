package nus.edu.u.system.service.notification;

import nus.edu.u.system.domain.dto.TemplateRequestDTO;
import nus.edu.u.system.domain.dto.TemplateResponseDTO;

public interface TemplateEngineImplementor {
    TemplateResponseDTO process(TemplateRequestDTO templateRequestDTO);
}
