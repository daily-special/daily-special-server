package com.dailyspecial.server.domain.visit;

/**
 * 오늘 상태를 뽑는 데 <b>실제로 필요한 손님 정보만</b> 담는다. 페르소나 전체를 끌어오지 않는다.
 *
 * <p>페르소나는 데이터 계약이 소유하고 클라이언트가 번들로 읽는다. 도메인이 그 모양을 알게 되면
 * 계약이 바뀔 때마다 규칙이 흔들린다. 여기서는 계산에 쓰는 것만 받고, 페르소나에서 이 값을
 * 뽑아내는 일은 바깥 층이 한다.
 *
 * @param prefersAffordable 선호 욕구에 "저렴"이 있는가
 */
public record GuestTraits(boolean prefersAffordable) {
}
