package com.corc.backend.service;

import com.corc.backend.dto.request.PaymentCardRequest;
import com.corc.backend.entity.PaymentCard;
import com.corc.backend.entity.User;
import com.corc.backend.exception.ResourceNotFoundException;
import com.corc.backend.repository.PaymentCardRepository;
import com.corc.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentCardService {

    private final PaymentCardRepository cardRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCards(String email) {
        User user = findUser(email);
        return cardRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> addCard(String email, PaymentCardRequest request) {
        User user = findUser(email);
        PaymentCard card = PaymentCard.builder()
                .user(user)
                .type(request.getType())
                .last4(request.getLast4())
                .expiry(request.getExpiry())
                .isDefault(request.isDefault())
                .build();

        card = cardRepository.save(card);
        return mapToMap(card);
    }

    @Transactional
    public void removeCard(String email, Long cardId) {
        User user = findUser(email);
        PaymentCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentCard", "id", cardId));

        if (!card.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Card does not belong to user");
        }

        cardRepository.delete(card);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private Map<String, Object> mapToMap(PaymentCard card) {
        return Map.of(
                "id", card.getId(),
                "type", card.getType(),
                "last4", card.getLast4(),
                "expiry", card.getExpiry(),
                "isDefault", card.isDefault()
        );
    }
}
