package com.dailyspecial.server.application.visit;

/** 콘텐츠 스냅샷에 없는 손님을 물었을 때. API 층이 404로 옮긴다. */
public class UnknownGuestException extends RuntimeException {

	private final String guestId;

	public UnknownGuestException(String guestId) {
		super("모르는 손님이다: " + guestId);
		this.guestId = guestId;
	}

	public String guestId() {
		return guestId;
	}
}
