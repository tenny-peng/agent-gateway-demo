package org.tenny.skill.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tenny.skill.entity.UserSkill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for matching user queries with relevant skills.
 * Uses keyword-based matching for now (no embedding API required).
 */
@Service
public class SkillMatchService {

    private static final Logger log = LoggerFactory.getLogger(SkillMatchService.class);

    private final SkillCacheService skillCacheService;

    public SkillMatchService(SkillCacheService skillCacheService) {
        this.skillCacheService = skillCacheService;
    }

    /**
     * Match skills for a user query.
     * Returns skills that have keyword overlap with the query.
     * 
     * @param userId the user ID
     * @param query the user's query text
     * @param maxResults maximum number of skills to return
     * @return list of matched skills, ordered by relevance
     */
    public List<UserSkill> matchSkills(Long userId, String query, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<UserSkill> allSkills = skillCacheService.getUserSkills(userId);
        if (allSkills.isEmpty()) {
            return Collections.emptyList();
        }

        // Extract keywords from query
        Set<String> queryTerms = extractTerms(query);

        // Score each skill
        List<SkillScore> scored = new ArrayList<>();
        for (UserSkill skill : allSkills) {
            long score = scoreSkill(skill, queryTerms);
            if (score > 0) {
                scored.add(new SkillScore(skill, score));
            }
        }

        // Sort by score descending
        scored.sort((a, b) -> Long.compare(b.score, a.score));

        // Return top results
        List<UserSkill> result = new ArrayList<>();
        int limit = Math.min(maxResults, scored.size());
        for (int i = 0; i < limit; i++) {
            result.add(scored.get(i).skill);
        }

        log.debug("Matched {} skills for query: {}", result.size(), query);
        return result;
    }

    /**
     * Extract terms from query for matching.
     * Extracts words (2+ chars) and 2-grams for better matching.
     */
    private Set<String> extractTerms(String query) {
        Set<String> terms = new HashSet<>();
        String normalized = query.toLowerCase().trim();

        // Extract words (split by non-alphanumeric)
        String[] words = normalized.split("[^a-zA-Z0-9\\u4e00-\\u9fa5]+");
        for (String word : words) {
            if (word.length() >= 2) {
                terms.add(word);
            }
        }

        // Extract 2-grams for Chinese and better matching
        for (int i = 0; i + 2 <= normalized.length(); i++) {
            String gram = normalized.substring(i, i + 2);
            if (!gram.trim().isEmpty()) {
                terms.add(gram);
            }
        }

        return terms;
    }

    /**
     * Score a skill based on term overlap with query.
     */
    private long scoreSkill(UserSkill skill, Set<String> queryTerms) {
        String skillText = (skill.getTitle() + " " + skill.getDescription() + " " + skill.getContent()).toLowerCase();
        long score = 0;

        for (String term : queryTerms) {
            if (term.isEmpty()) {
                continue;
            }
            // Count occurrences
            int count = countOccurrences(skillText, term);
            if (count > 0) {
                // Weight by term length (longer terms = more specific = higher weight)
                score += count * term.length();
            }
        }

        return score;
    }

    /**
     * Count occurrences of needle in haystack.
     */
    private int countOccurrences(String haystack, String needle) {
        int count = 0;
        int from = 0;
        while (from <= haystack.length() - needle.length()) {
            int idx = haystack.indexOf(needle, from);
            if (idx < 0) {
                break;
            }
            count++;
            from = idx + Math.max(1, needle.length());
        }
        return count;
    }

    /**
     * Helper class for scoring skills.
     */
    private static class SkillScore {
        final UserSkill skill;
        final long score;

        SkillScore(UserSkill skill, long score) {
            this.skill = skill;
            this.score = score;
        }
    }
}
