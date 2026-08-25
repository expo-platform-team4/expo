package com.expo.auth.service;

import com.expo.auth.dto.BusinessNumberAvailabilityResponse;
import com.expo.auth.exception.InvalidBusinessNumberException;
import com.expo.auth.repository.ClientProfileRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사업자등록번호 사용 가능 여부 검증 서비스 (A-API-005).
 *
 * <p>MVP 단계에서는 국세청·공공데이터 사업자등록번호 API 를 호출하지 않고, 서버에 하드코딩한 테스트 번호 목록과 {@link
 * ClientProfileRepository#existsByBusinessNumber(String)} 결과만으로 사용 가능 여부를 판정한다.
 *
 * <p>정규화 로직은 A-API-002 클라이언트 회원가입에서도 그대로 재사용할 수 있게 {@link #normalize(String)} 을 공개한다.
 */
@Service
public class BusinessNumberValidationService {

    /**
     * MVP 테스트 환경에서만 정상으로 인정하는 사업자등록번호. 실제 검증 API 는 사용하지 않는다.
     *
     * <p>여기 있다고 <b>가입할 수 있다는 뜻은 아니다.</b> 이미 가입한 클라이언트가 쓴 번호는
     * 중복으로 걸린다. 로컬 시드가 {@code 1234567890}·{@code 1111111111} 을 이미 쓰고 있어서,
     * QA 중에 쓸 수 있는 번호가 하나뿐인 상황이 되어 다섯을 더 열었다.
     *
     * <p><b>이 목록은 세 곳에 복제돼 있다.</b> 여기(판정), {@code AuthController} 의 Swagger 설명,
     * 프론트 가입 화면의 안내 문구. 늘리거나 줄일 때 셋을 함께 고쳐야 한다 — 한쪽만 고치면
     * 화면에는 보이는데 서버가 거절하거나, 그 반대가 된다.
     */
    private static final Set<String> TEST_BUSINESS_NUMBERS =
            Set.of(
                    "1234567890",
                    "1111111111",
                    "2222222222",
                    "3333333333",
                    "4444444444",
                    "5555555555",
                    "6666666666",
                    "7777777777");

    /**
     * 허용 입력 형식: 숫자 10자리 또는 3-2-5 하이픈 포함(하이픈은 두 위치에서 선택적).
     *
     * <p>{@code replaceAll("[^0-9]", "")} 로 강제 제거하면 문자·특수문자가 섞인 잘못된 입력이 정상 번호처럼 변환될 수 있으므로, 반드시 이
     * 정규식으로 사전 검증한 뒤에만 하이픈을 제거한다.
     */
    private static final Pattern INPUT_FORMAT = Pattern.compile("^\\d{3}-?\\d{2}-?\\d{5}$");

    /** 정규화 결과 이중 검증용. */
    private static final Pattern NUMERIC_10 = Pattern.compile("\\d{10}");

    private final ClientProfileRepository clientProfileRepository;

    public BusinessNumberValidationService(ClientProfileRepository clientProfileRepository) {
        this.clientProfileRepository = clientProfileRepository;
    }

    /**
     * 입력값을 형식 검증 후 하이픈을 제거하여 숫자 10자리로 정규화한다.
     *
     * @throws InvalidBusinessNumberException null / 빈 문자열 / 형식 불일치 / 정규화 결과가 10자리 숫자가 아닌 경우
     */
    public String normalize(String businessNumber) {
        if (businessNumber == null || businessNumber.isBlank()) {
            throw new InvalidBusinessNumberException("사업자등록번호를 입력해 주세요.");
        }
        String trimmed = businessNumber.trim();
        if (!INPUT_FORMAT.matcher(trimmed).matches()) {
            throw new InvalidBusinessNumberException("사업자등록번호 형식이 올바르지 않습니다.");
        }
        String normalized = trimmed.replace("-", "");
        // 정규식이 이미 3-2-5 구조를 강제하지만, 정규화 후 자릿수를 한 번 더 확인해 방어한다.
        if (!NUMERIC_10.matcher(normalized).matches()) {
            throw new InvalidBusinessNumberException("사업자등록번호 형식이 올바르지 않습니다.");
        }
        return normalized;
    }

    /**
     * 사업자등록번호 사용 가능 여부를 확인한다.
     *
     * <p>형식·필수값 오류는 {@link InvalidBusinessNumberException} 으로 던져 400 Bad Request 로 응답하고, 그 외 판정(테스트
     * 번호 아님 / 이미 가입됨 / 사용 가능)은 응답 DTO 로 200 OK 반환한다.
     */
    @Transactional(readOnly = true)
    public BusinessNumberAvailabilityResponse checkAvailability(String businessNumber) {
        String normalized = normalize(businessNumber);

        // 입력한 사업자등록번호가 MVP 테스트용 허용 목록에 없으면 “테스트 번호가 아님” 응답을 돌려주는 검사
        if (!TEST_BUSINESS_NUMBERS.contains(normalized)) {
            return BusinessNumberAvailabilityResponse.notTestNumber(normalized);
        }
        if (clientProfileRepository.existsByBusinessNumber(normalized)) {
            return BusinessNumberAvailabilityResponse.duplicate(normalized);
        }
        return BusinessNumberAvailabilityResponse.available(normalized);
    }

    /**
     * 클라이언트 회원가입(A-API-002)용 사업자등록번호 최종 검증.
     *
     * <p>A-API-005 HTTP API 를 호출하지 않고, 동일 Service 에서 형식·테스트 번호·DB 중복을 다시 확인한다. 프론트에서 사전 조회했더라도 백엔드는 이
     * 메서드로 반드시 재검증한다.
     *
     * @return 하이픈이 제거된 숫자 10자리 사업자등록번호
     * @throws InvalidBusinessNumberException 형식 오류 또는 테스트 번호 목록에 없는 경우
     * @throws BusinessException 이미 가입된 사업자등록번호 ({@link ErrorCode#DUPLICATE_BUSINESS_NUMBER})
     */
    @Transactional(readOnly = true)
    public String validateForClientSignup(String businessNumber) {
        String normalized = normalize(businessNumber);
        if (!TEST_BUSINESS_NUMBERS.contains(normalized)) {
            throw new InvalidBusinessNumberException("테스트용으로 등록되지 않은 사업자등록번호입니다.");
        }
        if (clientProfileRepository.existsByBusinessNumber(normalized)) {
            throw new BusinessException(ErrorCode.DUPLICATE_BUSINESS_NUMBER);
        }
        return normalized;
    }
}
