package nus.edu.u.system.convert.user;

import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.CreateUserDTO;
import nus.edu.u.system.domain.dto.UpdateUserDTO;
import nus.edu.u.system.domain.vo.user.*;
import org.mapstruct.Mapper;

/**
 * VO ↔ DTO ↔ DO 转换器
 */
@Mapper(componentModel = "spring")
public interface UserConvert {

    CreateUserDTO toDTO(CreateUserReqVO vo);
    UpdateUserDTO toDTO(UpdateUserReqVO vo);
    UpdateUserRespVO toUpdateUserRespVO(UserDO userDO);
//    UserCreateDTO toDTO(UserCreateReqVO vo);
//
//    // DO -> RespVO
//    UserCreateRespVO toCreateRespVO(UserDO userDO);
//
//
//    UserUpdateDTO toUpdateDTO(UserUpdateReqVO vo);
//
//    // DO -> RespVO
//    UserUpdateRespVO toUpdateRespVO(UserDO userDO);


}
