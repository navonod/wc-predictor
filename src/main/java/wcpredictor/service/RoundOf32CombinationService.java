package wcpredictor.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoundOf32CombinationService {

    private static final Logger log = LoggerFactory.getLogger(RoundOf32CombinationService.class);

    private final Map<Integer, Integer> maskToOption = new HashMap<>();
    private final List<Character> firstPlaceGroups = new ArrayList<>();
    private final Map<Integer, List<Character>> optionToThirdPlaceGroups = new HashMap<>();

    @PostConstruct
    public void load() {
        try (var reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("data/FWC26_Round_of_32_Combinations.csv").getInputStream(),
                StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                log.error("Round of 32 combinations CSV is empty");
                return;
            }

            String[] header = headerLine.split(",");
            for (String h : header) {
                if (h.length() >= 2 && h.charAt(0) == '1') {
                    firstPlaceGroups.add(h.charAt(1));
                }
            }
            log.info("First-place groups in bracket: {}", firstPlaceGroups);

            String line;
            while ((line = reader.readLine()) != null) {
                String[] cols = line.split(",");
                if (cols.length != 9) {
                    log.warn("Skipping malformed row: {}", line);
                    continue;
                }

                int option = Integer.parseInt(cols[0]);
                List<Character> groups = new ArrayList<>(8);
                int mask = 0;

                for (int i = 1; i <= 8; i++) {
                    char group = cols[i].charAt(1);
                    groups.add(group);
                    mask |= 1 << (group - 'A');
                }

                maskToOption.put(mask, option);
                optionToThirdPlaceGroups.put(option, groups);
            }

            log.info("Loaded {} Round of 32 combinations", maskToOption.size());

        } catch (Exception e) {
            log.error("Failed to load Round of 32 combinations", e);
        }
    }

    public Optional<Integer> findOption(Set<Character> thirdPlaceGroups) {
        if (thirdPlaceGroups == null || thirdPlaceGroups.size() != 8) {
            return Optional.empty();
        }
        int mask = 0;
        for (char g : thirdPlaceGroups) {
            if (g < 'A' || g > 'L') {
                return Optional.empty();
            }
            mask |= 1 << (g - 'A');
        }
        return Optional.ofNullable(maskToOption.get(mask));
    }

    public List<Character> getFirstPlaceGroups() {
        return Collections.unmodifiableList(firstPlaceGroups);
    }

    public List<Character> getThirdPlaceGroupsForOption(int option) {
        return Collections.unmodifiableList(
                optionToThirdPlaceGroups.getOrDefault(option, List.of()));
    }

    public List<List<Character>> getAllThirdPlaceGroupOptions() {
        return optionToThirdPlaceGroups.values().stream().distinct().toList();
    }

    public List<String> getPossibleThirdPlaceDescriptions() {
        List<String> result = new ArrayList<>(8);
        for (int slot = 0; slot < 8; slot++) {
            Set<Character> possible = new TreeSet<>();
            for (List<Character> groups : optionToThirdPlaceGroups.values()) {
                if (slot < groups.size()) possible.add(groups.get(slot));
            }
            result.add(possible.stream().map(String::valueOf)
                    .sorted().collect(Collectors.joining("/")));
        }
        return result;
    }
}
