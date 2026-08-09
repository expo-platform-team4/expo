package com.expo.ticket.service;

import com.expo.global.CustomException;
import com.expo.global.ErrorCode;
import com.expo.ticket.converter.TicketProductConverter;
import com.expo.ticket.dto.TicketProductCreateRequest;
import com.expo.ticket.dto.TicketProductCreateResponse;
import com.expo.ticket.entity.TicketInventory;
import com.expo.ticket.entity.TicketProduct;
import com.expo.ticket.repository.TicketProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TicketProductCreateService {

    private final TicketProductConverter ticketProductConverter;
    private final TicketProductRepository ticketProductRepository;

    @Transactional
    public TicketProductCreateResponse ticketCreate(
            Long expoId, TicketProductCreateRequest request) {
        validateSalesPeriod(request);

        TicketProduct product = ticketProductConverter.toEntity(expoId, request);
        TicketInventory inventory = TicketInventory.create(product, request.totalQuantity());
        product.attachInventory(inventory);

        TicketProduct savedProduct = ticketProductRepository.save(product);
        return ticketProductConverter.toCreateResponse(savedProduct);
    }

    private void validateSalesPeriod(TicketProductCreateRequest request) {
        if (!request.salesStartAt().isBefore(request.salesEndAt())) {
            throw new CustomException(ErrorCode.INVALID_SALES_PERIOD);
        }
    }

    /*
       주최자 검증 메소드
       현재 박람회 id를 조회할 수 있는 Repository 혹은 메서드가 존재하지않아,
       코드만 흐름만 적어 논다
    */
    private void validateExpoOwner(Long memberId, Long expoId) {
        /*
           URL에서 expoId를 받아옴
           findByExpoId(expoId)를 통해 db에서 찾음
           없으면 예외

           엔티티에서 가져온 클라이트 아이디값과 받은 dto값이 일치하는지 검증
           없으면 예외
        */
    }
}
