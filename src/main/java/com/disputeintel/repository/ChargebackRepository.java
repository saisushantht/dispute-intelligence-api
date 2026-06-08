package com.disputeintel.repository;

import com.disputeintel.domain.Chargeback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
}