package com.dailyspecial.server.api;

/**
 * 오류 응답. 모든 실패가 이 모양으로 나간다.
 *
 * @param error   기계가 분기할 값. 소문자 `snake_case`로 고정한다 — 문구가 바뀌어도 여기는 안 바뀐다
 * @param message 사람이 읽을 설명. 여기에 기대어 분기하지 말 것
 */
public record ApiError(String error, String message) {
}
