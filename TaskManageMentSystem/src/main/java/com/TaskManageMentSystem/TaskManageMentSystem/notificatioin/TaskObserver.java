package com.TaskManageMentSystem.TaskManageMentSystem.notificatioin;

import com.TaskManageMentSystem.TaskManageMentSystem.dto.TaskEvent;

/** Observer pattern: anything that wants to react to task events implements this. */
public interface TaskObserver {

    void onEvent(TaskEvent event);
}
