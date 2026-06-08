package com.disputeintel.repository;

import com.disputeintel.domain.Chargeback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;

public interface ChargebackRepository extends JpaRepository<Chargeback, String> {

    /** Filter by any combination of fields; a null param means "ignore". */
    @Query("""
        select c from Chargeback c
        where (:status is null or c.status = :status)
          and (:reasonCode is null or c.reasonCode = :reasonCode)
          and (:productCategory is null or c.productCategory = :productCategory)
          and (:customerCountry is null or c.customerCountry = :customerCountry)
          and (:merchantId is null or c.merchantId = :merchantId)
        order by c.openedAt desc
        """)
    List<Chargeback> search(@Param("status") String status,
                            @Param("reasonCode") String reasonCode,
                            @Param("productCategory") String productCategory,
                            @Param("customerCountry") String customerCountry,
                            @Param("merchantId") String merchantId);

    long countByStatus(String status);

    @Query("select count(c) from Chargeback c where c.status in ('won','lost')")
    long countResolved();

    @Query("select count(c) from Chargeback c where c.status = 'won'")
    long countWon();

    @Query("select coalesce(sum(c.amount),0) from Chargeback c")
    java.math.BigDecimal totalAmount();

    @Query("select coalesce(avg(c.amount),0) from Chargeback c")
    java.math.BigDecimal avgAmount();

    /** Generic grouped counts: returns [groupValue, disputeCount, wonCount, resolvedCount, sumAmount]. */
    @Query("""
        select g, count(c),
               sum(case when c.status = 'won' then 1 else 0 end),
               sum(case when c.status in ('won','lost') then 1 else 0 end),
               coalesce(sum(c.amount),0)
        from Chargeback c
        join (select c2.id as id,
                     case
                       when :dimension = 'reasonCode' then c2.reasonCode
                       when :dimension = 'productCategory' then c2.productCategory
                       when :dimension = 'customerCountry' then c2.customerCountry
                       when :dimension = 'merchantId' then c2.merchantId
                       else c2.reasonCategory
                     end as g
              from Chargeback c2) sub on sub.id = c.id
        group by g
        order by count(c) desc
        """)
    List<Object[]> groupedMetrics(@Param("dimension") String dimension);

    /** Win rate per reason code, learned from resolved history: [code, won, resolved]. */
    @Query("""
        select c.reasonCode,
               sum(case when c.status = 'won' then 1 else 0 end),
               sum(case when c.status in ('won','lost') then 1 else 0 end)
        from Chargeback c
        group by c.reasonCode
        """)
    List<Object[]> winStatsByReasonCode();

    /** Open disputes still within their deadline (the only fightable ones). */
    @Query("select c from Chargeback c where c.status = 'open' and c.deadlineAt >= :now")
    List<Chargeback> findFightable(@Param("now") OffsetDateTime now);
}
