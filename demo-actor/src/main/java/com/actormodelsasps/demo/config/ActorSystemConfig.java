package com.actormodelsasps.demo.config;

import akka.actor.typed.ActorSystem;
import com.actormodelsasps.demo.actor.ChatActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration
public class ActorSystemConfig {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Bean
    public ActorSystem<ChatActor.Command> actorSystem() {
        return ActorSystem.create(ChatActor.create(messagingTemplate), "ChatActorSystem");
    }
}
