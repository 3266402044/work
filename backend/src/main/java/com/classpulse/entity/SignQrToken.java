package com.classpulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sign_qr_token")
public class SignQrToken {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String tokenHash;
    private LocalDateTime expiredAt;
    private Integer usedCount;
    private LocalDateTime createTime;
}
