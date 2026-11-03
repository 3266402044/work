package com.classpulse.common;

import com.classpulse.entity.SignTask;

import java.time.LocalDateTime;

public enum TaskStatus {
    NOT_STARTED("未开始"),
    ONGOING("进行中"),
    CLOSED("已提前关闭"),
    ENDED("已结束");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static TaskStatus of(SignTask task, LocalDateTime now) {
        if (task.getClosedAt() != null) {
            return CLOSED;
        }
        if (now.isBefore(task.getStartTime())) {
            return NOT_STARTED;
        }
        if (!now.isAfter(task.getEndTime())) {
            return ONGOING;
        }
        return ENDED;
    }
}


