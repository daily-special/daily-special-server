package com.dailyspecial.server.api.relationship;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 방문 하나의 결과. <b>클라이언트가 계산한 것</b>이다.
 *
 * @param satisfaction 만족도 0~1. 클라이언트의 만족도 엔진이 낸 값
 * @param offAxes      이상 구간을 벗어난 축들. 없으면 빈 배열
 */
public record RecordVisitRequest(
		@NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double satisfaction, List<String> offAxes) {

	public List<String> offAxesOrEmpty() {
		return offAxes == null ? List.of() : offAxes;
	}
}
