package wcpredictor.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcpredictor.entity.Setting;
import wcpredictor.repository.SettingRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TimeService {

    private static final String KEY_ENABLED = "time_override_enabled";
    private static final String KEY_VALUE = "time_override_value";

    private final SettingRepository settingRepository;

    public TimeService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public LocalDateTime now() {
        if (isOverrideEnabled()) {
            String val = getOverrideValue();
            if (val != null && !val.isBlank()) {
                try {
                    return LocalDateTime.parse(val);
                } catch (Exception ignored) {}
            }
        }
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public boolean isOverrideEnabled() {
        return settingRepository.findByName(KEY_ENABLED)
                .map(s -> "true".equalsIgnoreCase(s.getValue())).orElse(false);
    }

    public String getOverrideValue() {
        return settingRepository.findByName(KEY_VALUE)
                .map(Setting::getValue).orElse(null);
    }

    @Transactional
    public void enableOverride(LocalDateTime time) {
        saveSetting(KEY_ENABLED, "true", "BOOLEAN");
        saveSetting(KEY_VALUE, time.toString(), "STRING");
    }

    @Transactional
    public void disableOverride() {
        settingRepository.findByName(KEY_ENABLED).ifPresent(settingRepository::delete);
        settingRepository.findByName(KEY_VALUE).ifPresent(settingRepository::delete);
    }

    private void saveSetting(String name, String value, String type) {
        var s = settingRepository.findByName(name).orElse(new Setting());
        s.setName(name);
        s.setValue(value);
        s.setValueType(type);
        settingRepository.save(s);
    }
}
