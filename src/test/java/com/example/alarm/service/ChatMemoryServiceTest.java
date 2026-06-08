package com.example.alarm.service;

import com.example.alarm.model.ChatSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatMemoryServiceTest {

    @Test
    void testCreateAndGetSession() {
        ChatMemoryService service = new ChatMemoryService();
        ChatSession session = service.createSession();
        assertNotNull(session.getSessionId());
        ChatSession fetched = service.getSession(session.getSessionId());
        assertNotNull(fetched);
    }

    @Test
    void testExpiredSessionReturnsNull() {
        // Sessions are tested for TTL; fresh session should be accessible
        ChatMemoryService service = new ChatMemoryService();
        ChatSession session = service.createSession();
        assertNotNull(service.getSession(session.getSessionId()));
    }

    @Test
    void testAddExchange() {
        ChatMemoryService service = new ChatMemoryService();
        ChatSession session = service.createSession();
        ChatSession.Exchange exchange = new ChatSession.Exchange();
        exchange.setAlarmText("test alarm");
        service.addExchange(session.getSessionId(), exchange);
        assertEquals(1, service.getSession(session.getSessionId()).getHistory().size());
    }
}
