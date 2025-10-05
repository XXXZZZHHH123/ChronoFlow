package nus.edu.u.system.convert.task;

import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.vo.task.TaskCreateReqVO;
import nus.edu.u.system.domain.vo.task.TaskRespVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TaskConvert {
    TaskConvert INSTANCE = Mappers.getMapper(TaskConvert.class);

    @Mapping(target = "userId", source = "targetUserId")
    @Mapping(target = "eventId", ignore = true)
    @Mapping(target = "remark", ignore = true)
    TaskDO convert(TaskCreateReqVO bean);

    @Mapping(target = "assignedUser", ignore = true)
    @Mapping(target = "event", ignore = true)
    TaskRespVO toRespVO(TaskDO bean);
}
