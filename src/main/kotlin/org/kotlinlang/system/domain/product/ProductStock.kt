package org.kotlinlang.system.domain.product

import jakarta.persistence.*

@Entity
class ProductStock(
    product: Product,
    quantity: Long
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    var product: Product = product

    var quantity: Long = quantity

    @Version
    var version: Long = 0

    fun canDecrease(quantity: Long): Boolean {
        if (quantity > this.quantity) {
            throw RuntimeException("Not enough quantity")
        }
        return true
    }

    fun decrease(quantity: Long) {
        if (canDecrease(quantity)) this.quantity -= quantity
    }

    fun increase(quantity: Long) {
        this.quantity += quantity
    }

}
