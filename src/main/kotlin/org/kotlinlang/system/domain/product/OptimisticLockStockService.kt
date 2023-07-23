package org.kotlinlang.system.domain.product

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OptimisticLockStockService(private val productStockRepository: ProductStockRepository) {

    @Transactional
    fun decrease(productId: Long, quantity: Long) {
        val productStock = productStockRepository.findByProductIdWithOptimisticLock(productId)
        productStock?.decrease(quantity)
        productStock?.let { productStockRepository.saveAndFlush(it) }
    }
}
