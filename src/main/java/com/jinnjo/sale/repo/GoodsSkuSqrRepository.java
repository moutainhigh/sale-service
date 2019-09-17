package com.jinnjo.sale.repo;

import com.jinnjo.sale.domain.GoodsSkuSqr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoodsSkuSqrRepository extends JpaRepository<GoodsSkuSqr, Long>{
}
