package com.dailyspecial.server.api;

import com.dailyspecial.server.application.visit.UnknownGuestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** 예외를 HTTP 응답으로 옮긴다. 도메인·유스케이스는 HTTP를 모른 채로 남는다. */
@RestControllerAdvice
public class ApiExceptionHandler {

	/** 스냅샷에 없는 손님. 콘텐츠 버전이 클라와 어긋났을 때 여기로 온다. */
	@ExceptionHandler(UnknownGuestException.class)
	ResponseEntity<ApiError> unknownGuest(UnknownGuestException cause) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiError("unknown_guest", cause.getMessage()));
	}

	/** 도메인 불변식 위반 — 0 이하 날짜, 빈 식별자 따위. 부른 쪽 잘못이다. */
	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ApiError> badArgument(IllegalArgumentException cause) {
		return ResponseEntity.badRequest().body(new ApiError("invalid_request", cause.getMessage()));
	}

	/** 경로 변수의 타입이 안 맞을 때 — 예를 들어 날짜 자리에 숫자가 아닌 값. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException cause) {
		return ResponseEntity.badRequest()
				.body(new ApiError("invalid_request", "경로 값이 올바르지 않다: " + cause.getName()));
	}
}
