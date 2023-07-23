package org.kotlinlang.system.domain.product

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PessimisticLockStockService(private val productStockRepository: ProductStockRepository) {


    @Transactional
    fun decrease(productId: Long, quantity: Long) {
        val productStock = productStockRepository.findByProductIdWithPessimisticLock(productId)
        productStock?.decrease(quantity)
    }
}
