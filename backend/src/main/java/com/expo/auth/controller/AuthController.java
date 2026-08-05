package com.expo.auth.controller;

import com.expo.auth.dto.AuthApiResponse;
import com.expo.auth.dto.BusinessNumberAvailabilityResponse;
import com.expo.auth.dto.ClientSignupRequest;
import com.expo.auth.dto.ClientSignupResponse;
import com.expo.auth.dto.EmailAvailabilityResponse;
import com.expo.auth.dto.SignupRequest;
import com.expo.auth.dto.SignupResponse;
import com.expo.auth.service.AuthService;
import com.expo.auth.service.BusinessNumberValidationService;
import com.expo.auth.service.EmailAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final EmailAvailabilityService emailAvailabilityService;
  private final BusinessNumberValidationService businessNumberValidationService;

  public AuthController(
      AuthService authService,
      EmailAvailabilityService emailAvailabilityService,
      BusinessNumberValidationService businessNumberValidationService) {
    this.authService = authService;
    this.emailAvailabilityService = emailAvailabilityService;
    this.businessNumberValidationService = businessNumberValidationService;
  }

  @Operation(summary = "일반 회원 로컬 회원가입", description = "이메일·비밀번호·닉네임으로 MEMBER 계정을 생성합니다.")
  @PostMapping("/signup")
  public ResponseEntity<AuthApiResponse<SignupResponse>> signup(
      // JSON body 를 SignupRequest 로 변환
      @Valid @RequestBody SignupRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(AuthApiResponse.ok(authService.signup(request)));
  }

  /**
   * 사업자등록번호 사용 가능 여부 확인 (A-API-005).
   *
   * <p>MVP 테스트 환경 전용. 외부 국세청·공공데이터 API 를 호출하지 않고, 서버에 등록된 테스트 번호 목록과 DB 중복 여부만 확인한다.
   */
  @Operation(
      summary = "사업자등록번호 사용 가능 여부 확인",
      description =
          """
          클라이언트 회원가입 전에 사업자등록번호를 사용할 수 있는지 확인합니다.

          - 외부 국세청 / 공공데이터 사업자등록번호 API 를 호출하지 않습니다.
          - 서버에 사전 등록된 테스트 번호 목록과 DB 중복 여부만 확인합니다.
          - 하이픈 포함 또는 미포함 입력을 허용합니다. (예: `1234567890`, `123-45-67890`)
          - 서버는 하이픈을 제거한 숫자 10자리로 정규화한 뒤 응답합니다.

          사용 가능한 테스트 번호: `1234567890`, `1111111111`, `2222222222`
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "판정 결과 반환 (available=true/false)",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = AuthApiResponse.class),
                examples = {
                  @ExampleObject(
                      name = "사용 가능",
                      value =
                          """
                          {
                            "success": true,
                            "data": {
                              "businessNumber": "1234567890",
                              "valid": true,
                              "duplicate": false,
                              "available": true,
                              "message": "사용 가능한 사업자등록번호입니다."
                            },
                            "message": null
                          }
                          """),
                  @ExampleObject(
                      name = "테스트 번호가 아님",
                      value =
                          """
                          {
                            "success": true,
                            "data": {
                              "businessNumber": "9999999999",
                              "valid": false,
                              "duplicate": false,
                              "available": false,
                              "message": "테스트용으로 등록되지 않은 사업자등록번호입니다."
                            },
                            "message": null
                          }
                          """),
                  @ExampleObject(
                      name = "이미 가입된 번호",
                      value =
                          """
                          {
                            "success": true,
                            "data": {
                              "businessNumber": "1234567890",
                              "valid": true,
                              "duplicate": true,
                              "available": false,
                              "message": "이미 가입된 사업자등록번호입니다."
                            },
                            "message": null
                          }
                          """)
                })),
    @ApiResponse(
        responseCode = "400",
        description = "형식 오류(빈 값 / 자릿수 오류 / 잘못된 하이픈 위치 / 문자 포함 등)",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "data": null,
                              "message": "사업자등록번호 형식이 올바르지 않습니다."
                            }
                            """)))
  })
  @GetMapping("/business-number-availability")
  public ResponseEntity<AuthApiResponse<BusinessNumberAvailabilityResponse>>
      checkBusinessNumberAvailability(
          // Swagger 문서용 설명입니다
          @Parameter(
                  description = "확인할 사업자등록번호. 하이픈 포함/미포함 모두 허용.",
                  example = "123-45-67890",
                  required = true)
              @RequestParam("businessNumber")
              String businessNumber) {
    BusinessNumberAvailabilityResponse result =
        businessNumberValidationService.checkAvailability(businessNumber);
    // HTTP 200과 함께 공통 응답 형식으로 반환합니다.
    return ResponseEntity.ok(AuthApiResponse.ok(result));
  }

  /**
   * 클라이언트 로컬 회원가입 (A-API-002).
   *
   * <p>사업자정보를 포함하여 CLIENT 권한 계정을 생성한다. 사업자등록번호는 회원가입 시 서버에서 최종 재검증한다.
   */
  @Operation(
      summary = "클라이언트 로컬 회원가입",
      description =
          """
          사업자정보를 포함한 클라이언트 회원가입 API입니다.

          - 가입 권한: CLIENT
          - 실제 국세청 / 공공데이터 사업자등록번호 API 를 호출하지 않습니다.
          - 서버에 등록된 테스트 사업자등록번호만 가입 가능합니다.
          - 사업자등록번호는 하이픈 포함 또는 미포함 입력 가능합니다.
          - DB에는 하이픈을 제거한 숫자 10자리로 저장합니다.
          - 이메일·사업자등록번호 중복 가입은 불가합니다.
          - 회원가입 시 사업자등록번호를 A-API-005와 동일 Service 로 최종 재검증합니다.

          테스트 사업자등록번호: `1234567890`, `1111111111`, `2222222222`
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "회원가입 성공",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        name = "정상 가입",
                        value =
                            """
                            {
                              "success": true,
                              "data": {
                                "userId": 1,
                                "role": "CLIENT",
                                "email": "client@espotic.com",
                                "companyName": "주식회사 에스포틱",
                                "message": "클라이언트 회원가입이 완료되었습니다."
                              },
                              "message": null
                            }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "입력값 오류 (비밀번호 불일치, 형식 오류, 약관 미동의 등)",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "data": null,
                              "message": "비밀번호가 일치하지 않습니다."
                            }
                            """))),
    @ApiResponse(
        responseCode = "409",
        description = "이메일 또는 사업자등록번호 중복",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = {
                  @ExampleObject(
                      name = "이메일 중복",
                      value =
                          """
                          {
                            "success": false,
                            "data": null,
                            "message": "이미 사용 중인 이메일입니다."
                          }
                          """),
                  @ExampleObject(
                      name = "사업자등록번호 중복",
                      value =
                          """
                          {
                            "success": false,
                            "data": null,
                            "message": "이미 가입된 사업자등록번호입니다."
                          }
                          """)
                }))
  })
  @PostMapping("/client-signup")
  public ResponseEntity<AuthApiResponse<ClientSignupResponse>> clientSignup(
      @Valid @RequestBody ClientSignupRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(AuthApiResponse.ok(authService.clientSignup(request)));
  }

  /**
   * 이메일 사용 가능 여부 확인 (A-API-003).
   *
   * <p>회원가입 전에 이메일 중복 여부를 조회한다. 형식 오류는 400, 판정 결과는 200 OK 로 반환한다.
   */
  @Operation(
      summary = "이메일 사용 가능 여부 확인",
      description =
          """
        회원가입 전에 이메일을 사용할 수 있는지 확인합니다.

        - 이메일 형식이 올바르지 않으면 400 Bad Request 를 반환합니다.
        - DB 에 동일 이메일이 있으면 `duplicate=true`, `available=false` 로 응답합니다.
        """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "판정 결과 반환 (available=true/false)",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = AuthApiResponse.class),
                examples = {
                  @ExampleObject(
                      name = "사용 가능",
                      value =
                          """
                        {
                          "success": true,
                          "data": {
                            "email": "member@espotic.com",
                            "valid": true,
                            "duplicate": false,
                            "available": true,
                            "message": "사용 가능한 이메일입니다."
                          },
                          "message": null
                        }
                        """),
                  @ExampleObject(
                      name = "이미 사용 중",
                      value =
                          """
                        {
                          "success": true,
                          "data": {
                            "email": "member@espotic.com",
                            "valid": true,
                            "duplicate": true,
                            "available": false,
                            "message": "이미 사용 중인 이메일입니다."
                          },
                          "message": null
                        }
                        """)
                })),
    @ApiResponse(
        responseCode = "400",
        description = "형식 오류(빈 값 / 잘못된 형식 / 255자 초과)",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        value =
                            """
                          {
                            "success": false,
                            "data": null,
                            "message": "올바른 이메일 형식이 아닙니다."
                          }
                          """)))
  })
  @GetMapping("/email-availability")
  public ResponseEntity<AuthApiResponse<EmailAvailabilityResponse>> checkEmailAvailability(
      @Parameter(description = "확인할 이메일", example = "member@espotic.com", required = true)
          @RequestParam("email")
          String email) {
    EmailAvailabilityResponse result = emailAvailabilityService.checkAvailability(email);
    return ResponseEntity.ok(AuthApiResponse.ok(result));
  }
}
