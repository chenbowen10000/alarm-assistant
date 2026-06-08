package com.example.alarm.service;

import com.example.alarm.model.ChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatMemoryService {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryService.class);
    private static final int TTL_MINUTES = 30;

    private final ConcurrentHashMap<String, ChatSession> sessions = new ConcurrentHashMap<>();

    public ChatSession createSession() {
        ChatSession session = new ChatSession();
        sessions.put(session.getSessionId(), session);
        log.info("[CHAT] Session created: {}", session.getSessionId());
        return session;
    }

    public ChatSession getSession(String sessionId) {
        ChatSession session = sessions.get(sessionId);
        if (session != null) {
            if (session.isExpired(TTL_MINUTES)) {
                sessions.remove(sessionId);
                return null;
            }
            session.touch();
        }
        return session;
    }

    public void addExchange(String sessionId, ChatSession.Exchange exchange) {
        ChatSession session = getSession(sessionId);
        if (session == null) session = createSession();
        session.getHistory().add(exchange);
        session.touch();
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupExpired() {
        sessions.entrySet().removeIf(e -> e.getValue().isExpired(TTL_MINUTES));
    }
}
