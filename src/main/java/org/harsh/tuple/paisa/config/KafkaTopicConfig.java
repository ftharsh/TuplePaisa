package org.harsh.tuple.paisa.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic emailNotificationTopic(){
        return new NewTopic("emailNotificationTopic", 1, (short) 1);
    }
}
