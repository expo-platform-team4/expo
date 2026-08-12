package com.expo.checkin.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.expo.repository.ExpoRepository;
import org.springframework.stereotype.Component;

/**
 * 요청자가 그 박람회의 <b>주최자 본인</b>인지 확인한다.
 *
 * <h2>왜 필요한가</h2>
 *
 * {@code SecurityConfig} 는 {@code /api/client/**} 를 {@code hasAnyRole("CLIENT","ADMIN")} 으로만
 * 막는다. 그런데 <b>주최사와 참여 기업이 같은 {@code CLIENT} 역할을 쓴다.</b> 둘을 가르는 것은 역할이 아니라 데이터다 — 주최사는
 * {@code expos.host_client_id} 에, 참여 기업은 {@code participation_applications.client_user_id} 에 있다.
 *
 * <p>따라서 역할 검사만 통과시키면 <b>다른 주최사나 참여 기업이 남의 박람회 입장객을 체크인할 수 있다.</b> 체크인 API 넷은 전부 이 검증으로
 * 시작해야 한다.
 */
@Component
public class ExpoHostVerifier {

    private final ExpoRepository expoRepository;

    public ExpoHostVerifier(ExpoRepository expoRepository) {
        this.expoRepository = expoRepository;
    }

    /**
     * @param expoId 경로로 들어온 박람회
     * @param clientUserId 로그인한 클라이언트
     * @throws BusinessException 박람회가 없거나({@code EXPO_NOT_FOUND}), 주최자가 아니면({@code NOT_EXPO_HOST})
     */
    public void verifyHost(Long expoId, Long clientUserId) {
        Long hostClientId =
                expoRepository
                        .findById(expoId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.EXPO_NOT_FOUND))
                        .getHostClientId();

        if (!hostClientId.equals(clientUserId)) {
            throw new BusinessException(ErrorCode.NOT_EXPO_HOST);
        }
    }
}
