package com.actormodelsasps.demo.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.actormodelsasps.demo.model.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatActor extends AbstractBehavior<ChatActor.Command> {

    public interface Command {}

    public static class HandleMessage implements Command {
        public final Message message;
        public HandleMessage(Message message) {
            this.message = message;
        }
    }

    private final SimpMessagingTemplate messagingTemplate;
    private final List<Message> messageHistory = new ArrayList<>();

    private ChatActor(ActorContext<Command> context, SimpMessagingTemplate messagingTemplate) {
        super(context);
        this.messagingTemplate = messagingTemplate;
    }

    public static Behavior<Command> create(SimpMessagingTemplate messagingTemplate) {
        return Behaviors.setup(context -> new ChatActor(context, messagingTemplate));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(HandleMessage.class, this::onHandleMessage)
                .build();
    }

    private Behavior<Command> onHandleMessage(HandleMessage command) {
        Message message = command.message;
        String threadName = Thread.currentThread().getName();
        System.out.println("\n📨 ══════════ MESSAGE RECEIVED (ACTOR) ══════════");
        System.out.println("   Actor: " + getContext().getSelf().path().name());
        System.out.println("   Thread: " + threadName);
        System.out.println("   From: " + message.getSender());
        System.out.println("   Content: " + message.getContent());

        message.setTimestamp(LocalDateTime.now());
        messageHistory.add(message);

        System.out.println("   Stored: ✅");
        System.out.println("   Broadcasting to all clients via /topic/messages...");
        System.out.println("════════════════════════════════════════════════\n");

        messagingTemplate.convertAndSend("/topic/messages", message);
        return this;
    }
}
