package nus.edu.u.system.service.attendee;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import nus.edu.u.system.domain.dataobject.attendee.EventAttendeeDO;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.tenant.TenantDO;
import nus.edu.u.system.domain.vo.attendee.AttendeeQrCodeRespVO;
import nus.edu.u.system.domain.vo.checkin.CheckInRespVO;
import nus.edu.u.system.domain.vo.qrcode.QrCodeReqVO;
import nus.edu.u.system.domain.vo.qrcode.QrCodeRespVO;
import nus.edu.u.system.mapper.attendee.EventAttendeeMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.tenant.TenantMapper;
import nus.edu.u.system.service.email.AttendeeEmailService;
import nus.edu.u.system.service.qrcode.QrCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttendeeServiceImplTest {

    private AttendeeServiceImpl attendeeService;
    private InMemoryAttendeeMapper attendeeMapper;
    private InMemoryEventMapper eventMapper;

    @BeforeEach
    void setUp() throws Exception {
        attendeeService = new AttendeeServiceImpl();
        attendeeMapper = new InMemoryAttendeeMapper();
        eventMapper = new InMemoryEventMapper();

        setField("attendeeMapper", attendeeMapper);
        setField("eventMapper", eventMapper);
        setField("qrCodeService", new NoopQrCodeService());
        setField("attendeeEmailService", new NoopAttendeeEmailService());
        setField("tenantMapper", new InMemoryTenantMapper());
    }

    private void setField(String name, Object value) throws Exception {
        Field field = AttendeeServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(attendeeService, value);
    }

    @Test
    void list_returnsConvertedResponses() {
        attendeeMapper.save(
                EventAttendeeDO.builder()
                        .id(1L)
                        .eventId(100L)
                        .attendeeEmail("user@example.com")
                        .attendeeName("User")
                        .attendeeMobile("123")
                        .checkInStatus(0)
                        .checkInToken("token")
                        .build());

        List<AttendeeQrCodeRespVO> list = attendeeService.list(100L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getAttendeeEmail()).isEqualTo("user@example.com");
    }

    @Test
    void delete_whenNotFoundThrows() {
        assertThatThrownBy(() -> attendeeService.delete(1L))
                .isInstanceOf(nus.edu.u.common.exception.ServiceException.class);
    }

    @Test
    void checkIn_updatesStatus() {
        eventMapper.save(
                EventDO.builder()
                        .id(200L)
                        .startTime(LocalDateTime.now().minusHours(1))
                        .endTime(LocalDateTime.now().plusHours(2))
                        .status(1)
                        .name("Event")
                        .build());
        attendeeMapper.save(
                EventAttendeeDO.builder()
                        .id(2L)
                        .eventId(200L)
                        .attendeeEmail("attendee@example.com")
                        .attendeeName("Attendee")
                        .checkInToken("token-1")
                        .checkInStatus(0)
                        .build());

        CheckInRespVO resp = attendeeService.checkIn("token-1");

        assertThat(resp.getSuccess()).isTrue();
        assertThat(attendeeMapper.byId.get(2L).getCheckInStatus()).isEqualTo(1);
    }

    @Test
    void getAttendeeInfo_returnsDetails() {
        eventMapper.save(
                EventDO.builder()
                        .id(300L)
                        .name("Expo")
                        .startTime(LocalDateTime.now())
                        .endTime(LocalDateTime.now().plusHours(1))
                        .status(1)
                        .build());
        attendeeMapper.save(
                EventAttendeeDO.builder()
                        .id(3L)
                        .eventId(300L)
                        .attendeeEmail("info@example.com")
                        .attendeeName("Info")
                        .checkInToken("tok-info")
                        .checkInStatus(0)
                        .build());

        var info = attendeeService.getAttendeeInfo("tok-info");

        assertThat(info.getEventName()).isEqualTo("Expo");
        assertThat(info.getAttendeeEmail()).isEqualTo("info@example.com");
    }

    private static final class NoopQrCodeService implements QrCodeService {
        @Override
        public QrCodeRespVO generateQrCode(QrCodeReqVO reqVO) {
            return null;
        }

        @Override
        public byte[] generateQrCodeBytes(String content, int size, String format) {
            return new byte[0];
        }

        @Override
        public QrCodeRespVO generateEventCheckInQrWithToken(String checkInToken) {
            return null;
        }
    }

    private static final class NoopAttendeeEmailService implements AttendeeEmailService {
        @Override
        public String sendAttendeeInvite(
                nus.edu.u.system.domain.vo.attendee.AttendeeInviteReqVO req) {
            return "sent";
        }
    }

    private static final class InMemoryAttendeeMapper implements EventAttendeeMapper {
        private final Map<Long, EventAttendeeDO> byId = new HashMap<>();
        private final Map<String, EventAttendeeDO> byToken = new HashMap<>();

        void save(EventAttendeeDO attendee) {
            byId.put(attendee.getId(), attendee);
            byToken.put(attendee.getCheckInToken(), attendee);
        }

        @Override
        public EventAttendeeDO selectById(java.io.Serializable id) {
            return byId.get(id);
        }

        @Override
        public int deleteById(java.io.Serializable id) {
            if (byId.remove(id) != null) {
                return 1;
            }
            return 0;
        }

        @Override
        public int updateById(EventAttendeeDO entity) {
            save(entity);
            return 1;
        }

        @Override
        public int insert(EventAttendeeDO entity) {
            if (entity.getId() == null) {
                entity.setId((long) (byId.size() + 1));
            }
            save(entity);
            return 1;
        }

        @Override
        public EventAttendeeDO selectByToken(String token) {
            return byToken.get(token);
        }

        @Override
        public EventAttendeeDO selectByEventAndEmail(Long eventId, String email) {
            return byId.values().stream()
                    .filter(a -> Objects.equals(a.getEventId(), eventId))
                    .filter(a -> Objects.equals(a.getAttendeeEmail(), email))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public java.util.List<EventAttendeeDO> selectByEventId(Long eventId) {
            List<EventAttendeeDO> list = new ArrayList<>();
            for (EventAttendeeDO attendee : byId.values()) {
                if (Objects.equals(attendee.getEventId(), eventId)) {
                    list.add(attendee);
                }
            }
            return list;
        }

        // Unused BaseMapper methods
        @Override
        public int deleteById(EventAttendeeDO entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int delete(
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventAttendeeDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteBatchIds(java.util.Collection<?> idList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(
                EventAttendeeDO entity,
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventAttendeeDO> updateWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.lang.Long selectCount(
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventAttendeeDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<EventAttendeeDO> selectBatchIds(
                java.util.Collection<? extends java.io.Serializable> idList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectBatchIds(
                java.util.Collection<? extends java.io.Serializable> idList,
                org.apache.ibatis.session.ResultHandler<EventAttendeeDO> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EventAttendeeDO selectOne(
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventAttendeeDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<EventAttendeeDO> selectList(
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventAttendeeDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectList(
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventAttendeeDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<EventAttendeeDO> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<EventAttendeeDO> selectList(
                com.baomidou.mybatisplus.core.metadata.IPage<EventAttendeeDO> page,
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventAttendeeDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectList(
                com.baomidou.mybatisplus.core.metadata.IPage<EventAttendeeDO> page,
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventAttendeeDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<EventAttendeeDO> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<java.util.Map<String, Object>> selectMaps(
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventAttendeeDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectMaps(
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventAttendeeDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<java.util.Map<String, Object>>
                        resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<java.util.Map<String, Object>> selectMaps(
                com.baomidou.mybatisplus.core.metadata.IPage<
                                ? extends java.util.Map<String, Object>>
                        page,
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventAttendeeDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectMaps(
                com.baomidou.mybatisplus.core.metadata.IPage<
                                ? extends java.util.Map<String, Object>>
                        page,
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventAttendeeDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<java.util.Map<String, Object>>
                        resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E> java.util.List<E> selectObjs(
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventAttendeeDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E> void selectObjs(
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventAttendeeDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<E> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <P extends com.baomidou.mybatisplus.core.metadata.IPage<EventAttendeeDO>>
                P selectPage(
                        P page,
                        com.baomidou.mybatisplus.core.conditions.Wrapper<EventAttendeeDO>
                                queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <
                        P extends
                                com.baomidou.mybatisplus.core.metadata.IPage<
                                                java.util.Map<String, Object>>>
                P selectMapsPage(
                        P page,
                        com.baomidou.mybatisplus.core.conditions.Wrapper<EventAttendeeDO>
                                queryWrapper) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class InMemoryEventMapper implements EventMapper {
        private final Map<Long, EventDO> events = new HashMap<>();

        void save(EventDO event) {
            events.put(event.getId(), event);
        }

        @Override
        public EventDO selectById(java.io.Serializable id) {
            return events.get(id);
        }

        @Override
        public EventDO selectRawById(Long id) {
            return events.get(id);
        }

        @Override
        public int restoreById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int insert(EventDO entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteById(java.io.Serializable id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteById(EventDO entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int delete(com.baomidou.mybatisplus.core.conditions.Wrapper<EventDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteBatchIds(java.util.Collection<?> idList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateById(EventDO entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(
                EventDO entity,
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventDO> updateWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<EventDO> selectBatchIds(
                java.util.Collection<? extends java.io.Serializable> idList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectBatchIds(
                java.util.Collection<? extends java.io.Serializable> idList,
                org.apache.ibatis.session.ResultHandler<EventDO> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.lang.Long selectCount(
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EventDO selectOne(
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<EventDO> selectList(
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectList(
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<EventDO> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<EventDO> selectList(
                com.baomidou.mybatisplus.core.metadata.IPage<EventDO> page,
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectList(
                com.baomidou.mybatisplus.core.metadata.IPage<EventDO> page,
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<EventDO> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<java.util.Map<String, Object>> selectMaps(
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectMaps(
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<java.util.Map<String, Object>>
                        resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<java.util.Map<String, Object>> selectMaps(
                com.baomidou.mybatisplus.core.metadata.IPage<
                                ? extends java.util.Map<String, Object>>
                        page,
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectMaps(
                com.baomidou.mybatisplus.core.metadata.IPage<
                                ? extends java.util.Map<String, Object>>
                        page,
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<java.util.Map<String, Object>>
                        resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E> java.util.List<E> selectObjs(
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E> void selectObjs(
                com.baomidou.mybatisplus.core.conditions.Wrapper<EventDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<E> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <P extends com.baomidou.mybatisplus.core.metadata.IPage<EventDO>> P selectPage(
                P page, com.baomidou.mybatisplus.core.conditions.Wrapper<EventDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <
                        P extends
                                com.baomidou.mybatisplus.core.metadata.IPage<
                                                java.util.Map<String, Object>>>
                P selectMapsPage(
                        P page,
                        com.baomidou.mybatisplus.core.conditions.Wrapper<EventDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class InMemoryTenantMapper implements TenantMapper {
        @Override
        public nus.edu.u.system.domain.dataobject.tenant.TenantDO selectById(
                java.io.Serializable id) {
            return TenantDO.builder().id((Long) id).name("Tenant").build();
        }

        @Override
        public int insert(nus.edu.u.system.domain.dataobject.tenant.TenantDO entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteById(java.io.Serializable id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteById(nus.edu.u.system.domain.dataobject.tenant.TenantDO entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int delete(
                com.baomidou.mybatisplus.core.conditions.Wrapper<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteBatchIds(java.util.Collection<?> idList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateById(nus.edu.u.system.domain.dataobject.tenant.TenantDO entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(
                nus.edu.u.system.domain.dataobject.tenant.TenantDO entity,
                com.baomidou.mybatisplus.core.conditions.Wrapper<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        updateWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<nus.edu.u.system.domain.dataobject.tenant.TenantDO> selectBatchIds(
                java.util.Collection<? extends java.io.Serializable> idList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectBatchIds(
                java.util.Collection<? extends java.io.Serializable> idList,
                org.apache.ibatis.session.ResultHandler<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.lang.Long selectCount(
                com.baomidou.mybatisplus.core.conditions.Wrapper<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public nus.edu.u.system.domain.dataobject.tenant.TenantDO selectOne(
                com.baomidou.mybatisplus.core.conditions.Wrapper<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<nus.edu.u.system.domain.dataobject.tenant.TenantDO> selectList(
                com.baomidou.mybatisplus.core.conditions.Wrapper<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectList(
                com.baomidou.mybatisplus.core.conditions.Wrapper<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        queryWrapper,
                org.apache.ibatis.session.ResultHandler<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<nus.edu.u.system.domain.dataobject.tenant.TenantDO> selectList(
                com.baomidou.mybatisplus.core.metadata.IPage<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        page,
                com.baomidou.mybatisplus.core.conditions.Wrapper<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectList(
                com.baomidou.mybatisplus.core.metadata.IPage<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        page,
                com.baomidou.mybatisplus.core.conditions.Wrapper<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        queryWrapper,
                org.apache.ibatis.session.ResultHandler<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<java.util.Map<String, Object>> selectMaps(
                com.baomidou.mybatisplus.core.conditions.Wrapper<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectMaps(
                com.baomidou.mybatisplus.core.conditions.Wrapper<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        queryWrapper,
                org.apache.ibatis.session.ResultHandler<java.util.Map<String, Object>>
                        resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<java.util.Map<String, Object>> selectMaps(
                com.baomidou.mybatisplus.core.metadata.IPage<
                                ? extends java.util.Map<String, Object>>
                        page,
                com.baomidou.mybatisplus.core.conditions.Wrapper<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectMaps(
                com.baomidou.mybatisplus.core.metadata.IPage<
                                ? extends java.util.Map<String, Object>>
                        page,
                com.baomidou.mybatisplus.core.conditions.Wrapper<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        queryWrapper,
                org.apache.ibatis.session.ResultHandler<java.util.Map<String, Object>>
                        resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E> java.util.List<E> selectObjs(
                com.baomidou.mybatisplus.core.conditions.Wrapper<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E> void selectObjs(
                com.baomidou.mybatisplus.core.conditions.Wrapper<
                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                        queryWrapper,
                org.apache.ibatis.session.ResultHandler<E> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <
                        P extends
                                com.baomidou.mybatisplus.core.metadata.IPage<
                                                nus.edu.u.system.domain.dataobject.tenant.TenantDO>>
                P selectPage(
                        P page,
                        com.baomidou.mybatisplus.core.conditions.Wrapper<
                                        nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                                queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <
                        P extends
                                com.baomidou.mybatisplus.core.metadata.IPage<
                                                java.util.Map<String, Object>>>
                P selectMapsPage(
                        P page,
                        com.baomidou.mybatisplus.core.conditions.Wrapper<
                                        nus.edu.u.system.domain.dataobject.tenant.TenantDO>
                                queryWrapper) {
            throw new UnsupportedOperationException();
        }
    }
}
