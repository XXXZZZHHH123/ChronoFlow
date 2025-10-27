package nus.edu.u.system.domain.vo.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nus.edu.u.system.domain.vo.role.RoleRespVO;
import org.junit.jupiter.api.Test;

class LoginRespVOTest {

    @Test
    void builderStoresUserRolesAndRefreshToken() {
        RoleRespVO role = RoleRespVO.builder().id(1L).name("ADMIN").build();
        UserVO userVO =
                UserVO.builder().id(5L).name("member01").email("member@example.com").build();

        LoginRespVO respVO =
                LoginRespVO.builder()
                        .user(userVO)
                        .roles(List.of(role))
                        .refreshToken("refresh-123")
                        .build();

        assertThat(respVO.getUser()).isEqualTo(userVO);
        assertThat(respVO.getRoles()).containsExactly(role);
        assertThat(respVO.getRefreshToken()).isEqualTo("refresh-123");
    }
}
