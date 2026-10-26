package com.classpulse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.classpulse.entity.SysConfig;
import com.classpulse.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConfigService {
    private final SysConfigMapper configMapper;

    public static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put("default.sign.duration.minutes", "15");
        DEFAULTS.put("default.location.radius", "150");
        DEFAULTS.put("late.threshold.minutes", "5");
        DEFAULTS.put("rollcall.response.seconds", "30");
        DEFAULTS.put("rollcall.absence.warning", "3");
        DEFAULTS.put("participation.sign.weight", "40");
        DEFAULTS.put("participation.late.weight", "20");
        DEFAULTS.put("participation.rollcall.weight", "25");
        DEFAULTS.put("participation.quiz.weight", "10");
        DEFAULTS.put("participation.race.weight", "5");
        DEFAULTS.put("allow.sync.rollcall.attendance", "true");
    }

    public static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
            Map.entry("default.sign.duration.minutes", "默认签到时长（分钟）"),
            Map.entry("default.location.radius", "默认定位签到半径（米）"),
            Map.entry("late.threshold.minutes", "迟到判定阈值（分钟）"),
            Map.entry("rollcall.response.seconds", "点名默认响应时长（秒）"),
            Map.entry("rollcall.absence.warning", "连续未响应预警阈值"),
            Map.entry("participation.sign.weight", "参与度-正常签到权重"),
            Map.entry("participation.late.weight", "参与度-迟到签到权重"),
            Map.entry("participation.rollcall.weight", "参与度-点名响应权重"),
            Map.entry("participation.quiz.weight", "参与度-小测完成权重"),
            Map.entry("participation.race.weight", "参与度-抢答权重"),
            Map.entry("allow.sync.rollcall.attendance", "允许将点名异常同步为出勤提醒")
    );

    public String get(String key) {
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key).last("limit 1"));
        if (config != null && config.getConfigValue() != null) {
            return config.getConfigValue();
        }
        return DEFAULTS.get(key);
    }

    public int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(get(key));
        } catch (Exception e) {
            return fallback;
        }
    }

    public double getDouble(String key, double fallback) {
        try {
            return Double.parseDouble(get(key));
        } catch (Exception e) {
            return fallback;
        }
    }

    public boolean getBool(String key, boolean fallback) {
        String value = get(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    public List<SysConfig> list() {
        List<SysConfig> list = configMapper.selectList(null);
        List<String> existingKeys = list.stream().map(SysConfig::getConfigKey).toList();
        DEFAULTS.forEach((key, value) -> {
            if (!existingKeys.contains(key)) {
                SysConfig config = new SysConfig();
                config.setConfigKey(key);
                config.setConfigValue(value);
                config.setDescription(DESCRIPTIONS.get(key));
                list.add(config);
            }
        });
        return list;
    }

    @Transactional
    public void update(Map<String, String> values) {
        values.forEach((key, value) -> {
            SysConfig existing = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                    .eq(SysConfig::getConfigKey, key).last("limit 1"));
            if (existing == null) {
                SysConfig config = new SysConfig();
                config.setConfigKey(key);
                config.setConfigValue(value);
                config.setDescription(DESCRIPTIONS.get(key));
                configMapper.insert(config);
            } else {
                existing.setConfigValue(value);
                configMapper.updateById(existing);
            }
        });
    }
}
