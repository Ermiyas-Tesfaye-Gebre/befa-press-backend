package com.befapress.config;

import com.befapress.entity.Opinion;
import com.befapress.repository.OpinionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpinionStatusFixer implements CommandLineRunner {

    private final OpinionRepository opinionRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Checking for opinions with 'APPROVED' status to migrate to 'PUBLISHED'...");

        // We can't easily query by "APPROVED" using the existing method if it's strict,
        // but we can just use a custom query or strict status check if the DB allows
        // strings.
        // Since findAll works, we can just iterate. Only for small datasets.
        // Ideally we'd add a method findByStatus("APPROVED") but let's try to be
        // minimally invasive.

        // Actually, let's just add the query method to repository if checking all is
        // too heavy?
        // No, current dataset is small (as per user context).

        List<Opinion> allOpinions = opinionRepository.findAll();
        int count = 0;
        for (Opinion opinion : allOpinions) {
            if ("APPROVED".equalsIgnoreCase(opinion.getStatus())) {
                opinion.setStatus("PUBLISHED");
                if (opinion.getPublishedAt() == null) {
                    opinion.setPublishedAt(LocalDateTime.now());
                }
                opinionRepository.save(opinion);
                count++;
            }
        }

        if (count > 0) {
            log.info("Successfully migrated {} opinions from 'APPROVED' to 'PUBLISHED'.", count);
        } else {
            log.info("No opinions needed migration.");
        }
    }
}
