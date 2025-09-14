package nus.edu.u.system.controller.user;

import static nus.edu.u.common.core.domain.CommonResult.error;
import static nus.edu.u.common.core.domain.CommonResult.success;
import static nus.edu.u.system.enums.ErrorCodeConstants.REG_FAIL;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.system.domain.vo.reg.RegMemberReqVO;
import nus.edu.u.system.domain.vo.reg.RegOrganizerReqVO;
import nus.edu.u.system.domain.vo.reg.RegSearchReqVO;
import nus.edu.u.system.domain.vo.reg.RegSearchRespVO;
import nus.edu.u.system.service.user.RegService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registration controller
 *
 * @author Lu Shuwen
 * @date 2025-09-10
 */
@RestController
@RequestMapping("/system/reg")
@Validated
@Slf4j
public class RegController {

  @Resource private RegService regService;

  @PostMapping("/search")
  public CommonResult<RegSearchRespVO> search(@RequestBody @Valid RegSearchReqVO regSearchReqVO) {
    return success(regService.search(regSearchReqVO));
  }

  @PostMapping("/member")
  public CommonResult<Boolean> registerAsMember(@RequestBody @Valid RegMemberReqVO regMemberReqVO) {
    boolean isSuccess = regService.registerAsMember(regMemberReqVO);
    return isSuccess ? success(true) : error(REG_FAIL);
  }

  @PostMapping("/organizer")
  public CommonResult<Boolean> registerAsOrganizer(
      @RequestBody @Valid RegOrganizerReqVO regOrganizerReqVO) {
    boolean isSuccess = regService.registerAsOrganizer(regOrganizerReqVO);
    return isSuccess ? success(true) : error(REG_FAIL);
  }
}
