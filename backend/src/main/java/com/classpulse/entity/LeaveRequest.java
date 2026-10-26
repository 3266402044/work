package com.classpulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("leave_request")
public class LeaveRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long taskId;
    private String reason;
    private String proofUrl;
    private String status;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private String remark;
    private LocalDateTime createTime;
}
