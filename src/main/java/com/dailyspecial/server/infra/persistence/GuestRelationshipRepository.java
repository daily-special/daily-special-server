package com.dailyspecial.server.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** 스프링 데이터 리포지토리. 바깥은 이것을 모르고 포트만 본다. */
interface GuestRelationshipRepository
		extends JpaRepository<GuestRelationshipEntity, GuestRelationshipEntity.Key> {}
