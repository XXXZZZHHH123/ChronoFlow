package nus.edu.u.system.service.attendee;

import static nus.edu.u.common.constant.Constants.SESSION_TENANT_ID;
import static org.assertj.core.api.Assertions.*;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import nus.edu.u.system.domain.dataobject.attendee.EventAttendeeDO;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.tenant.TenantDO;
import nus.edu.u.system.domain.vo.attendee.AttendeeQrCodeRespVO;
import nus.edu.u.system.domain.vo.attendee.AttendeeReqVO;
import nus.edu.u.system.domain.vo.checkin.CheckInRespVO;
import nus.edu.u.system.domain.vo.checkin.GenerateQrCodesReqVO;
import nus.edu.u.system.domain.vo.checkin.GenerateQrCodesRespVO;
import nus.edu.u.system.domain.vo.qrcode.QrCodeReqVO;
import nus.edu.u.system.domain.vo.qrcode.QrCodeRespVO;
import nus.edu.u.system.mapper.attendee.EventAttendeeMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.tenant.TenantMapper;
import nus.edu.u.system.service.notification.AttendeeEmailService;
import nus.edu.u.system.service.qrcode.QrCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttendeeServiceImplTest {

    private AttendeeServiceImpl attendeeService;
    private InMemoryAttendeeMapper attendeeMapper;
    private InMemoryEventMapper eventMapper;
    private RecordingQrCodeService qrCodeService;

    @BeforeEach
    void setUp() throws Exception {
        attendeeService = new AttendeeServiceImpl();
        attendeeMapper = new InMemoryAttendeeMapper();
        eventMapper = new InMemoryEventMapper();
        qrCodeService = new RecordingQrCodeService();

        setField("attendeeMapper", attendeeMapper);
        setField("eventMapper", eventMapper);
        setField("qrCodeService", qrCodeService);
        setField("tenantMapper", new InMemoryTenantMapper());
        setField("baseUrl", "http://test-host");
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
    void delete_whenFoundRemoves() {
        attendeeMapper.save(
                EventAttendeeDO.builder()
                        .id(10L)
                        .eventId(1L)
                        .attendeeEmail("del@example.com")
                        .checkInToken("tok-del")
                        .build());

        attendeeService.delete(10L);

        assertThat(attendeeMapper.selectById(10L)).isNull();
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
    void update_generatesTokenAndQrCodeWhenMissing() {
        eventMapper.save(
                EventDO.builder()
                        .id(400L)
                        .name("Summit")
                        .description("Desc")
                        .location("Hall A")
                        .startTime(LocalDateTime.now())
                        .endTime(LocalDateTime.now().plusHours(3))
                        .status(1)
                        .build());
        attendeeMapper.save(
                EventAttendeeDO.builder()
                        .id(20L)
                        .eventId(400L)
                        .attendeeEmail("old@example.com")
                        .attendeeName("Old")
                        .attendeeMobile("000")
                        .checkInStatus(0)
                        .build());

        try (TenantSession ignored = new TenantSession(99L)) {
            AttendeeReqVO req = attendee("new@example.com", "New User", "111");

            AttendeeQrCodeRespVO resp = attendeeService.update(20L, req);

            assertThat(resp.getAttendeeEmail()).isEqualTo("new@example.com");
            assertThat(resp.getCheckInToken()).isNotBlank();
            assertThat(resp.getQrCodeUrl())
                    .isEqualTo(
                            "http://test-host/system/attendee/scan?token="
                                    + resp.getCheckInToken());
            EventAttendeeDO stored = attendeeMapper.selectById(20L);
            assertThat(stored.getCheckInToken()).isEqualTo(resp.getCheckInToken());
            assertThat(stored.getQrCodeGeneratedTime()).isNotNull();
        }
    }

    @Test
    void update_whenAlreadyCheckedIn_throws() {
        attendeeMapper.save(
                EventAttendeeDO.builder()
                        .id(21L)
                        .eventId(401L)
                        .attendeeEmail("checked@example.com")
                        .checkInStatus(1)
                        .build());

        assertThatThrownBy(() -> attendeeService.update(21L, attendee("x@y.com", "Name", "123")))
                .isInstanceOf(nus.edu.u.common.exception.ServiceException.class);
    }

    @Test
    void generateQrCodesForAttendees_partialSuccessReturnsList() {
        eventMapper.save(
                EventDO.builder()
                        .id(500L)
                        .name("Expo")
                        .description("desc")
                        .location("loc")
                        .startTime(LocalDateTime.now())
                        .endTime(LocalDateTime.now().plusHours(1))
                        .status(1)
                        .build());

        GenerateQrCodesReqVO req = new GenerateQrCodesReqVO();
        req.setEventId(500L);
        req.setAttendees(
                List.of(
                        attendee("first@example.com", "First", "111"),
                        attendee("first@example.com", "Duplicate", "222")));

        try (TenantSession ignored = new TenantSession(1L)) {
            GenerateQrCodesRespVO resp = attendeeService.generateQrCodesForAttendees(req);

            assertThat(resp.getTotalCount()).isEqualTo(1);
            assertThat(resp.getAttendees()).hasSize(1);
        }
    }

    @Test
    void generateQrCodesForAttendees_whenAllFailThrows() {
        eventMapper.save(
                EventDO.builder()
                        .id(501L)
                        .name("Fail Event")
                        .startTime(LocalDateTime.now())
                        .endTime(LocalDateTime.now().plusHours(1))
                        .status(1)
                        .build());

        GenerateQrCodesReqVO req = new GenerateQrCodesReqVO();
        req.setEventId(501L);
        req.setAttendees(List.of(attendee("", "No Email", "333")));

        assertThatThrownBy(() -> attendeeService.generateQrCodesForAttendees(req))
                .isInstanceOf(nus.edu.u.common.exception.ServiceException.class)
                .hasMessageContaining("Attendee already exists");
    }

    @Test
    void getCheckInToken_generatesWhenMissing() {
        eventMapper.save(
                EventDO.builder()
                        .id(600L)
                        .name("Token Event")
                        .startTime(LocalDateTime.now())
                        .endTime(LocalDateTime.now().plusHours(1))
                        .status(1)
                        .build());
        attendeeMapper.save(
                EventAttendeeDO.builder()
                        .id(30L)
                        .eventId(600L)
                        .attendeeEmail("token@example.com")
                        .checkInStatus(0)
                        .build());

        String token = attendeeService.getCheckInToken(600L, "token@example.com");

        assertThat(token).isNotBlank();
        assertThat(attendeeMapper.selectById(30L).getCheckInToken()).isEqualTo(token);
    }

    private AttendeeReqVO attendee(String email, String name, String mobile) {
        AttendeeReqVO req = new AttendeeReqVO();
        req.setEmail(email);
        req.setName(name);
        req.setMobile(mobile);
        return req;
    }

    private static final class RecordingQrCodeService implements QrCodeService {
        private final List<String> requestedTokens = new ArrayList<>();

        @Override
        public QrCodeRespVO generateQrCode(QrCodeReqVO reqVO) {
            return null;
        }

        @Override
        public byte[] generateQrCodeBytes(String content, int size, String format) {
            return content.getBytes();
        }

        @Override
        public QrCodeRespVO generateEventCheckInQrWithToken(String checkInToken) {
            requestedTokens.add(checkInToken);
            String base64 =
                    Base64.getEncoder()
                            .encodeToString(
                                    ("qr:" + checkInToken)
                                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return QrCodeRespVO.builder()
                    .base64Image(base64)
                    .contentType("image/png")
                    .size(128)
                    .build();
        }
    }

    private static final class TenantSession implements AutoCloseable {
        private final StpLogic previous;

        private TenantSession(Long tenantId) {
            this.previous = StpUtil.getStpLogic();
            StpUtil.setStpLogic(new StubTenantLogic(tenantId));
        }

        @Override
        public void close() {
            StpUtil.setStpLogic(previous);
        }
    }

    private static final class StubTenantLogic extends StpLogic {
        private final SaSession session;

        private StubTenantLogic(Long tenantId) {
            super(StpUtil.TYPE);
            this.session = new SaSession("test");
            this.session.set(SESSION_TENANT_ID, tenantId);
        }

        @Override
        public SaSession getSession() {
            return session;
        }
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

    private static final class InMemoryAttendeeMapper implements EventAttendeeMapper {
        private final Map<Long, EventAttendeeDO> byId = new HashMap<>();
        private final Map<String, EventAttendeeDO> byToken = new HashMap<>();

        void save(EventAttendeeDO attendee) {
            byToken.entrySet()
                    .removeIf(e -> Objects.equals(e.getValue().getId(), attendee.getId()));
            byId.put(attendee.getId(), attendee);
            if (attendee.getCheckInToken() != null) {
                byToken.put(attendee.getCheckInToken(), attendee);
            }
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
