package com.bandmong.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** id 커서 페이지: before보다 오래된 것부터 거꾸로. OFFSET을 쓰지 않아 새 메시지가 와도 밀리지 않는다. */
    @Query("select m from ChatMessage m left join fetch m.sender where m.band.id = :bandId and m.id < :before order by m.id desc")
    List<ChatMessage> findPage(@Param("bandId") Long bandId, @Param("before") Long before, Pageable page);

    Optional<ChatMessage> findByBandIdAndSenderIdAndClientId(Long bandId, Long senderId, String clientId);

    @Query("select coalesce(max(m.id), 0) from ChatMessage m where m.band.id = :bandId")
    long maxId(@Param("bandId") Long bandId);

    /** 밴드별 안 읽은 수 (내가 보낸 건 빼고). 여러 밴드를 쿼리 한 번으로. */
    @Query("""
            select m.band.id, count(m) from ChatMessage m
            join BandMember bm on bm.band = m.band and bm.user.id = :userId
            left join m.sender s
            where m.band.id in :bandIds
              and m.id > coalesce(bm.lastReadMessageId, 0)
              and (s is null or s.id <> :userId)
            group by m.band.id""")
    List<Object[]> unreadByBand(@Param("userId") Long userId, @Param("bandIds") Collection<Long> bandIds);
}
