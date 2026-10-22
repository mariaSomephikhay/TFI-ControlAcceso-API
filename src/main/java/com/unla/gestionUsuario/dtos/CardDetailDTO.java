package com.unla.gestionUsuario.dtos;

import java.time.LocalDateTime;

import com.unla.gestionUsuario.entities.Card;

public record CardDetailDTO(Long id, String code, String userId, boolean active,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static CardDetailDTO from(Card card, String userId) {
        return new CardDetailDTO(card.getId(), card.getCode(), userId,
                card.isActive(), card.getCreatedAt(), card.getUpdatedAt());
    }
}
