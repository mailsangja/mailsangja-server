package com.mailsangja.core.common.exception.payment;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    PAYMENT_ID_BLANK(400, "MS-PAYMENT-ID-BLANK", "결제 ID는 필수입니다."),
    PAYMENT_NOT_FOUND(404, "MS-PAYMENT-NOT-FOUND", "결제 정보를 찾을 수 없습니다."),
    PAYMENT_AMOUNT_MISMATCH(400, "MS-PAYMENT-AMOUNT-MISMATCH", "결제 금액이 일치하지 않습니다."),
    PAYMENT_STATUS_INVALID(400, "MS-PAYMENT-STATUS-INVALID", "결제 상태가 유효하지 않습니다."),
    PAYMENT_VERIFICATION_FAILED(502, "MS-PAYMENT-VERIFICATION-FAILED", "결제 검증에 실패했습니다."),
    PAYMENT_PLAN_UNKNOWN(400, "MS-PAYMENT-PLAN-UNKNOWN", "알 수 없는 플랜입니다."),
    PAYMENT_WEBHOOK_INVALID(400, "MS-PAYMENT-WEBHOOK-INVALID", "유효하지 않은 웹훅 요청입니다."),
    PAYMENT_USER_NOT_FOUND(404, "MS-PAYMENT-USER-NOT-FOUND", "결제 대상 사용자를 찾을 수 없습니다."),
    ORDER_NOT_FOUND(404, "MS-PAYMENT-ORDER-NOT-FOUND", "주문 정보를 찾을 수 없습니다."),
    ORDER_FORBIDDEN(403, "MS-PAYMENT-ORDER-FORBIDDEN", "해당 주문에 대한 권한이 없습니다.");

    private final int status;
    private final String code;
    private final String message;
}
