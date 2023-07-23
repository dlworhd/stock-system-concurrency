package org.kotlinlang.system.domain.product

import org.kotlinlang.system.dto.ProductDto
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/stocks")
class StockController(private val productStockService: ProductStockService) {

    @PostMapping
    fun createProduct(
        @RequestBody productDto: ProductDto
    ) {
        productStockService.createProduct(productDto)
    }

    @PutMapping("/decrease/{productId}")
    fun decrease(
        @PathVariable productId: Long,
        @RequestParam quantity: Long
    ) {
        productStockService.decrease(productId, quantity)
    }

    @PutMapping("/increase/{productId}")
    fun increase(
        @PathVariable productId: Long,
        @RequestParam quantity: Long
    ) {
        productStockService.increase(productId, quantity)
    }


}