package nus.edu.u.system.service.event;

import java.util.List;
import nus.edu.u.system.domain.vo.event.*;

public interface EventService {
    EventRespVO createEvent(EventCreateReqVO reqVO);

    EventRespVO getByEventId(Long eventId);

    List<EventRespVO> getByOrganizerId(Long organizerId);

    UpdateEventRespVO updateEvent(Long id, EventUpdateReqVO req);

    Boolean deleteEvent(Long id);

    Boolean restoreEvent(Long id);
    //
    //    Boolean updateEvent(EventUpdateReqVO reqVO);
    //
    //    Boolean deleteEvent(Long id);
    //
    //    EventRespVO getEvent(Long id);
    //
    //    PageResult<EventRespVO> getEventPage(EventPageReqVO reqVO);
}
