package nus.edu.u.system.convert.event;

import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dto.EventDTO;
import nus.edu.u.system.domain.vo.event.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface EventConvert {
    EventConvert INSTANCE = Mappers.getMapper(EventConvert.class);

    EventDTO convert(EventCreateReqVO bean);
    EventDTO convert(EventUpdateReqVO bean);

    @Mapping(target = "userId", source = "organizerId")
    @Mapping(target = "remark", source = "remarks")   // VO.organizerId → DO.userId
    EventDO convert(EventDTO bean);

    @Mapping(target = "organizerId", source = "userId")
    EventDTO convert(EventDO bean);

    EventRespVO convertResp(EventDTO bean);

    @Mapping(target = "organizerId", source = "userId")
    EventRespVO DOconvertVO(EventDO bean);

    List<EventRespVO> convertList(List<EventDO> list);
}
