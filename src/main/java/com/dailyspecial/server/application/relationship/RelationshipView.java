package com.dailyspecial.server.application.relationship;

import com.dailyspecial.server.domain.relationship.Disclosure;
import com.dailyspecial.server.domain.relationship.Relationship;
import com.dailyspecial.server.domain.relationship.Tier;

/**
 * 한 손님과의 관계를 지금 시점에서 본 것.
 *
 * <p>단계와 공개는 관계에서 <b>유도된다.</b> 저장하지 않는 이유가 그것이다 — 수치를 조정하면
 * 같은 관계가 다른 단계로 읽혀야 하는데, 저장해두면 옛 판정이 남는다.
 */
public record RelationshipView(Relationship relationship, Tier tier, Disclosure disclosure) {}
