package com.email.scheduler.email_scheduler_server.newsletter.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterMessage {
    private String fileName;
    private String content;
}
