package com.dailyspecial.server.infra.content;

import com.dailyspecial.server.application.port.GuestCatalog;
import com.dailyspecial.server.domain.visit.GuestTraits;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 파이프라인이 뱉은 `guests.json` 스냅샷에서 손님 성향을 읽는다.
 *
 * <p><b>서버가 고정 콘텐츠를 읽는 것은 계약 위반이 아니다.</b> 경계 기준은 "플레이 중에 바뀌면
 * 서버, 안 바뀌면 계약"이고, 선호 욕구는 안 바뀌니 계약이다. 서버는 그것을 <b>읽되 배달하지
 * 않는다</b> — 클라이언트는 자기 번들에서 페르소나를 읽는다.
 *
 * <p>이 값을 클라이언트가 요청에 실어 보내게 하지 않는 이유도 같다. 그러면 클라가 스스로
 * 부자 손님을 만들 수 있다.
 *
 * <p>스냅샷은 파이프라인이 원본을 소유한다. 콘텐츠를 다시 뽑으면 이 파일도 새로 받아야 한다.
 */
@Component
public class JsonGuestCatalog implements GuestCatalog {

	/** 파이프라인 `out/packages/<버전>/guests.json`의 사본. */
	private static final String RESOURCE_PATH = "content/guests.json";

	private static final int SUPPORTED_MAJOR = 1;
	private static final String EXPECTED_KIND = "guests";

	/** 선호 욕구에 이 값이 있으면 지갑이 빠듯한 손님이다 (데이터 계약 7절). */
	private static final String AFFORDABLE = "affordable";

	private final Map<String, GuestTraits> traitsById;

	public JsonGuestCatalog(ObjectMapper objectMapper) {
		this.traitsById = load(objectMapper);
	}

	@Override
	public Optional<GuestTraits> findTraits(String guestId) {
		return Optional.ofNullable(traitsById.get(guestId));
	}

	/** 스냅샷에 들어 있는 손님 수. 기동 로그와 테스트가 쓴다. */
	public int size() {
		return traitsById.size();
	}

	private static Map<String, GuestTraits> load(ObjectMapper objectMapper) {
		JsonNode root = read(objectMapper);

		checkKind(root);
		checkSchemaVersion(root);

		JsonNode items = root.path("items");
		if (!items.isArray() || items.isEmpty()) {
			throw new IllegalStateException(RESOURCE_PATH + ": items가 비었다");
		}

		Map<String, GuestTraits> traits = new LinkedHashMap<>();
		for (JsonNode item : items) {
			String guestId = item.path("guest_id").stringValue(null);
			if (guestId == null || guestId.isBlank()) {
				throw new IllegalStateException(RESOURCE_PATH + ": guest_id가 없는 항목이 있다");
			}
			if (traits.put(guestId, new GuestTraits(prefersAffordable(item))) != null) {
				throw new IllegalStateException(RESOURCE_PATH + ": guest_id가 겹친다 — " + guestId);
			}
		}
		return Map.copyOf(traits);
	}

	private static boolean prefersAffordable(JsonNode item) {
		for (JsonNode need : item.path("preferred_needs")) {
			if (AFFORDABLE.equals(need.stringValue(null))) {
				return true;
			}
		}
		return false;
	}

	private static void checkKind(JsonNode root) {
		String kind = root.path("kind").stringValue(null);
		if (!EXPECTED_KIND.equals(kind)) {
			throw new IllegalStateException(
					"%s: kind가 %s가 아니다 — %s".formatted(RESOURCE_PATH, EXPECTED_KIND, kind));
		}
	}

	/**
	 * major가 다르면 <b>기동을 막는다</b> (데이터 계약 3-2절).
	 *
	 * <p>클라이언트와 같은 규칙이다. 어긋난 스냅샷으로 조용히 뜨면 손님이 통째로 404가 되거나
	 * 밸런스가 어긋난 채 굴러간다 — 둘 다 한참 뒤에 발견된다.
	 */
	private static void checkSchemaVersion(JsonNode root) {
		String version = root.path("schema_version").stringValue(null);
		if (version == null || version.isBlank()) {
			throw new IllegalStateException(RESOURCE_PATH + ": schema_version이 없다");
		}

		int major;
		try {
			major = Integer.parseInt(version.split("\\.", 2)[0]);
		} catch (NumberFormatException cause) {
			throw new IllegalStateException(
					"%s: schema_version을 읽을 수 없다 — %s".formatted(RESOURCE_PATH, version), cause);
		}

		if (major != SUPPORTED_MAJOR) {
			throw new IllegalStateException(
					"%s: 지원하지 않는 schema_version %s (major %d만 읽는다)"
							.formatted(RESOURCE_PATH, version, SUPPORTED_MAJOR));
		}
	}

	private static JsonNode read(ObjectMapper objectMapper) {
		try (InputStream source = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
			return objectMapper.readTree(source);
		} catch (IOException | JacksonException cause) {
			throw new IllegalStateException(RESOURCE_PATH + "을(를) 읽을 수 없다", cause);
		}
	}
}
