package com.expo.ticket.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.expo.entity.Expo;
import com.expo.expo.repository.ExpoRepository;
import com.expo.ticket.converter.TicketProductConverter;
import com.expo.ticket.dto.TicketProductSearchResponse;
import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.repository.TicketProductRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TicketSearchService {

    private final TicketProductConverter ticketProductConverter;
    private ExpoRepository expoRepository;
    private TicketProductRepository ticketProductRepository;

    public List<TicketProductSearchResponse> ticketProductSearch(Long memberId, Long expoId) {
        validateExpoOwner(memberId, expoId);
        List<TicketProduct> products = ticketProductRepository.findByExpoId(expoId);

        List<TicketProductSearchResponse> responses = new ArrayList<>();

        for (TicketProduct product : products) {
            TicketProductSearchResponse response =
                    ticketProductConverter.toSearchTicketProduct(product);

            responses.add(response);
        }

        return responses;
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
