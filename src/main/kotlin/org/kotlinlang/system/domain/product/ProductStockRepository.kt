package org.kotlinlang.system.domain.product

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductStockRepository: JpaRepository<ProductStock, Long> {

    fun findByProduct_Id(productId: Long): ProductStock?


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ProductStock s WHERE s.product.id = :id")
    fun findByProductIdWithPessimisticLock(@Param("id") id: Long): ProductStock?

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT s FROM ProductStock s WHERE s.product.id = :id")
    fun findByProductIdWithOptimisticLock(@Param("id") id: Long): ProductStock?

}