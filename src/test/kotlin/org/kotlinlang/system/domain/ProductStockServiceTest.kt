package org.kotlinlang.system.domain

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kotlinlang.system.domain.facade.NamedLockStockFacade
import org.kotlinlang.system.domain.facade.OptimisticLockStockFacade
import org.kotlinlang.system.domain.product.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SpringBootTest
class ProductStockServiceTest {

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var productStockRepository: ProductStockRepository

    @Autowired
    lateinit var productStockService: ProductStockService

    @Autowired
    lateinit var pessimisticLockStockService: PessimisticLockStockService

    @Autowired
    lateinit var optimisticLockStockFacade: OptimisticLockStockFacade

    @Autowired
    lateinit var namedLockStockFacade: NamedLockStockFacade

    var productId: Long = 0

    @BeforeEach
    fun `init`() {
        val product = Product()
        product.name = "나이키 슈즈"

        val savedProduct = productRepository.save(product)
        val productStock = ProductStock(savedProduct, 100)
        val savedProductStock = productStockRepository.save(productStock)

        productId = savedProduct.id

    }

    @Test
    fun `productTest`() {

        val threadCount = 100;
        val product = productRepository.findById(productId).orElseThrow()
        val latch = CountDownLatch(threadCount)
        val executorService: ExecutorService = Executors.newFixedThreadPool(32);

        for (i in 1..threadCount) {
            executorService.submit {
                try {
                    productStockService.decrease(product.id, 1);
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()

        val savedProductStock = productStockRepository.findByProduct_Id(productId);
        Assertions.assertThat(savedProductStock?.quantity).isEqualTo(0)

    }

    @Test
    fun `pessimisticLockTest`() {

        val threadCount = 100;
        val product = productRepository.findById(productId).orElseThrow()
        val latch = CountDownLatch(threadCount)
        val executorService: ExecutorService = Executors.newFixedThreadPool(32);

        for (i in 1..threadCount) {
            executorService.submit {
                try {
                    pessimisticLockStockService.decrease(product.id, 1);
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()

        val savedProductStock = productStockRepository.findByProduct_Id(productId);
        Assertions.assertThat(savedProductStock?.quantity).isEqualTo(0)

    }

    @Test
    fun `optimisticLockTest`() {

        val threadCount = 100;
        val product = productRepository.findById(productId).orElseThrow()
        val latch = CountDownLatch(threadCount)
        val executorService: ExecutorService = Executors.newFixedThreadPool(32);

        for (i in 1..threadCount) {
            executorService.submit {
                try {
                    optimisticLockStockFacade.decrease(product.id, 1);
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()

        val savedProductStock = productStockRepository.findByProduct_Id(productId);
        Assertions.assertThat(savedProductStock?.quantity).isEqualTo(0)

    }


    @Test
    fun `namedLockTest`() {

        val threadCount = 100;
        val product = productRepository.findById(productId).orElseThrow()
        val latch = CountDownLatch(threadCount)
        val executorService: ExecutorService = Executors.newFixedThreadPool(32);

        for (i in 1..threadCount) {
            executorService.submit {
                try {
                    namedLockStockFacade.decrease(product.id, 1);
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()

        val savedProductStock = productStockRepository.findByProduct_Id(productId);
        Assertions.assertThat(savedProductStock?.quantity).isEqualTo(0)

    }
}
