package org.tenny.skill.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tenny.skill.entity.UserSkill;
import org.tenny.skill.mapper.UserSkillMapper;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache service for user skills.
 * Stores active skills in memory for fast access during chat.
 * Uses ConcurrentHashMap for thread-safe access.
 */
@Service
public class SkillCacheService {

    private static final Logger log = LoggerFactory.getLogger(SkillCacheService.class);

    /**
     * Cache structure: userId -> list of active skills
     */
    private final Map<Long, List<UserSkill>> userSkillCache = new ConcurrentHashMap<>();

    private final UserSkillMapper userSkillMapper;

    public SkillCacheService(UserSkillMapper userSkillMapper) {
        this.userSkillMapper = userSkillMapper;
    }

    /**
     * Load all active skills for all users on startup.
     */
    @PostConstruct
    public void initCache() {
        log.info("Initializing skill cache...");
        // Note: For large user bases, consider lazy loading or pagination
        // For now, we'll load on-demand per user
        log.info("Skill cache initialized (lazy loading mode)");
    }

    /**
     * Get cached skills for a user. Loads from DB if not cached.
     */
    public List<UserSkill> getUserSkills(Long userId) {
        return userSkillCache.computeIfAbsent(userId, this::loadSkillsFromDb);
    }

    /**
     * Refresh cache for a specific user (call after skill changes).
     */
    public void refreshUserCache(Long userId) {
        List<UserSkill> skills = loadSkillsFromDb(userId);
        userSkillCache.put(userId, skills);
        log.debug("Refreshed skill cache for user {}", userId);
    }

    /**
     * Clear cache for a specific user (e.g., on logout).
     */
    public void clearUserCache(Long userId) {
        userSkillCache.remove(userId);
        log.debug("Cleared skill cache for user {}", userId);
    }

    /**
     * Clear all cache.
     */
    public void clearAllCache() {
        userSkillCache.clear();
        log.info("Cleared all skill cache");
    }

    /**
     * Load skills from database for a user.
     */
    private List<UserSkill> loadSkillsFromDb(Long userId) {
        List<UserSkill> skills = userSkillMapper.selectActiveByUserId(userId);
        log.debug("Loaded {} active skills for user {}", skills.size(), userId);
        return skills;
    }

    /**
     * Get the number of cached users.
     */
    public int getCacheSize() {
        return userSkillCache.size();
    }
}
