package com.expo.auth.service;

import com.expo.auth.converter.ClientProfileConverter;
import com.expo.auth.converter.UserConverter;
import com.expo.auth.dto.ClientSignupRequest;
import com.expo.auth.dto.ClientSignupResponse;
import com.expo.auth.dto.SignupRequest;
import com.expo.auth.dto.SignupResponse;
import com.expo.auth.entity.ClientProfile;
import com.expo.auth.entity.User;
import com.expo.auth.repository.ClientProfileRepository;
import com.expo.auth.repository.UserRepository;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final UserConverter userConverter;
    private final ClientProfileConverter clientProfileConverter;
    private final PasswordEncoder passwordEncoder;
    private final BusinessNumberValidationService businessNumberValidationService;
    private final EmailVerificationService emailVerificationService;

    public AuthService(
            UserRepository userRepository,
            ClientProfileRepository clientProfileRepository,
            UserConverter userConverter,
            ClientProfileConverter clientProfileConverter,
            PasswordEncoder passwordEncoder,
            BusinessNumberValidationService businessNumberValidationService,
            EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.clientProfileRepository = clientProfileRepository;
        this.userConverter = userConverter;
        this.clientProfileConverter = clientProfileConverter;
        this.passwordEncoder = passwordEncoder;
        this.businessNumberValidationService = businessNumberValidationService;
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * 일반 회원 로컬 회원가입. role 은 MEMBER, account_status 는 ACTIVE 로 고정한다.
     *
     * <p>이메일 인증 토큰은 사용자를 저장한 <b>뒤</b> 소비한다. 같은 트랜잭션 안이라 토큰이 유효하지
     * 않으면({@code EMAIL_VERIFICATION_TOKEN_INVALID}) 방금 만든 사용자 저장까지 통째로 롤백된다 —
     * "이메일은 저장됐는데 인증은 실패한" 상태가 남지 않는다.
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        String passwordHash = passwordEncoder.encode(request.password());
        User user =
                User.createMember(
                        request.email(), passwordHash, request.nickname(), request.phoneNumber());
        User saved = userRepository.save(user);

        emailVerificationService.consumeForSignup(
                request.email(), request.emailVerificationToken(), saved.getId());

        return userConverter.toSignupResponse(saved);
    }

    /**
     * 클라이언트 로컬 회원가입 (A-API-002).
     *
     * <p>사업자등록번호는 {@link BusinessNumberValidationService#validateForClientSignup(String)} 로 최종 재검증한다.
     * 프론트의 A-API-005 사전 조회 결과는 신뢰하지 않는다.
     */
    @Transactional
    public ClientSignupResponse clientSignup(ClientSignupRequest request) {
        // 비밀번호 일치 여부 검사
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        // 클라이언트 회원가입(clientSignup)에서 사업자등록번호를 서버에서 최종 검증
        String normalizedBusinessNumber =
                businessNumberValidationService.validateForClientSignup(request.businessNumber());

        // 회원가입 시 평문 비밀번호를 DB에 저장할 수 없는 해시값으로 변환
        String passwordHash = passwordEncoder.encode(request.password());
        User user =
                User.createClient(
                        request.email(), passwordHash, request.nickname(), request.phoneNumber());
        User savedUser = userRepository.save(user);

        // ClientProfile 엔티티 생성(사업자 정보)
        ClientProfile profile =
                ClientProfile.create(
                        savedUser.getId(), normalizedBusinessNumber, request.companyName());

        try {
            clientProfileRepository.save(profile);
        } catch (DataIntegrityViolationException ex) {
            throw toBusinessException(ex);
        }

        emailVerificationService.consumeForSignup(
                request.email(), request.emailVerificationToken(), savedUser.getId());

        return clientProfileConverter.toClientSignupResponse(savedUser, profile);
    }

    /** 동시 가입 등으로 unique 제약이 위반된 경우 도메인 예외로 변환한다. */
    // DataIntegrityViolationException :  Spring이 DB 제약 위반(UNIQUE, NOT NULL 등)을 감지했을 때 던지는 예외
    private BusinessException toBusinessException(DataIntegrityViolationException ex) {
        // 가장 안쪽(실제 원인) 예외를 꺼냅니다.그 메시지 문자열을 message에 담아, 어떤 컬럼이 중복됐는지 판별합니다.
        String message = ex.getMostSpecificCause().getMessage();
        // getMessage()가 null이면 → 아래 contains() 검사를 건너뛰고 119줄 INVALID_INPUT으로 처리
        if (message != null) {
            // 대소문자 통일해서 어떤 UNIQUE 제약이 깨졌는지 문자열로 추론
            String lower = message.toLowerCase();
            // 110줄에서 소문자로 바꾼 lower에 "business_number"가 있으면, 사업자등록번호 중복으로 판단합니다.
            if (lower.contains("business_number")) {
                return new BusinessException(ErrorCode.DUPLICATE_BUSINESS_NUMBER);
            }
            if (lower.contains("email")) {
                return new BusinessException(ErrorCode.DUPLICATE_EMAIL);
            }
            if (lower.contains("nickname")) {
                return new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
            }
        }
        return new BusinessException(ErrorCode.INVALID_INPUT);
    }
}
