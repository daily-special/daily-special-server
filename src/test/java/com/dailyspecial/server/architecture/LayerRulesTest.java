package com.dailyspecial.server.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * 계층 규칙을 사람이 아니라 도구가 지키게 한다 (`docs/conventions.md` 1절).
 *
 * <p>파이프라인은 import-linter가, 클라이언트는 어셈블리 정의가 같은 선을 긋는다. 이 저장소의
 * 몫이 여기다. 규약에 적어두기만 하면 언젠가 깨지고, 깨진 뒤에 떼는 것은 비싸다.
 *
 * <p>{@code application} · {@code api} · {@code infra} 규칙은 그 패키지가 생길 때 더한다.
 */
@AnalyzeClasses(
		packages = "com.dailyspecial.server",
		importOptions = ImportOption.DoNotIncludeTests.class)
class LayerRulesTest {

	@ArchTest
	static final ArchRule 도메인은_프레임워크를_모른다 = noClasses()
			.that()
			.resideInAPackage("..domain..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage(
					"org.springframework..",
					"jakarta.persistence..",
					"jakarta.validation..",
					"com.fasterxml.jackson..",
					"tools.jackson..")
			.because("도메인은 DB도 스프링 컨텍스트도 없이 테스트돼야 한다");

	@ArchTest
	static final ArchRule 도메인은_바깥_계층을_모른다 = noClasses()
			.that()
			.resideInAPackage("..domain..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage("..api..", "..application..", "..infra..")
			.because("의존 화살표는 도메인으로만 들어간다")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule 유스케이스는_전달_수단을_모른다 = noClasses()
			.that()
			.resideInAPackage("..application..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage("..api..", "..infra..")
			.because("유스케이스는 HTTP로 불리든 아니든 같아야 하고, 저장 방식은 포트 뒤에 있다")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule API는_구현_세부를_모른다 = noClasses()
			.that()
			.resideInAPackage("..api..")
			.should()
			.dependOnClassesThat()
			.resideInAPackage("..infra..")
			.because("컨트롤러는 포트 구현이 무엇인지 알 필요가 없다")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule JPA는_infra에만_있다 = noClasses()
			.that()
			.resideOutsideOfPackage("..infra..")
			.should()
			.dependOnClassesThat()
			.resideInAPackage("jakarta.persistence..")
			.because("저장 모양이 새어 나오면 스키마를 바꿀 때 규칙과 화면까지 흔들린다")
			.allowEmptyShould(true);
}
