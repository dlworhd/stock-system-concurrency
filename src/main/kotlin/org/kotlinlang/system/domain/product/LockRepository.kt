package org.kotlinlang.system.domain.product

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LockRepository: JpaRepository<ProductStock, Long> {

    @Query(value = "SELECT get_lock(:key, 3000)", nativeQuery = true)
    fun getLock(@Param("key") key: String)

    @Query(value = "SELECT release_lock(:key)", nativeQuery = true)
    fun releaseLock(@Param("key") key: String)

}