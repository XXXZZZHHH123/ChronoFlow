package nus.edu.u.system.service.task.action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nus.edu.u.system.enums.task.TaskActionEnum;
import org.springframework.stereotype.Component;

/**
 * @author Lu Shuwen
 * @date 2025-10-02
 */
@Component
public class TaskActionFactory {

    private final Map<TaskActionEnum, TaskStrategy> map = new HashMap<>();

    public TaskActionFactory(List<TaskStrategy> taskStrategies) {
        for (TaskStrategy taskStrategy : taskStrategies) {
            map.put(taskStrategy.getType(), taskStrategy);
        }
    }

    public TaskStrategy getStrategy(TaskActionEnum action) {
        return map.get(action);
    }
}
