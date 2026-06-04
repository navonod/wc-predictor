package wcpredictor.service;

import org.springframework.stereotype.Service;
import wcpredictor.entity.Setting;
import wcpredictor.repository.SettingRepository;

import java.util.List;
import java.util.Optional;

@Service
public class SettingService {

    private final SettingRepository settingRepository;

    public SettingService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public List<Setting> getAllSettings() {
        return settingRepository.findAll();
    }

    public Optional<Setting> findByName(String name) {
        return settingRepository.findByName(name);
    }

    public Setting save(Setting setting) {
        return settingRepository.save(setting);
    }

    public double getDoubleValue(String name, double defaultValue) {
        return findByName(name).map(s -> Double.parseDouble(s.getValue())).orElse(defaultValue);
    }
}
