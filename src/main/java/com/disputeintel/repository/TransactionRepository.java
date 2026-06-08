package com.disputeintel.repository;

import com.disputeintel.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    /** Transaction counts grouped by a dimension (the chargeback-rate denominator). */
    @Query("""
        select case
                 when :dimension = 'productCategory' then t.productCategory
                 when :dimension = 'customerCountry' then t.customerCountry
                 when :dimension = 'merchantId' then t.merchantId
                 else t.productCategory
               end as g,
               count(t)
        from Transaction t
        group by g
        """)
    List<Object[]> countsByDimension(@Param("dimension") String dimension);
}
