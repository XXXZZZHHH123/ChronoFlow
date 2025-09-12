package nus.edu.u.system.convert.user;

import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.UserCreateDTO;
import nus.edu.u.system.domain.dto.UserUpdateDTO;
import nus.edu.u.system.domain.vo.user.UserCreateReqVO;
import nus.edu.u.system.domain.vo.user.UserCreateRespVO;
import nus.edu.u.system.domain.vo.user.UserUpdateReqVO;
import nus.edu.u.system.domain.vo.user.UserUpdateRespVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * VO ↔ DTO ↔ DO 转换器
 */
@Mapper(componentModel = "spring")
public interface UserConvert {

    // Controller 入参 VO -> Service 入参 DTO
    UserCreateDTO toDTO(UserCreateReqVO vo);

    // DO -> RespVO
    UserCreateRespVO toCreateRespVO(UserDO userDO);

    UserUpdateDTO toUpdateDTO(UserUpdateReqVO vo);

    // DO -> RespVO（字段同名自动映射）
    UserUpdateRespVO toUpdateRespVO(UserDO userDO);
}
