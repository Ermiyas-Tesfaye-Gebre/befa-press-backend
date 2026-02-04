package com.befapress.controller;

import com.befapress.dto.request.CreateRuleRequest;
import com.befapress.dto.response.ModerationRuleResponse;
import com.befapress.entity.ModerationRule;
import com.befapress.service.ContentModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/moderation")
@RequiredArgsConstructor
@Tag(name = "Moderation", description = "Admin content moderation rule management")
public class ModerationController {

    private final ContentModerationService moderationService;

    @GetMapping("/rules")
    @Operation(summary = "Get all custom moderation rules")
    public ResponseEntity<List<ModerationRuleResponse>> getAllRules() {
        List<ModerationRule> rules = moderationService.getAllRules();
        List<ModerationRuleResponse> response = rules.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rules")
    @Operation(summary = "Add a new moderation rule")
    public ResponseEntity<ModerationRuleResponse> addRule(@Valid @RequestBody CreateRuleRequest request) {
        ModerationRule rule = moderationService.addRule(request);
        return ResponseEntity.ok(mapToResponse(rule));
    }

    private ModerationRuleResponse mapToResponse(ModerationRule rule) {
        return ModerationRuleResponse.builder()
                .id(rule.getId())
                .category(rule.getCategory())
                .language(rule.getLanguage())
                .pattern(rule.getPattern())
                .createdAt(rule.getCreatedAt())
                .build();
    }

    @DeleteMapping("/rules/{id}")
    @Operation(summary = "Delete a moderation rule")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        moderationService.deleteRule(id);
        return ResponseEntity.ok().build();
    }
}
