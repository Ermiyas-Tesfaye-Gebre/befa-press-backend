package com.befapress.service;

import com.befapress.dto.ModerationResult;
import com.befapress.dto.request.CreateRuleRequest;
import com.befapress.entity.ModerationRule;
import com.befapress.repository.ModerationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Content Moderation Service
 * Detects harmful content in Amharic and English including:
 * - Insults and slurs
 * - Hate speech
 * - Threats and harassment
 * - Mockery and belittling
 * - Ad hominem attacks
 * - Off-topic/derailing content
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContentModerationService {

    private final ModerationRuleRepository moderationRuleRepository;

    // Category constants
    public static final String CATEGORY_INSULT = "INSULT";
    public static final String CATEGORY_HATE_SPEECH = "HATE_SPEECH";
    public static final String CATEGORY_THREAT = "THREAT";
    public static final String CATEGORY_MOCKERY = "MOCKERY";
    public static final String CATEGORY_AD_HOMINEM = "AD_HOMINEM";
    public static final String CATEGORY_OFF_TOPIC = "OFF_TOPIC";

    // User-facing messages
    private static final Map<String, String> CATEGORY_MESSAGES = Map.of(
            CATEGORY_INSULT, "Your comment contains insulting language",
            CATEGORY_HATE_SPEECH, "Your comment contains hate speech",
            CATEGORY_THREAT, "Your comment contains threatening language",
            CATEGORY_MOCKERY, "Your comment contains mocking or belittling language",
            CATEGORY_AD_HOMINEM, "Your comment contains personal attacks",
            CATEGORY_OFF_TOPIC, "Your comment appears to be off-topic");

    // Wordlists by category
    private final Map<String, Set<String>> amharicWordlists = new HashMap<>();
    private final Map<String, Set<String>> englishWordlists = new HashMap<>();
    private final Map<String, List<Pattern>> amharicPatterns = new HashMap<>();
    private final Map<String, List<Pattern>> englishPatterns = new HashMap<>();

    @PostConstruct
    public void init() {
        refreshRules();
    }

    public void refreshRules() {
        // Clear existing lists
        amharicWordlists.clear();
        englishWordlists.clear();
        // Patterns maps are not cleared because compilePatterns overwrites/puts list.
        // But better to clear to be safe if we were reloading patterns (which we aren't
        // dynamically yet)

        // Load built-in lists
        initializeAmharicWordlists();
        initializeEnglishWordlists();
        compilePatterns();

        // Load custom rules from DB
        loadCustomRules();

        log.info("ContentModerationService initialized/refreshed with {} Amharic and {} English categories",
                amharicWordlists.size(), englishWordlists.size());
    }

    private void loadCustomRules() {
        try {
            List<ModerationRule> rules = moderationRuleRepository.findAll();
            for (ModerationRule rule : rules) {
                String category = rule.getCategory().toUpperCase();
                String lang = rule.getLanguage().toUpperCase();
                String pattern = rule.getPattern().toLowerCase();

                if ("AMHARIC".equals(lang) || "AM".equals(lang)) {
                    amharicWordlists.computeIfAbsent(category, k -> new HashSet<>()).add(pattern);
                } else if ("ENGLISH".equals(lang) || "EN".equals(lang)) {
                    englishWordlists.computeIfAbsent(category, k -> new HashSet<>()).add(pattern);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load custom moderation rules from DB (table might not exist yet): {}", e.getMessage());
        }
    }

    public ModerationRule addRule(CreateRuleRequest request) {
        ModerationRule rule = new ModerationRule();
        rule.setCategory(request.getCategory().toUpperCase());
        rule.setLanguage(request.getLanguage().toUpperCase());
        rule.setPattern(request.getPattern().toLowerCase());

        rule = moderationRuleRepository.save(rule);
        refreshRules();
        return rule;
    }

    public void deleteRule(Long id) {
        moderationRuleRepository.deleteById(id);
        refreshRules();
    }

    public List<ModerationRule> getAllRules() {
        return moderationRuleRepository.findAll();
    }

    /**
     * Analyze content for harmful material
     * 
     * @param content The text to analyze
     * @return ModerationResult with findings
     */
    public ModerationResult analyzeContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return ModerationResult.passed();
        }

        String normalizedContent = content.toLowerCase().trim();
        List<String> detectedCategories = new ArrayList<>();
        List<String> matchedTerms = new ArrayList<>();

        // Check Amharic content
        for (Map.Entry<String, Set<String>> entry : amharicWordlists.entrySet()) {
            String category = entry.getKey();
            for (String term : entry.getValue()) {
                if (normalizedContent.contains(term)) {
                    if (!detectedCategories.contains(category)) {
                        detectedCategories.add(category);
                    }
                    matchedTerms.add(term);
                }
            }
        }

        // Check Amharic patterns
        for (Map.Entry<String, List<Pattern>> entry : amharicPatterns.entrySet()) {
            String category = entry.getKey();
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(normalizedContent).find()) {
                    if (!detectedCategories.contains(category)) {
                        detectedCategories.add(category);
                    }
                }
            }
        }

        // Check English content
        for (Map.Entry<String, Set<String>> entry : englishWordlists.entrySet()) {
            String category = entry.getKey();
            for (String term : entry.getValue()) {
                if (containsWord(normalizedContent, term)) {
                    if (!detectedCategories.contains(category)) {
                        detectedCategories.add(category);
                    }
                    matchedTerms.add(term);
                }
            }
        }

        // Check English patterns
        for (Map.Entry<String, List<Pattern>> entry : englishPatterns.entrySet()) {
            String category = entry.getKey();
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(normalizedContent).find()) {
                    if (!detectedCategories.contains(category)) {
                        detectedCategories.add(category);
                    }
                }
            }
        }

        if (detectedCategories.isEmpty()) {
            return ModerationResult.passed();
        }

        // Build user-facing reason
        String reason = detectedCategories.stream()
                .map(cat -> CATEGORY_MESSAGES.getOrDefault(cat, "Inappropriate content detected"))
                .collect(Collectors.joining("; "));

        // Calculate confidence based on number of matches
        double confidence = Math.min(1.0, matchedTerms.size() * 0.2);

        return ModerationResult.builder()
                .harmful(true)
                .detectedCategories(detectedCategories)
                .matchedTerms(matchedTerms)
                .reason(reason)
                .confidenceScore(confidence)
                .build();
    }

    /**
     * Check if content contains a whole word (not substring)
     */
    private boolean containsWord(String text, String word) {
        // Simple contains for efficiency if regex is overkill for simple words
        // But for English, whole word matching is better to avoid "ass" in "class"
        String pattern = "\\b" + Pattern.quote(word) + "\\b";
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text).find();
    }

    private void initializeAmharicWordlists() {
        // ===== INSULTS (ስድብ) =====
        amharicWordlists.put(CATEGORY_INSULT, new HashSet<>(Arrays.asList(
                // Direct insults
                "ደደብ", "ፋራ", "ባለጌ", "ገገማ", "አተላ", "ድንጋይ", "አላዋቂ", "መሃይም",
                "ቆሻሻ", "የሰው አተላ", "ኋላ-ቀር", "የማይረባ", "አፍህን ዝጋ",
                // Additional insults
                "ሞኝ", "ደንቆሮ", "ጅል", "ቂል", "ቂልጥምጣም", "ቂላጤ", "ጎሽ", "ደነዝ")));

        // ===== MOCKERY (ማጣጣል) =====
        amharicWordlists.put(CATEGORY_MOCKERY, new HashSet<>(Arrays.asList(
                "እንቶ ፈንቶ", "ቆሻሻ ጽሁፍ", "ጨቅላ", "የውሃ ሽታ", "ቅዠት",
                "የህፃን ወሬ", "ሰብሳቢ", "ምንም አይገባህም", "ገና ነህ", "ብዙ ይቀርሃል",
                "የቆሻሻ ቅርጫት", "ቁም ነገር የለውም", "ጊዜ ማባከን")));

        // ===== HATE SPEECH (ጥላቻ) =====
        amharicWordlists.put(CATEGORY_HATE_SPEECH, new HashSet<>(Arrays.asList(
                "ዘረኛ", "ባንዳ", "ፅንፈኛ", "አክራሪ", "ሰላቢ",
                "የባንዳ ልጅ", "የዚህ ብሄር ተወላጅ", "እናንተ ሁላችሁም አንድ ናችሁ",
                "ፅንፈኛ ወሬ", "ራሳችሁን አጽዱ")));

        // ===== THREATS (ዛቻ) =====
        amharicWordlists.put(CATEGORY_THREAT, new HashSet<>(Arrays.asList(
                "ዋጋህን ታገኛለህ", "ተጠንቀቅ", "መጨረሻህ አያምርም", "እናውቅሃለን",
                "አንለቅህም", "የመጨረሻ ማስጠንቀቂያ", "እሳት ጋር አትጫወት",
                "የት እንደምትኖር እናውቃለን", "አድራሻህ እጃችን ላይ ነው", "በቅርቡ እንገናኛለን",
                "ዋጋህን እንደምትከፍል እወቅ")));

        // ===== AD HOMINEM (ስብዕና ማጉደፍ) =====
        amharicWordlists.put(CATEGORY_AD_HOMINEM, new HashSet<>(Arrays.asList(
                "ሆዳም", "ሌባ", "መልክ ጥፉ", "አስቀያሚ", "የመንደር ወሬኛ",
                "ራስህን አስተካክል", "ልክ እንደ መልኩ", "ተገዛ", "ለጥቅም ያደረ")));

        // ===== OFF-TOPIC (ከአውድ ውጪ) =====
        amharicWordlists.put(CATEGORY_OFF_TOPIC, new HashSet<>(Arrays.asList(
                "በሬ ወለደ", "ምን አገናኘው", "ሌላ አጀንዳ", "አጀንዳ አስቀያሪ", "ሆድ አደር",
                "ሆድ አደሮች", "ፕሮፓጋንዳ")));
    }

    private void initializeEnglishWordlists() {
        // ===== INSULTS =====
        englishWordlists.put(CATEGORY_INSULT, new HashSet<>(Arrays.asList(
                "idiot", "moron", "stupid", "dumb", "imbecile", "fool", "ignorant",
                "retard", "retarded", "loser", "pathetic", "worthless", "useless",
                "trash", "garbage", "scum", "pig", "dog", "animal",
                "shut up", "shut your mouth", "stfu")));

        // ===== MOCKERY =====
        englishWordlists.put(CATEGORY_MOCKERY, new HashSet<>(Arrays.asList(
                "joke", "laughable", "ridiculous", "absurd", "nonsense", "bs",
                "bullshit", "crap", "rubbish", "waste of time", "nobody cares",
                "grow up", "childish", "immature", "amateur", "wannabe")));

        // ===== HATE SPEECH =====
        englishWordlists.put(CATEGORY_HATE_SPEECH, new HashSet<>(Arrays.asList(
                "racist", "terrorism", "terrorist", "extremist", "radical",
                "go back to your country", "you people", "your kind",
                "inferior", "subhuman", "vermin")));

        // ===== THREATS =====
        englishWordlists.put(CATEGORY_THREAT, new HashSet<>(Arrays.asList(
                "kill you", "hurt you", "attack you", "find you", "know where you live",
                "watch your back", "you'll pay", "you will regret", "i'll get you",
                "destroy you", "end you", "beat you up", "punch you")));

        // ===== AD HOMINEM =====
        englishWordlists.put(CATEGORY_AD_HOMINEM, new HashSet<>(Arrays.asList(
                "ugly", "fat", "skinny", "disgusting", "corrupt", "liar",
                "hypocrite", "fraud", "fake", "sellout", "traitor", "puppet")));

        // ===== OFF-TOPIC =====
        englishWordlists.put(CATEGORY_OFF_TOPIC, new HashSet<>(Arrays.asList(
                "off topic", "irrelevant", "what does this have to do with",
                "nothing to do with", "stop derailing")));
    }

    private void compilePatterns() {
        // Amharic threat patterns
        amharicPatterns.put(CATEGORY_THREAT, Arrays.asList(
                Pattern.compile(".*ዋጋ.*ታገኛ.*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*መጨረሻ.*አያምር.*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*የት.*እንደ.*ትኖር.*እናውቃ.*", Pattern.CASE_INSENSITIVE)));

        // English threat patterns
        englishPatterns.put(CATEGORY_THREAT, Arrays.asList(
                Pattern.compile(".*i('ll|\\s+will)\\s+(kill|hurt|destroy|attack).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*you('ll|\\s+will)\\s+(pay|regret|suffer).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*watch\\s+(out|your\\s+back).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*know\\s+where\\s+you\\s+(live|work).*", Pattern.CASE_INSENSITIVE)));

        // English insult patterns
        englishPatterns.put(CATEGORY_INSULT, Arrays.asList(
                Pattern.compile(".*you\\s+(are|r)\\s+(an?\\s+)?(idiot|moron|stupid|dumb).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*f+u+c+k+.*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*a+s+s+h+o+l+e+.*", Pattern.CASE_INSENSITIVE)));
    }
}
