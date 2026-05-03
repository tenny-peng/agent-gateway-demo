package org.tenny.skill.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tenny.skill.entity.UserSkill;

import java.util.List;
import java.util.Map;

/**
 * Service for injecting matched skills into system prompts.
 * Similar to RAG but specifically for user-defined skills/rules.
 */
@Service
public class SkillInjectService {

    private static final Logger log = LoggerFactory.getLogger(SkillInjectService.class);

    /**
     * Marker for the beginning of skill-injected content.
     * Used to strip skills from messages before persisting.
     */
    public static final String SKILL_BEGIN_MARKER = "\n\n<<<SKILLS>>>\n";

    private final SkillMatchService skillMatchService;

    public SkillInjectService(SkillMatchService skillMatchService) {
        this.skillMatchService = skillMatchService;
    }

    /**
     * Augment system message with matched skills for generic chat.
     */
    public void augmentChatSystem(List<Map<String, String>> messages, String query, Long userId) {
        if (messages.isEmpty() || query == null || query.trim().isEmpty() || userId == null) {
            return;
        }

        Map<String, String> first = messages.get(0);
        if (!"system".equals(first.get("role"))) {
            return;
        }

        String base = first.get("content");
        if (base == null) {
            base = "";
        }

        String skillBlock = buildSkillBlock(userId, query);
        if (skillBlock.isEmpty()) {
            return;
        }

        first.put("content", base + SKILL_BEGIN_MARKER + skillBlock);
        log.debug("Injected skills into system prompt for user {}", userId);
    }

    /**
     * Augment system message with matched skills for agent chat (Object-based messages).
     */
    public void augmentAgentSystem(List<Map<String, Object>> messages, String query, Long userId) {
        if (messages.isEmpty() || query == null || query.trim().isEmpty() || userId == null) {
            return;
        }

        Map<String, Object> first = messages.get(0);
        if (!"system".equals(String.valueOf(first.get("role")))) {
            return;
        }

        Object contentObj = first.get("content");
        if (!(contentObj instanceof String)) {
            return;
        }

        String base = (String) contentObj;
        String skillBlock = buildSkillBlock(userId, query);
        if (skillBlock.isEmpty()) {
            return;
        }

        first.put("content", base + SKILL_BEGIN_MARKER + skillBlock);
        log.debug("Injected skills into agent system prompt for user {}", userId);
    }

    /**
     * Strip skill-injected content from a string.
     */
    public String stripSkillsFromContent(String content) {
        if (content == null) {
            return null;
        }
        int i = content.indexOf(SKILL_BEGIN_MARKER);
        if (i < 0) {
            return content;
        }
        return content.substring(0, i);
    }

    /**
     * Strip skills from chat messages before persisting.
     */
    public void stripSkillsFromChatMessages(List<Map<String, String>> messages) {
        if (messages.isEmpty()) {
            return;
        }
        Map<String, String> first = messages.get(0);
        if (!"system".equals(first.get("role"))) {
            return;
        }
        first.put("content", stripSkillsFromContent(first.get("content")));
    }

    /**
     * Strip skills from agent messages before persisting.
     */
    public void stripSkillsFromAgentMessages(List<Map<String, Object>> messages) {
        if (messages.isEmpty()) {
            return;
        }
        Map<String, Object> first = messages.get(0);
        if (!"system".equals(String.valueOf(first.get("role")))) {
            return;
        }
        Object c = first.get("content");
        if (c instanceof String) {
            first.put("content", stripSkillsFromContent((String) c));
        }
    }

    /**
     * Build the skill block by matching query with user's skills.
     */
    private String buildSkillBlock(Long userId, String query) {
        // Match top 3 skills max to avoid token overflow
        List<UserSkill> matchedSkills = skillMatchService.matchSkills(userId, query, 3);
        if (matchedSkills.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("以下是指令/规则，请严格遵守：\n");
        for (int i = 0; i < matchedSkills.size(); i++) {
            UserSkill skill = matchedSkills.get(i);
            sb.append("\n### Skill: ").append(skill.getTitle()).append("\n");
            if (skill.getDescription() != null && !skill.getDescription().isEmpty()) {
                sb.append("描述：").append(skill.getDescription()).append("\n");
            }
            sb.append("内容：\n").append(skill.getContent()).append("\n");
        }
        return sb.toString();
    }
}
