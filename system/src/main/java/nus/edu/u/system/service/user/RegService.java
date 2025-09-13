package nus.edu.u.system.service.user;

import nus.edu.u.system.domain.vo.reg.RegMemberReqVO;
import nus.edu.u.system.domain.vo.reg.RegOrganizerReqVO;
import nus.edu.u.system.domain.vo.reg.RegSearchReqVO;
import nus.edu.u.system.domain.vo.reg.RegSearchRespVO;

/**
 * Register service
 *
 * @author Lu Shuwen
 * @date 2025-09-10
 */
public interface RegService {

  /**
   * Search user info when sign up
   *
   * @param regSearchReqVO Organization id and user id
   * @return RegSearchRespVO
   */
  RegSearchRespVO search(RegSearchReqVO regSearchReqVO);

  /**
   * User register as member after getting organization info
   *
   * @param regMemberReqVO register property
   */
  boolean registerAsMember(RegMemberReqVO regMemberReqVO);

  boolean registerAsOrganizer(RegOrganizerReqVO regOrganizerReqVO);
}
