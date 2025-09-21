package nus.edu.u.system.convert.event;

import java.util.List;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dto.EventDTO;
import nus.edu.u.system.domain.vo.event.*;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper
public interface EventConvert {
    EventConvert INSTANCE = Mappers.getMapper(EventConvert.class);

    EventDTO convert(EventCreateReqVO bean);

    EventDTO convert(EventUpdateReqVO bean);

    @Mapping(target = "userId", source = "organizerId")
    @Mapping(target = "remark", source = "remarks") // VO.organizerId → DO.userId
    @Mapping(target = "name", source = "eventName")
    EventDO convert(EventDTO bean);

    @Mapping(target = "organizerId", source = "userId")
    EventDTO convert(EventDO bean);

    EventRespVO convertResp(EventDTO bean);

    @Mapping(target = "organizerId", source = "userId")
    EventRespVO DOconvertVO(EventDO bean);

    List<EventRespVO> convertList(List<EventDO> list);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
            @Mapping(target = "name",       source = "eventName"),
            @Mapping(target = "remark",     source = "remark"),
            // 其他同名字段（description/location/status/startTime/endTime/organizerId→userId）：
            @Mapping(target = "userId",     source = "organizerId")
    })
    void patch(@MappingTarget EventDO target, EventUpdateReqVO source);

    @Mappings({
            @Mapping(target = "eventName",  source = "name"),
            @Mapping(target = "remarks",    source = "remark"),
            @Mapping(target = "organizerId",source = "userId")
    })
    UpdateEventRespVO toUpdateResp(EventDO bean);
}
