package com.example.demo.service;

import com.example.demo.common.BizAssert;
import com.example.demo.common.ErrorCodes;
import com.example.demo.dto.AiDtos;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiChatService {

    private static final Set<String> ALLOWED_ROLES = Set.of("system", "user", "assistant");

    private static final int RECENT_POSTS_FOR_AI = 15;
    private static final int RECENT_POST_EXCERPT_CHARS = 220;
    private static final int CONTEXT_POST_BODY_MAX = 8000;
    private static final int CONTEXT_COMMENT_MAX = 50;
    private static final int CONTEXT_COMMENT_TEXT_MAX = 400;

    private static final String DEFAULT_SYSTEM_PROMPT = """
        你是「校园论坛」站内的智能助手，回答简洁、友好、实用。
        下列若出现「摘录」段落，仅可基于其中可见内容讨论本站帖子；未出现在摘录中的帖子/评论不要捏造。
        若问题与论坛无关可简要回答并提示用户可去搜索或发帖交流。
        """;

    private final ScnetLlmService scnetLlmService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public AiChatService(ScnetLlmService scnetLlmService,
                         PostRepository postRepository,
                         CommentRepository commentRepository) {
        this.scnetLlmService = scnetLlmService;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    public AiDtos.AiChatReply chat(AiDtos.AiChatRequest request) {
        BizAssert.isTrue(scnetLlmService.isConfigured(), ErrorCodes.SERVER_ERROR, "AI 未启用或未配置密钥，请联系管理员");

        List<AiDtos.AiChatMessage> raw = request.messages();
        boolean hasUser = false;
        for (AiDtos.AiChatMessage m : raw) {
            String role = m.role().trim().toLowerCase();
            BizAssert.isTrue(ALLOWED_ROLES.contains(role), ErrorCodes.BAD_REQUEST, "消息 role 仅支持 system、user、assistant");
            if ("user".equals(role)) {
                hasUser = true;
            }
        }
        BizAssert.isTrue(hasUser, ErrorCodes.BAD_REQUEST, "至少需要一条用户消息");

        String forumContext = buildForumContext(request.contextPostId(), request.includeRecentPostSummaries());
        String forumSuffix =
            StringUtils.hasText(forumContext) ? "\n\n" + forumContext.trim() : "";

        List<AiDtos.AiChatMessage> messages = new ArrayList<>();
        boolean hasLeadingSystem = !raw.isEmpty() && "system".equalsIgnoreCase(raw.getFirst().role());
        if (!hasLeadingSystem) {
            messages.add(new AiDtos.AiChatMessage("system", DEFAULT_SYSTEM_PROMPT.trim() + forumSuffix));
            messages.addAll(raw);
        } else {
            AiDtos.AiChatMessage first = raw.getFirst();
            messages.add(new AiDtos.AiChatMessage("system", first.content().trim() + forumSuffix));
            messages.addAll(raw.subList(1, raw.size()));
        }

        return new AiDtos.AiChatReply(scnetLlmService.chatCompletionMessages(messages));
    }

    /**
     * 从数据库拉取公开帖子摘要，及可选的当前帖与评论，供模型参考（不进入多轮 user 消息，避免被用户篡改）。
     */
    private String buildForumContext(Long contextPostId, Boolean includeRecentPostSummaries) {
        boolean wantRecent = includeRecentPostSummaries == null || Boolean.TRUE.equals(includeRecentPostSummaries);
        StringBuilder sb = new StringBuilder();

        if (wantRecent) {
            var page = postRepository.pagePosts(null, null, null, 1, RECENT_POSTS_FOR_AI);
            if (!page.list().isEmpty()) {
                sb.append("【论坛最近帖子摘录（仅含正常状态帖子，按发布时间倒序，最多 ")
                    .append(RECENT_POSTS_FOR_AI)
                    .append(" 条）】\n");
                for (var row : page.list()) {
                    sb.append("- [帖子ID:").append(row.id()).append("][")
                        .append(safeOneLine(row.boardName())).append("] ")
                        .append(safeOneLine(row.title()))
                        .append(" | 摘要：")
                        .append(excerpt(row.contentText(), RECENT_POST_EXCERPT_CHARS))
                        .append('\n');
                }
            }
        }

        if (contextPostId != null) {
            postRepository.findViewById(contextPostId, null).ifPresent(view -> {
                if (!"normal".equals(view.status())) {
                    return;
                }
                sb.append("\n【用户当前浏览的帖子】\n");
                sb.append("帖子ID: ").append(view.id()).append('\n');
                sb.append("板块: ").append(safeOneLine(view.boardName())).append('\n');
                sb.append("作者: ").append(safeOneLine(authorDisplay(view.authorNickname(), view.authorUsername())))
                    .append('\n');
                sb.append("标题: ").append(safeOneLine(view.title())).append('\n');
                sb.append("正文:\n")
                    .append(excerpt(view.contentText(), CONTEXT_POST_BODY_MAX))
                    .append('\n');

                List<CommentRepository.CommentViewRow> comments =
                    commentRepository.listByPostId(contextPostId, null);
                int n = Math.min(comments.size(), CONTEXT_COMMENT_MAX);
                if (n > 0) {
                    sb.append("\n【该帖评论摘录（按时间正序，最多 ").append(CONTEXT_COMMENT_MAX).append(" 条）】\n");
                    for (int i = 0; i < n; i++) {
                        var c = comments.get(i);
                        sb.append("#").append(c.id()).append(" ")
                            .append(safeOneLine(authorDisplay(c.authorNickname(), c.authorUsername())))
                            .append(": ")
                            .append(excerpt(c.contentText(), CONTEXT_COMMENT_TEXT_MAX))
                            .append('\n');
                    }
                }
            });
        }

        String out = sb.toString().trim();
        return out.isEmpty() ? null : out;
    }

    private static String authorDisplay(String nickname, String username) {
        if (StringUtils.hasText(nickname)) {
            return nickname.trim();
        }
        return username == null ? "用户" : username.trim();
    }

    private static String safeOneLine(String s) {
        if (!StringUtils.hasText(s)) {
            return "";
        }
        return s.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private static String excerpt(String s, int maxChars) {
        if (!StringUtils.hasText(s)) {
            return "";
        }
        String t = s.replace('\r', '\n').replaceAll("\\s+", " ").trim();
        if (t.length() <= maxChars) {
            return t;
        }
        return t.substring(0, maxChars) + "…";
    }
}
