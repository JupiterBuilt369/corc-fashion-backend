package com.corc.backend.service;

import com.corc.backend.entity.NewsletterSubscription;
import com.corc.backend.exception.DuplicateResourceException;
import com.corc.backend.repository.NewsletterSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewsletterService {

    private final NewsletterSubscriptionRepository subscriptionRepository;
    private final MailService mailService;

    @Transactional
    public void subscribe(String email) {
        var existing = subscriptionRepository.findByEmail(email);
        if (existing.isPresent()) {
            NewsletterSubscription sub = existing.get();
            if (sub.isActive()) {
                throw new DuplicateResourceException("Email is already subscribed");
            }
            sub.setActive(true);
            sub.setUnsubscribedAt(null);
            subscriptionRepository.save(sub);
        } else {
            subscriptionRepository.save(NewsletterSubscription.builder()
                    .email(email)
                    .active(true)
                    .build());
        }

        mailService.sendNewsletterWelcome(email);
    }

    @Transactional
    public void unsubscribe(String email) {
        NewsletterSubscription sub = subscriptionRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email not found in subscriptions"));
        sub.setActive(false);
        sub.setUnsubscribedAt(java.time.Instant.now());
        subscriptionRepository.save(sub);
    }
}
