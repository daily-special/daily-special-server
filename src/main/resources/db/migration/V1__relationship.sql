-- 이 서버의 첫 영속 상태. 손님과의 관계는 플레이 중에 바뀌므로 서버가 소유한다 (계약 5절).
--
-- 오늘 상태·욕구·만족도는 여기 없다. 그쪽은 씨앗에서 결정적으로 다시 계산하거나
-- 클라이언트가 소유한다 — 저장할 이유가 없는 것을 저장하지 않는다.

create table guest_relationship (
    save_id  varchar(64) not null,
    guest_id varchar(64) not null,
    affinity integer     not null,
    primary key (save_id, guest_id)
);

-- 축 힌트를 JSON 한 칸에 넣지 않고 행으로 편다. 축은 계약 어휘라 개수가 적고,
-- 행으로 두면 "어느 축이 몇 번 어긋났나"를 SQL로 바로 볼 수 있다 — 밸런스를 볼 때 쓴다.
create table guest_relationship_axis_hint (
    save_id  varchar(64) not null,
    guest_id varchar(64) not null,
    axis     varchar(64) not null,
    hints    integer     not null,
    primary key (save_id, guest_id, axis),
    constraint fk_axis_hint_relationship
        foreign key (save_id, guest_id)
        references guest_relationship (save_id, guest_id)
        on delete cascade
);
