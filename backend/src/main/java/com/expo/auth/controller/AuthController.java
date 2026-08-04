package com.expo.auth.controller;

import com.expo.auth.dto.AuthApiResponse;
import com.expo.auth.dto.BusinessNumberAvailabilityResponse;
import com.expo.auth.dto.SignupRequest;
import com.expo.auth.dto.SignupResponse;
import com.expo.auth.service.AuthService;
import com.expo.auth.service.BusinessNumberValidationService;
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
  private final BusinessNumberValidationService businessNumberValidationService;

  public AuthController(
      AuthService authService, BusinessNumberValidationService businessNumberValidationService) {
    this.authService = authService;
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
        //Swagger 문서용 설명입니다
          @Parameter(
                  description = "확인할 사업자등록번호. 하이픈 포함/미포함 모두 허용.",
                  example = "123-45-67890",
                  required = true)
              @RequestParam("businessNumber")
              String businessNumber) {
    BusinessNumberAvailabilityResponse result =
        businessNumberValidationService.checkAvailability(businessNumber);
        //HTTP 200과 함께 공통 응답 형식으로 반환합니다.
    return ResponseEntity.ok(AuthApiResponse.ok(result));
  }
}
