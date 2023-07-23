package org.kotlinlang.system.domain.facade

import org.kotlinlang.system.domain.product.LockRepository
import org.kotlinlang.system.domain.product.OptimisticLockStockService
import org.kotlinlang.system.domain.product.ProductStockRepository
import org.kotlinlang.system.domain.product.ProductStockService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NamedLockStockFacade(
    private var stockService: ProductStockService,
    private var lockRepository: LockRepository
    ) {

    @Transactional
    fun decrease(profileId: Long, quantity: Long){

        try {
            lockRepository.getLock(profileId.toString())
            stockService.decrease(profileId, quantity)
        } finally {
            lockRepository.releaseLock(profileId.toString())
        }

    }
}