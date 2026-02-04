package com.befapress.controller;

import com.befapress.dto.request.CreateOpinionRequest;
import com.befapress.dto.response.MessageResponse;
import com.befapress.dto.response.OpinionResponse;
import com.befapress.dto.response.PageResponse;
import com.befapress.service.OpinionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/intellectual/opinions")
@RequiredArgsConstructor
@Tag(name = "Intellectual - Opinions", description = "Opinion writing APIs for intellectuals")
@PreAuthorize("hasRole('ROLE_INTELLECTUAL')")
public class IntellectualOpinionController {

    private final OpinionService opinionService;

    @GetMapping
    @Operation(summary = "Get my opinions")
    public ResponseEntity<PageResponse<OpinionResponse>> getMyOpinions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<OpinionResponse> response = opinionService.getMyOpinions(userDetails.getUsername(), page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create a new opinion")
    public ResponseEntity<OpinionResponse> createOpinion(
            @Valid @RequestBody CreateOpinionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        OpinionResponse response = opinionService.createOpinion(request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update my opinion (only drafts)")
    public ResponseEntity<OpinionResponse> updateOpinion(
            @PathVariable Long id,
            @Valid @RequestBody CreateOpinionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        OpinionResponse response = opinionService.updateOpinion(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit opinion for review")
    public ResponseEntity<OpinionResponse> submitForReview(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        OpinionResponse response = opinionService.submitOpinion(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete my opinion (only drafts/rejected)")
    public ResponseEntity<MessageResponse> deleteOpinion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        opinionService.deleteOwnOpinion(id, userDetails.getUsername());
        return ResponseEntity.ok(MessageResponse.success("Opinion deleted successfully"));
    }
}
