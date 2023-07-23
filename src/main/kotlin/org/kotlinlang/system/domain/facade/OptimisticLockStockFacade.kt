package org.kotlinlang.system.domain.facade

import org.kotlinlang.system.domain.product.OptimisticLockStockService
import org.springframework.stereotype.Service

@Service
class OptimisticLockStockFacade(var optimisticLockStockService: OptimisticLockStockService) {

    fun decrease(profileId: Long, quantity: Long){
        while(true) {
            try {
                optimisticLockStockService.decrease(profileId, quantity)
                break
            } catch (e: Exception) {
                Thread.sleep(50)
            }
        }
    }
}