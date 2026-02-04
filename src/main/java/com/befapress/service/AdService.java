package com.befapress.service;

import com.befapress.dto.AdDto;
import com.befapress.dto.response.PageResponse;
import com.befapress.entity.Advertisement;
import com.befapress.exception.ResourceNotFoundException;
import com.befapress.repository.AdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdService {

    private final AdRepository adRepository;

    /**
     * Get a random active ad for a specific zone
     */
    public AdDto getAdForZone(String zone) {
        List<Advertisement> fs = adRepository.findByPlacementZoneAndAdStatus(zone, "ACTIVE");

        LocalDateTime now = LocalDateTime.now();
        List<Advertisement> activeAds = fs.stream()
                .filter(ad -> (ad.getStartDate() == null || !ad.getStartDate().isAfter(now)) &&
                        (ad.getEndDate() == null || !ad.getEndDate().isBefore(now)))
                .collect(Collectors.toList());

        if (activeAds.isEmpty()) {
            return null;
        }
        // Return a random one if multiple exist
        Advertisement ad = activeAds.get((int) (Math.random() * activeAds.size()));

        // Track view async (simplification)
        incrementViews(ad.getId());

        return mapToDto(ad);
    }

    @Transactional
    public void incrementViews(Long adId) {
        // In production, use async/redis for high throughput
        adRepository.findById(adId).ifPresent(ad -> {
            ad.setViews(ad.getViews() + 1);
            adRepository.save(ad);
        });
    }

    @Transactional
    public void trackClick(Long adId) {
        adRepository.findById(adId).ifPresent(ad -> {
            ad.setClicks(ad.getClicks() + 1);
            adRepository.save(ad);
        });
    }

    // === ADMIN METHODS ===

    public PageResponse<AdDto> getAllAds(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Advertisement> adPage = adRepository.findAll(pageable);
        return mapToPageResponse(adPage);
    }

    public AdDto createAd(AdDto request) {
        Advertisement ad = mapToEntity(request);
        ad.setAdStatus("ACTIVE"); // Default active if dates valid
        ad = adRepository.save(ad);
        return mapToDto(ad);
    }

    public AdDto updateAd(Long id, AdDto request) {
        Advertisement ad = adRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ad", "id", id));

        ad.setTitle(request.getTitle());
        ad.setAdType(request.getAdType());
        ad.setPlacementZone(request.getPlacementZone());
        ad.setImageUrl(request.getImageUrl());
        ad.setVideoUrl(request.getVideoUrl());
        ad.setTargetUrl(request.getTargetUrl());
        ad.setScriptContent(request.getScriptContent());
        ad.setHeading(request.getHeading());
        ad.setDescription(request.getDescription());
        ad.setCtaText(request.getCtaText());
        ad.setStartDate(request.getStartDate());
        ad.setEndDate(request.getEndDate());

        if (request.getStatus() != null) {
            ad.setAdStatus(request.getStatus());
        }

        ad = adRepository.save(ad);
        return mapToDto(ad);
    }

    public void deleteAd(Long id) {
        adRepository.deleteById(id);
    }

    /**
     * Scheduled task to expire ads
     * Run every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void checkExpiredAds() {
        LocalDateTime now = LocalDateTime.now();
        List<Advertisement> expiredAds = adRepository.findByAdStatusAndEndDateBefore("ACTIVE", now);
        for (Advertisement ad : expiredAds) {
            ad.setAdStatus("EXPIRED");
            log.info("Ad {} expired automatically", ad.getId());
        }
        adRepository.saveAll(expiredAds);
    }

    // === MAPPERS ===

    private Advertisement mapToEntity(AdDto dto) {
        return Advertisement.builder()
                .title(dto.getTitle())
                .adType(dto.getAdType())
                .placementZone(dto.getPlacementZone())
                .imageUrl(dto.getImageUrl())
                .videoUrl(dto.getVideoUrl())
                .targetUrl(dto.getTargetUrl())
                .scriptContent(dto.getScriptContent())
                .heading(dto.getHeading())
                .description(dto.getDescription())
                .ctaText(dto.getCtaText())
                .startDate(dto.getStartDate())
                .position(dto.getPosition() != null ? dto.getPosition() : 0)
                .endDate(dto.getEndDate())
                .adStatus("ACTIVE")
                .build();
    }

    private AdDto mapToDto(Advertisement ad) {
        return AdDto.builder()
                .id(ad.getId())
                .title(ad.getTitle())
                .adType(ad.getAdType())
                .placementZone(ad.getPlacementZone())
                .imageUrl(ad.getImageUrl())
                .videoUrl(ad.getVideoUrl())
                .targetUrl(ad.getTargetUrl())
                .scriptContent(ad.getScriptContent())
                .heading(ad.getHeading())
                .description(ad.getDescription())
                .ctaText(ad.getCtaText())
                .startDate(ad.getStartDate())
                .endDate(ad.getEndDate())
                .status(ad.getAdStatus())
                .position(ad.getPosition())
                .views(ad.getViews())
                .clicks(ad.getClicks())
                .build();
    }

    private PageResponse<AdDto> mapToPageResponse(Page<Advertisement> page) {
        List<AdDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PageResponse.<AdDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
