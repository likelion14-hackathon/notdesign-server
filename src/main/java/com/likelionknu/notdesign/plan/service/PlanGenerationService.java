package com.likelionknu.notdesign.plan.service;

import com.likelionknu.notdesign.plan.data.dto.ai.PlanGenerationAiResponse;
import com.likelionknu.notdesign.plan.data.dto.request.PlanCreateRequestDto;
import com.likelionknu.notdesign.plan.data.dto.response.PlanCreateResponseDto;
import com.likelionknu.notdesign.plan.data.dto.response.PlanDetailItemResponseDto;
import com.likelionknu.notdesign.plan.data.entity.Plan;
import com.likelionknu.notdesign.plan.data.enums.PlanGenerationMode;
import com.likelionknu.notdesign.plan.exception.PlanGenerationFailedException;
import com.likelionknu.notdesign.plan.service.PlanCatalogService.Catalog;
import com.likelionknu.notdesign.plan.service.PlanCatalogService.CatalogEntry;
import com.likelionknu.notdesign.result.data.entity.Result;
import com.likelionknu.notdesign.result.data.exception.ResultNotFoundException;
import com.likelionknu.notdesign.result.data.repository.ResultRepository;
import com.likelionknu.notdesign.user.data.entity.User;
import com.likelionknu.notdesign.user.data.exception.UserNotFoundException;
import com.likelionknu.notdesign.user.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanGenerationService {
    private final UserRepository userRepository;
    private final ResultRepository resultRepository;
    private final PlanCatalogService planCatalogService;
    private final PlanPromptBuilder planPromptBuilder;
    private final PlanAiClient planAiClient;
    private final PlanGenerationValidator planGenerationValidator;
    private final PlanAssembler planAssembler;

    /**
     * 플랜을 생성한다.
     *
     * @param email   요청 사용자
     * @param request 생성 모드와 모드별 입력
     * @return 생성된 플랜 미리보기(planId 포함)
     */
    public PlanCreateResponseDto generate(String email, PlanCreateRequestDto request) {
        PlanGenerationMode mode = request.getMode();

        Catalog catalog = planCatalogService.load();
        String systemPrompt = planPromptBuilder.buildSystem(catalog);
        String userPrompt = buildUserPrompt(email, request, mode);

        PlanGenerationAiResponse aiResponse = generateWithRetry(systemPrompt, userPrompt, catalog, mode.getDurationWeeks());
        Plan plan = planAssembler.assemble(mode, aiResponse, catalog);

        return toResponse(plan, mode, aiResponse, catalog);
    }

    private String buildUserPrompt(String email, PlanCreateRequestDto request, PlanGenerationMode mode) {
        return switch (mode) {
            case NEW -> {
                User user = userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);
                Result result = resultRepository.findAllByUserOrderByMeasuredAtDesc(user).stream()
                        .findFirst()
                        .orElseThrow(ResultNotFoundException::new);
                yield planPromptBuilder.buildUserNew(
                        result.getPigmentation(), result.getHydration(), result.getErythema(), request.getMonthlyBudget());
            }
            case TRIAL, ADJUST, NEXT ->
                    throw new PlanGenerationFailedException(mode + " 모드는 아직 구현되지 않았습니다.");
        };
    }

    private PlanGenerationAiResponse generateWithRetry(String systemPrompt, String userPrompt,
                                                       Catalog catalog, int durationWeeks) {
        PlanGenerationAiResponse response = planAiClient.generate(systemPrompt, userPrompt);
        List<String> violations = planGenerationValidator.validate(response, catalog, durationWeeks);
        if (violations.isEmpty()) {
            return response;
        }

        log.warn("[PlanGenerationService] AI 응답 규칙 위반, 1회 재요청: {}", violations);
        String retryPrompt = userPrompt + "\n\n# 이전 응답의 오류 (반드시 고쳐서 다시 생성)\n- "
                + String.join("\n- ", violations);

        PlanGenerationAiResponse retried = planAiClient.generate(systemPrompt, retryPrompt);
        List<String> retryViolations = planGenerationValidator.validate(retried, catalog, durationWeeks);
        if (!retryViolations.isEmpty()) {
            throw new PlanGenerationFailedException("재요청 후에도 규칙 위반: " + retryViolations);
        }

        return retried;
    }

    private PlanCreateResponseDto toResponse(Plan plan, PlanGenerationMode mode,
                                             PlanGenerationAiResponse aiResponse, Catalog catalog) {
        List<PlanDetailItemResponseDto> items = aiResponse.items().stream()
                .map(item -> {
                    CatalogEntry entry = catalog.resolve(item.itemEffectId().longValue());
                    return PlanDetailItemResponseDto.builder()
                            .category(entry.category())
                            .categoryName(entry.category().getDisplayName())
                            .name(entry.name())
                            .frequency(item.frequency())
                            .price(entry.price())
                            .weeks(item.weeks())
                            .reason(item.reason())
                            .build();
                })
                .toList();

        return PlanCreateResponseDto.builder()
                .planId(plan.getId())
                .mode(mode)
                .planSummary(plan.getSummary())
                .durationWeeks(plan.getDurationWeeks())
                .totalPrice(plan.getTotalPrice())
                .items(items)
                .build();
    }
}
