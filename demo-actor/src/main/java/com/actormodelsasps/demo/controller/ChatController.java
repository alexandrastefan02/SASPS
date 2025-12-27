package com.actormodelsasps.demo.controller;

import akka.actor.typed.ActorSystem;
import com.actormodelsasps.demo.actor.ChatActor;
import com.actormodelsasps.demo.model.Message;
import com.actormodelsasps.demo.service.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * ChatController - Handles WebSocket chat messages
 *
 * ACTOR-BASED ARCHITECTURE:
 *
 * When a client sends a message:
 * 1. Message arrives at WebSocket endpoint
 * 2. Tomcat's thread pool assigns a thread (e.g., Thread-12)
 * 3. That thread calls the @MessageMapping method
 * 4. The controller sends the message to the ChatActor
 * 5. The ChatActor processes messages sequentially, avoiding race conditions.
 *
 * Message Flow:
 * Client → /app/chat.send → handleChatMessage() → ChatActor → Broadcasts to all subscribers
 */
@Controller
public class ChatController {

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private ActorSystem<ChatActor.Command> actorSystem;

    /**
     * Handle incoming chat messages
     *
     * @MessageMapping("/chat.send")  - Listens for messages sent to /app/chat.send
     *
     * ACTOR BEHAVIOR:
     * - This method is still called by a Tomcat thread.
     * - Instead of processing the message here, we send it to the ChatActor.
     * - The actor processes one message at a time, ensuring thread safety without locks.
     */
    @MessageMapping("/chat.send")
    public void handleChatMessage(Message message,
                                     SimpMessageHeaderAccessor headerAccessor) {

        String threadName = Thread.currentThread().getName();
        System.out.println("\n📨 ══════════ MESSAGE RECEIVED (CONTROLLER) ══════════");
        System.out.println("   Thread: " + threadName);
        System.out.println("   Forwarding to ChatActor...");
        System.out.println("════════════════════════════════════════════════\n");

        // Send the message to the actor system
        actorSystem.tell(new ChatActor.HandleMessage(message));
    }

    /**
     * Handle user registration
     *
     * Called when a new user joins the chat
     */
    @MessageMapping("/chat.register")
    public void registerUser(Message message,
                               SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        String username = message.getSender();

        System.out.println("\n🆕 ══════════ USER JOINING ══════════");
        System.out.println("   Thread: " + Thread.currentThread().getName());
        System.out.println("   Username: " + username);
        System.out.println("   Session: " + sessionId);

        // Add user to session manager
        sessionManager.addUser(sessionId, username);

        // Create join notification message
        Message joinMessage = new Message();
        joinMessage.setType(Message.MessageType.JOIN);
        joinMessage.setSender(username);
        joinMessage.setContent(username + " joined the chat!");
        joinMessage.setTimestamp(LocalDateTime.now());

        // Print statistics
        sessionManager.printStats();

        System.out.println("═════════════════════════════════════════\n");

        // Broadcast join notification via actor
        actorSystem.tell(new ChatActor.HandleMessage(joinMessage));
    }

    /**
     * Get message history
     *
     * Clients can request recent messages
     */
    @MessageMapping("/chat.history")
    public java.util.List<Message> getHistory() {
        System.out.println("📜 History requested by " + Thread.currentThread().getName());
        return sessionManager.getRecentMessages(50);
    }
}
