package com.classpulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sign_record")
public class SignRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long studentId;
    private LocalDateTime signTime;
    private String status;
    private String method;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private BigDecimal distanceMeters;
    private String remark;
    private Long operatorId;
    private LocalDateTime createTime;
}
