package nus.edu.u.system.convert.user;

import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.OrganizerCreateUserDTO;
import nus.edu.u.system.domain.dto.OrganizerUpdateUserDTO;
import nus.edu.u.system.domain.dto.UserCreateDTO;
import nus.edu.u.system.domain.dto.UserUpdateDTO;
import nus.edu.u.system.domain.vo.user.*;
import org.mapstruct.Mapper;

/**
 * VO ↔ DTO ↔ DO 转换器
 */
@Mapper(componentModel = "spring")
public interface UserConvert {

    OrganizerCreateUserDTO toDTO(OrganizerCreateUserReqVO vo);
    OrganizerUpdateUserDTO toDTO(OrganizerUpdateUserReqVO vo);
    OrganizerUpdateUserRespVO toOrganizerUpdateUserRespVO(UserDO userDO);
    // Controller 入参 VO -> Service 入参 DTO
    UserCreateDTO toDTO(UserCreateReqVO vo);

    // DO -> RespVO
    UserCreateRespVO toCreateRespVO(UserDO userDO);


    UserUpdateDTO toUpdateDTO(UserUpdateReqVO vo);

    // DO -> RespVO（字段同名自动映射）
    UserUpdateRespVO toUpdateRespVO(UserDO userDO);


}
