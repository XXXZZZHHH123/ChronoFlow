package nus.edu.u.system.mapper.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import java.io.Serializable;
import java.util.*;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.UserPermissionDTO;
import nus.edu.u.system.domain.dto.UserRoleDTO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.ResultHandler;
import org.junit.jupiter.api.Test;

class UserMapperTest {

    static {
        MybatisConfiguration configuration = new MybatisConfiguration();
        GlobalConfigUtils.setGlobalConfig(configuration, GlobalConfigUtils.defaults());
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        TableInfoHelper.initTableInfo(assistant, UserDO.class);
    }

    @Test
    void existsUsername_returnsTrueWhenRecordFound() {
        StubUserMapper mapper = new StubUserMapper();
        mapper.selectCountResult = 1L;

        assertThat(mapper.existsUsername("alice", null)).isTrue();
        mapper.selectCountResult = 0L;
        assertThat(mapper.existsUsername("alice", null)).isFalse();
    }

    @Test
    void existsEmail_respectsExcludeId() {
        StubUserMapper mapper = new StubUserMapper();
        mapper.selectCountResult = 0L;
        assertThat(mapper.existsEmail("test@example.com", 10L)).isFalse();

        mapper.selectCountResult = 2L;
        assertThat(mapper.existsEmail("test@example.com", 10L)).isTrue();
    }

    @Test
    void existsPhone_returnsFalseWhenNoMatch() {
        StubUserMapper mapper = new StubUserMapper();
        mapper.selectCountResult = 0L;
        assertThat(mapper.existsPhone("12345678", null)).isFalse();
    }

    @Test
    void existsPhone_returnsTrueWhenRecordFound() {
        StubUserMapper mapper = new StubUserMapper();
        mapper.selectCountResult = 5L;
        assertThat(mapper.existsPhone("12345678", 2L)).isTrue();
    }

    @Test
    void selectIdByEmail_returnsIdWhenExists() {
        StubUserMapper mapper = new StubUserMapper();
        mapper.selectOneResult = UserDO.builder().id(42L).build();
        assertThat(mapper.selectIdByEmail("owner@example.com")).isEqualTo(42L);

        mapper.selectOneResult = null;
        assertThat(mapper.selectIdByEmail("missing@example.com")).isNull();
    }

    @Test
    void selectExistingEmails_returnsExistingOnly() {
        StubUserMapper mapper = new StubUserMapper();
        mapper.selectObjsResult = List.of("found@example.com");

        Set<String> emails =
                mapper.selectExistingEmails(List.of("found@example.com", "missing@example.com"));

        assertThat(emails).containsExactly("found@example.com");
        assertThat(mapper.selectExistingEmails(Collections.emptyList())).isEmpty();
        assertThat(mapper.selectExistingEmails(null)).isEmpty();
    }

    @Test
    void selectExistingEmails_deduplicatesDbResults() {
        StubUserMapper mapper = new StubUserMapper();
        mapper.selectObjsResult = List.of("dup@example.com", "dup@example.com");

        Set<String> emails = mapper.selectExistingEmails(List.of("dup@example.com"));

        assertThat(emails).containsExactly("dup@example.com");
    }

    private static class StubUserMapper implements UserMapper {

        long selectCountResult;
        UserDO selectOneResult;
        List<Object> selectObjsResult = Collections.emptyList();

        @Override
        public Long selectCount(Wrapper<UserDO> queryWrapper) {
            return selectCountResult;
        }

        @Override
        public <E> List<E> selectObjs(Wrapper<UserDO> queryWrapper) {
            return (List<E>) selectObjsResult;
        }

        @Override
        public UserDO selectOne(Wrapper<UserDO> queryWrapper) {
            return selectOneResult;
        }

        // ===== Methods required by BaseMapper but unused in tests =====
        @Override
        public int insert(UserDO entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteById(Serializable id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteById(UserDO entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int delete(Wrapper<UserDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteBatchIds(Collection<?> idList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateById(UserDO entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(UserDO entity, Wrapper<UserDO> updateWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserDO selectById(Serializable id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<UserDO> selectBatchIds(Collection<? extends Serializable> idList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectBatchIds(
                Collection<? extends Serializable> idList, ResultHandler<UserDO> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<UserDO> selectList(Wrapper<UserDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectList(Wrapper<UserDO> queryWrapper, ResultHandler<UserDO> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<UserDO> selectList(IPage<UserDO> page, Wrapper<UserDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectList(
                IPage<UserDO> page,
                Wrapper<UserDO> queryWrapper,
                ResultHandler<UserDO> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Map<String, Object>> selectMaps(Wrapper<UserDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectMaps(
                Wrapper<UserDO> queryWrapper, ResultHandler<Map<String, Object>> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Map<String, Object>> selectMaps(
                IPage<? extends Map<String, Object>> page, Wrapper<UserDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectMaps(
                IPage<? extends Map<String, Object>> page,
                Wrapper<UserDO> queryWrapper,
                ResultHandler<Map<String, Object>> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E> void selectObjs(Wrapper<UserDO> queryWrapper, ResultHandler<E> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteByMap(Map<String, Object> columnMap) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<UserDO> selectByMap(Map<String, Object> columnMap) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectByMap(
                Map<String, Object> columnMap, ResultHandler<UserDO> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <P extends IPage<UserDO>> P selectPage(P page, Wrapper<UserDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <P extends IPage<Map<String, Object>>> P selectMapsPage(
                P page, Wrapper<UserDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        // ===== Custom UserMapper methods (unused) =====
        @Override
        public UserRoleDTO selectUserWithRole(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserDO selectRawById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<UserRoleDTO> selectAllUsersWithRoles() {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserDO selectByUsername(String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<UserPermissionDTO> selectUserWithPermission(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserDO selectByIdWithoutTenant(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Integer updateByIdWithoutTenant(UserDO userDO) {
            throw new UnsupportedOperationException();
        }
    }
}
