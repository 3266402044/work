package com.classpulse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SignRequest {
    @NotBlank(message = "请选择签到方式")
    private String method;

    private String qrToken;
    private String passcode;
    private BigDecimal longitude;
    private BigDecimal latitude;
}
