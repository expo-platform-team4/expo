package com.expo.booth.service;

import com.expo.booth.entity.BoothAllocation;
import com.expo.booth.entity.BoothOrder;
import com.expo.booth.entity.BoothProduct;
import com.expo.booth.entity.BoothReservation;
import com.expo.booth.entity.BoothReservationStatus;
import com.expo.booth.repository.BoothAllocationRepository;
import com.expo.booth.repository.BoothProductRepository;
import com.expo.booth.repository.BoothReservationRepository;
import com.expo.participation.entity.ParticipationApplication;
import com.expo.participation.repository.ParticipationApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 결제 승인 성공 시 주문·부스 상품·예약·신청서·배정 상태를 한 번에 전파한다. */
@Service
public class BoothOrderCompletionService {

    private final BoothProductRepository boothProductRepository;
    private final BoothReservationRepository boothReservationRepository;
    private final ParticipationApplicationRepository participationApplicationRepository;
    private final BoothAllocationRepository boothAllocationRepository;

    public BoothOrderCompletionService(
            BoothProductRepository boothProductRepository,
            BoothReservationRepository boothReservationRepository,
            ParticipationApplicationRepository participationApplicationRepository,
            BoothAllocationRepository boothAllocationRepository) {
        this.boothProductRepository = boothProductRepository;
        this.boothReservationRepository = boothReservationRepository;
        this.participationApplicationRepository = participationApplicationRepository;
        this.boothAllocationRepository = boothAllocationRepository;
    }

    @Transactional
    public void complete(BoothOrder order) {
        order.markPaid();
        boothProductRepository
                .findById(order.getBoothProductId())
                .ifPresent(BoothProduct::markSold);
        boothReservationRepository
                .findFirstByBoothOrderIdAndStatus(order.getId(), BoothReservationStatus.ACTIVE)
                .ifPresent(BoothReservation::confirm);
        participationApplicationRepository
                .findByIdAndClientUserId(order.getApplicationId(), order.getClientUserId())
                .ifPresent(ParticipationApplication::submit);
        boothAllocationRepository.save(
                BoothAllocation.create(
                        order.getApplicationId(),
                        order.getId(),
                        order.getBoothProductId(),
                        order.getClientUserId()));
    }
}
