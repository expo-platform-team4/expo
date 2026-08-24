package com.expo.ticket.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.expo.entity.Expo;
import com.expo.expo.repository.ExpoRepository;
import com.expo.ticket.converter.TicketProductConverter;
import com.expo.ticket.dto.TicketProductSearchResponse;
import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.entity.TicketProductStatus;
import com.expo.ticket.repository.TicketProductRepository;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 티켓 상품 판매 상태 전환. {@code SOLD_OUT}·{@code SALE_ENDED} 는 재고·판매기간에서 계산돼야 할
 * 값이라(이 엔티티엔 그 계산 로직이 아직 없다) 여기서 목적지로 받지 않는다 — 주최사가 직접 고를 수
 * 있는 전환만 다룬다: 초안을 게시하거나(ON_SALE), 언제든 판매를 접는(CANCELED) 것.
 */
@Service
@AllArgsConstructor
public class TicketProductStatusService {

    private static final Map<TicketProductStatus, Set<TicketProductStatus>> ALLOWED_TRANSITIONS =
            Map.of(
                    TicketProductStatus.DRAFT,
                            Set.of(TicketProductStatus.ON_SALE, TicketProductStatus.CANCELED),
                    TicketProductStatus.ON_SALE, Set.of(TicketProductStatus.CANCELED));

    private final TicketProductRepository ticketProductRepository;
    private final ExpoRepository expoRepository;
    private final TicketProductConverter ticketProductConverter;

    @Transactional
    public TicketProductSearchResponse changeStatus(
            Long memberId, Long expoId, Long ticketProductId, TicketProductStatus targetStatus) {
        validateExpoOwner(memberId, expoId);

        TicketProduct product =
                ticketProductRepository
                        .findByIdAndExpoId(ticketProductId, expoId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));

        if (!ALLOWED_TRANSITIONS
                .getOrDefault(product.getStatus(), Set.of())
                .contains(targetStatus)) {
            throw new BusinessException(ErrorCode.TICKET_PRODUCT_STATUS_TRANSITION_NOT_ALLOWED);
        }

        product.changeStatus(targetStatus);
        return ticketProductConverter.toSearchTicketProduct(product);
    }

    private void validateExpoOwner(Long memberId, Long expoId) {
        Expo expo =
                expoRepository
                        .findById(expoId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.EXPO_NOT_FOUND));

        if (!expo.getHostClientId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_EXPO_HOST);
        }
    }
}
