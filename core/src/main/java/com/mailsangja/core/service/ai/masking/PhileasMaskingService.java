package com.mailsangja.core.service.ai.masking;

import ai.philterd.phileas.filters.Filter;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.CreditCard;
import ai.philterd.phileas.policy.filters.EmailAddress;
import ai.philterd.phileas.policy.filters.PhoneNumber;
import ai.philterd.phileas.policy.filters.Url;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.filters.custom.PhoneNumberRulesFilter;
import ai.philterd.phileas.services.filters.regex.CreditCardFilter;
import ai.philterd.phileas.services.filters.regex.EmailAddressFilter;
import ai.philterd.phileas.services.filters.regex.UrlFilter;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.CreditCardFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.EmailAddressFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.PhoneNumberFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.UrlFilterStrategy;
import com.mailsangja.core.common.exception.masking.MaskingErrorCode;
import com.mailsangja.core.common.exception.masking.MaskingException;
import com.mailsangja.core.dto.ai.masking.MaskingCommand;
import com.mailsangja.core.dto.ai.masking.MaskingDetectionResult;
import com.mailsangja.core.dto.ai.masking.MaskingResult;
import com.mailsangja.core.dto.ai.masking.MaskingScope;
import com.mailsangja.core.dto.ai.masking.MaskingTokenResult;
import com.mailsangja.core.dto.ai.masking.PiiType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PhileasMaskingService {

    private static final Map<FilterType, PiiType> FILTER_TYPE_MAP = Map.of(
            FilterType.EMAIL_ADDRESS, PiiType.EMAIL,
            FilterType.PHONE_NUMBER, PiiType.PHONE,
            FilterType.URL, PiiType.URL,
            FilterType.CREDIT_CARD, PiiType.CARD_NUMBER
    );

    private static final Pattern EMAIL = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern URL = Pattern.compile("\\bhttps?://[^\\s<>()]+|\\bwww\\.[^\\s<>()]+");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?82[-\\s]?)?0?1[016789][-\\s]?\\d{3,4}[-\\s]?\\d{4}(?!\\d)");
    private static final Pattern CARD = Pattern.compile("(?<!\\d)(?:\\d{4}[-\\s]?){3}\\d{4}(?!\\d)");
    private static final Pattern KOREAN_RRN = Pattern.compile("(?<!\\d)\\d{6}-[1-4]\\d{6}(?!\\d)");
    private static final Pattern ACCOUNT = Pattern.compile("(?<!\\d)(?:계좌|account|acct|입금)[^\\n\\r\\d]{0,12}(\\d{2,6}[-\\s]?\\d{2,6}[-\\s]?\\d{2,8})(?!\\d)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADDRESS = Pattern.compile("[가-힣A-Za-z0-9 ]{2,40}(?:시|도) ?[가-힣A-Za-z0-9 ]{1,40}(?:구|군) ?[가-힣A-Za-z0-9 ]{1,60}(?:로|길) ?\\d{1,5}(?:-\\d{1,5})?");
    private static final Pattern KOREAN_PERSON = Pattern.compile("(?:(?:수신|발신|담당|대표|문의|참조|to|from|dear)[:：\\s]+)([가-힣]{2,4})(?:\\s?(?:님|대리|과장|부장|팀장|대표))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENGLISH_PERSON = Pattern.compile("\\b(?:Dear|To|From)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+){0,2})\\b");

    private final Policy policy;
    private final List<Filter> filters;

    public PhileasMaskingService() {
        FilterConfiguration configuration = createFilterConfiguration();
        this.policy = createPolicy();
        this.filters = List.of(
                createEmailAddressFilter(configuration),
                new PhoneNumberRulesFilter(configuration),
                new UrlFilter(configuration, false),
                new CreditCardFilter(configuration, false, false, false)
        );
    }

    public MaskingResult mask(String text, MaskingCommand command) {
        if (text == null) {
            throw new MaskingException(MaskingErrorCode.INVALID_TEXT);
        }
        if (command == null) {
            throw new MaskingException(MaskingErrorCode.INVALID_COMMAND);
        }
        List<MaskingDetectionResult> detections = detectWithPhileas(text, command);
        detections.addAll(detectWithLocalPatterns(text, command));
        return replaceTokens(text, detections, command);
    }

    public String restore(String text, MaskingResult maskingResult) {
        if (text == null) {
            throw new MaskingException(MaskingErrorCode.INVALID_TEXT);
        }
        if (maskingResult == null) {
            throw new MaskingException(MaskingErrorCode.INVALID_RESULT);
        }
        String restored = text;
        for (Map.Entry<String, String> entry : maskingResult.restoreTokenMap().entrySet()) {
            restored = restored.replace(entry.getKey(), entry.getValue());
        }
        return restored;
    }

    private List<MaskingDetectionResult> detectWithPhileas(String text, MaskingCommand command) {
        List<MaskingDetectionResult> detections = new ArrayList<>();
        for (Filter filter : filters) {
            try {
                Filtered filtered = filter.filter(policy, text, 0, "ai-masking");
                for (Span span : filtered.getSpans()) {
                    PiiType piiType = FILTER_TYPE_MAP.get(span.getFilterType());
                    if (piiType == null || !command.isEnabled(piiType)) {
                        continue;
                    }
                    detections.add(new MaskingDetectionResult(
                            piiType,
                            span.getCharacterStart(),
                            span.getCharacterEnd()
                    ));
                }
            } catch (Exception e) {
                throw new MaskingException(MaskingErrorCode.PHILEAS_DETECTION_FAILED, e);
            }
        }
        return detections;
    }

    private List<MaskingDetectionResult> detectWithLocalPatterns(String text, MaskingCommand command) {
        List<MaskingDetectionResult> detections = new ArrayList<>();
        addMatches(detections, text, EMAIL, PiiType.EMAIL, command);
        addMatches(detections, text, URL, PiiType.URL, command);
        addMatches(detections, text, PHONE, PiiType.PHONE, command);
        addMatches(detections, text, CARD, PiiType.CARD_NUMBER, command);
        addMatches(detections, text, KOREAN_RRN, PiiType.KOREAN_RRN, command);
        addGroupMatches(detections, text, ACCOUNT, PiiType.ACCOUNT_NUMBER, command);
        addMatches(detections, text, ADDRESS, PiiType.ADDRESS, command);
        addGroupMatches(detections, text, KOREAN_PERSON, PiiType.PERSON_NAME, command);
        addGroupMatches(detections, text, ENGLISH_PERSON, PiiType.PERSON_NAME, command);
        return detections;
    }

    private void addMatches(List<MaskingDetectionResult> detections, String text, Pattern pattern, PiiType type,
                            MaskingCommand command) {
        if (!command.isEnabled(type)) {
            return;
        }
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            detections.add(new MaskingDetectionResult(type, matcher.start(), matcher.end()));
        }
    }

    private void addGroupMatches(List<MaskingDetectionResult> detections, String text, Pattern pattern, PiiType type,
                                 MaskingCommand command) {
        if (!command.isEnabled(type)) {
            return;
        }
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            detections.add(new MaskingDetectionResult(type, matcher.start(1), matcher.end(1)));
        }
    }

    private MaskingResult replaceTokens(String text, List<MaskingDetectionResult> rawDetections, MaskingCommand command) {
        List<MaskingDetectionResult> detections = resolveOverlaps(rawDetections).stream()
                .filter(detection -> command.isEnabled(detection.piiType()))
                .sorted(Comparator.comparingInt(MaskingDetectionResult::startInclusive))
                .toList();

        Map<String, String> tokenByIdentity = new HashMap<>();
        Map<PiiType, Integer> counters = new HashMap<>();
        List<MaskingTokenResult> tokens = new ArrayList<>();
        StringBuilder maskedText = new StringBuilder(text.length());
        int cursor = 0;

        for (MaskingDetectionResult detection : detections) {
            if (detection.startInclusive() < cursor) {
                continue;
            }
            String originalValue = text.substring(detection.startInclusive(), detection.endExclusive());
            String identity = detection.piiType().name() + "\u0000" + originalValue;
            String token = tokenByIdentity.computeIfAbsent(identity, ignored -> {
                int next = counters.merge(detection.piiType(), 1, Integer::sum);
                return "[" + detection.piiType().tokenPrefix() + "_" + next + "]";
            });
            maskedText.append(text, cursor, detection.startInclusive());
            maskedText.append(token);
            cursor = detection.endExclusive();
            tokens.add(new MaskingTokenResult(
                    detection.piiType(),
                    token,
                    originalValue,
                    detection.startInclusive(),
                    detection.endExclusive()
            ));
        }
        maskedText.append(text, cursor, text.length());

        Map<String, String> restoreTokenMap = new LinkedHashMap<>();
        Map<String, String> redactedTokenMap = new LinkedHashMap<>();
        for (MaskingTokenResult token : tokens) {
            if (command.scope() == MaskingScope.CURRENT_CONTEXT) {
                restoreTokenMap.putIfAbsent(token.token(), token.originalValue());
            } else {
                redactedTokenMap.putIfAbsent(token.token(), token.originalValue());
            }
        }
        return new MaskingResult(maskedText.toString(), tokens, restoreTokenMap, redactedTokenMap);
    }

    private List<MaskingDetectionResult> resolveOverlaps(List<MaskingDetectionResult> rawDetections) {
        List<MaskingDetectionResult> selected = new ArrayList<>();
        List<MaskingDetectionResult> candidates = rawDetections.stream()
                .sorted(Comparator.comparingInt(MaskingDetectionResult::length).reversed()
                        .thenComparingInt(MaskingDetectionResult::startInclusive))
                .toList();
        for (MaskingDetectionResult candidate : candidates) {
            boolean overlaps = selected.stream().anyMatch(existing -> existing.overlaps(candidate));
            if (!overlaps) {
                selected.add(candidate);
            }
        }
        return selected;
    }

    private FilterConfiguration createFilterConfiguration() {
        List<AbstractFilterStrategy> strategies = List.of(
                new EmailAddressFilterStrategy(),
                new PhoneNumberFilterStrategy(),
                new UrlFilterStrategy(),
                new CreditCardFilterStrategy()
        );
        return new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(strategies)
                .withContextService(new DefaultContextService())
                .withRandom(new Random(1))
                .withIgnored(Set.of())
                .withIgnoredFiles(Set.of())
                .withIgnoredPatterns(List.of())
                .withWindowSize(5)
                .withPriority(0)
                .build();
    }

    private Policy createPolicy() {
        Policy createdPolicy = new Policy();
        Identifiers identifiers = new Identifiers();
        identifiers.setEmailAddress(new EmailAddress());
        identifiers.setPhoneNumber(new PhoneNumber());
        identifiers.setUrl(new Url());
        identifiers.setCreditCard(new CreditCard());
        createdPolicy.setIdentifiers(identifiers);
        return createdPolicy;
    }

    private EmailAddressFilter createEmailAddressFilter(FilterConfiguration configuration) {
        try {
            return new EmailAddressFilter(configuration, false, false);
        } catch (IOException e) {
            throw new MaskingException(MaskingErrorCode.PHILEAS_INITIALIZATION_FAILED, e);
        }
    }
}
