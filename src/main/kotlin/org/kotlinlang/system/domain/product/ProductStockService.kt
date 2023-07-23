package org.kotlinlang.system.domain.product

import org.kotlinlang.system.dto.ProductDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class ProductStockService(private var productStockRepository: ProductStockRepository,
                          private var productRepository: ProductRepository
) {

    @Transactional
    fun createProduct(request: ProductDto){
        var product = Product()
        product.name = request.productName
        val savedProduct = productRepository.save(product)

        var productStock = ProductStock(savedProduct, request.quantity)

        productStockRepository.save(productStock)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    @Synchronized
    fun decrease(productId: Long, quantity: Long) {
        val productStock = productStockRepository.findByProduct_Id(productId)
        productStock?.decrease(quantity)
        productStock?.let { productStockRepository.saveAndFlush(it) }
    }

    @Transactional
    fun increase(productId: Long, quantity: Long) {
        val productStock = productStockRepository.findByProduct_Id(productId)
        productStock?.increase(quantity)
        productStock?.let { productStockRepository.saveAndFlush(it) }
    }

}