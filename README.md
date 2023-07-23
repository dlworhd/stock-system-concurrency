![](https://velog.velcdn.com/images/rockstar/post/d2a9fa55-a432-47ee-8170-ac959ede14b3/image.png)

---
# 동시성 이슈
재고를 관리하는 시스템에서 공유 자원에 둘 이상의 쓰레드가 동시에 접근하여 데이터의 불일치가 발생하는 상황을 전제로 이슈를 해결하려고 한다.

## Synchronized
메서드 레벨에서 사용되는 키워드이며, 동기화라는 뜻을 가지고 있다. 동기화는 다른 메서드의 접근을 막기 위해 존재하는 개념이며, 임계 영역을 설정하여 동시 접근을 제어한다. 즉, 해당 키워드가 적용된 메서드에는 하나의 쓰레드만 접근 가능하다. 메서드 뿐만 아니라 메서드 내부에서 더 작은 영역까지 Synchronized 적용도 가능하다.

### 멀티쓰레드 테스트
Synchronized를 사용해서 메서드 동시 접근을 막고, ExecutorService를 이용해서 원하는 Thread Pool 사이즈를 설정하여 간단하게 멀티쓰레딩을 테스트할 수 있다. 

```
 @Transactional
 @Synchronized
    fun decrease(productId: Long, quantity: Long) {
        val productStock = productStockRepository.findByProduct_Id(productId)
        productStock?.decrease(quantity)
        productStock?.let { productStockRepository.saveAndFlush(it) }
    }
```

```
@SpringBootTest
class ProductStockServiceTest {

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var productStockRepository: ProductStockRepository

    @Autowired
    lateinit var productStockService: ProductStockService


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
        val productStock = productStockRepository.findByProduct_Id(productId)
        val latch = CountDownLatch(threadCount)
        val executorService: ExecutorService = Executors.newFixedThreadPool(32);
        
        for (i in 1..threadCount) {
            executorService.submit() {
                try {
                    productStockService.decrease(product.id, 1);
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        Assertions.assertThat(productStock?.quantity).isEqualTo(0)

    }
}


```
코드를 작성해서 실행했을 때, 예상되는 값은 0이지만 실제 값은 0이 아니다. 왜 그럴까? ![](https://velog.velcdn.com/images/rockstar/post/626a3ad5-591f-4f9c-8cff-f2fbd1dd9459/image.png)

우선 @Transactional이 적용된 메서드의 실행 원리를 알아야 한다. @Transactional은 AOP를 위한 애너테이션인데, 일일이 getTransaction()을 하고 커밋 또는 롤백하는 걸 구현하기가 번거롭기 때문에 고안된 방법이다. 해당 애너테이션을 적용시키면 코드 진행이 아래와 같아진다.

```
startTransaction();

decrease();

endTransaction();
```

우리가 synchronized를 적용한 것은 decrease()이지, 위아래의 Transaction과 관련된 로직이 아니다. 그렇기 때문에, 멀티 쓰레드 환경에서
decrease()가 끝난 후 endTransaction()이 실행되기 전에 접근을 할 수 있게 되고, DB 커밋이 되기 전의 데이터를 읽어들이게 되는 것이다.

### @Transactional 제거
그렇다면 @Transactional을 제거하면 어떻게 될까? 아래처럼 주석처리하고 테스트를 다시 실행해보자.

```
    //@Transactional
    @Synchronized
    fun decrease(productId: Long, quantity: Long) {
        val productStock = productStockRepository.findByProduct_Id(productId)
        productStock?.decrease(quantity)
        productStock?.let { productStockRepository.saveAndFlush(it) } // @Transactional을 적용하지 않는 경우 해당 코드를 사용하여 변경사항 Flush
    }
```

아래처럼 테스트가 성공하는 것을 볼 수 있다.
![](https://velog.velcdn.com/images/rockstar/post/3922fcdd-a99b-4d67-abaa-1d8e5cb4b6d5/image.png)

다만, Synchronized를 사용하는 경우 한 개의 서버에서만 임계 영역을 보장 받기 때문에 서버가 한 개가 아닌 여러 개인 경우 Synchronized를 사용할 이유가 없어지게 된다. 그래서 실무에서도 거의 사용하지 않는다고 하기에 더 좋은 방법을 찾아야 될 것 같다.

# Pessimistic Lock
Pessimistic Lock(비관적인 락)은 실제로 데이터에 Lock을 걸어서 정합성을 맞추는 방법이다. Exclusive Lock을 걸게 되면 다른 트랜잭션에서는 Lock이 해제되기 전까지는 데이터를 가져갈 수 없게 된다. 다만, 데드락이 걸릴 수 있기 때문에 주의하여 사용해야 한다.

Lock을 통해 업데이트를 제어하기 때문에 데이터 작업에 대한 보장이 된다. 단점으로는 별도의 락을 걸기 때문에 성능 문제가 생길 수 있다는 점이 있지만, 충돌이 빈번하게 일어나는 경우에는 Optimistic Lock보다 나을 수 있다.

```
	//Spring이 지원하는 Pessimistic 관련 Lock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ProductStock s WHERE s.product.id = :id")
    fun findByProductIdWithPessimisticLock(@Param("id") productId: Long): ProductStock?
```

```
	 @Transactional
    fun decrease(productId: Long, quantity: Long) {
        val productStock = productStockRepository.findByProductIdWithPessimisticLock(productId)
        productStock?.decrease(quantity)
    }
```
```
	@Test
    fun `pessimisticTest`() {

        val threadCount = 100;
        val product = productRepository.findById(productId).orElseThrow()
        val latch = CountDownLatch(threadCount)
        val executorService: ExecutorService = Executors.newFixedThreadPool(32);

        for (i in 1..threadCount) {
            executorService.submit {
                try {
                    pessimisticLockService.decrease(product.id, 1);
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()

        val savedProductStock = productStockRepository.findByProduct_Id(productId);
        Assertions.assertThat(savedProductStock?.quantity).isEqualTo(0)

    }
```
```
// Data에 Lock을 걸고 가져오는 쿼리 출력
2023-07-23T19:45:23.855+09:00 DEBUG 8086 --- [ool-2-thread-13] org.hibernate.SQL                        : select p1_0.id,p1_0.product_id,p1_0.quantity from product_stock p1_0 where p1_0.product_id=? for update
```

![](https://velog.velcdn.com/images/rockstar/post/43441c68-9315-4b09-b098-148d19a077ef/image.png)

구현 난이도가 그렇게 어렵지 않다. 테스트가 잘 성공하는 걸 볼 수 있지만, 속도가 이전보다 느려진 감이 있다. 그리고 여러 번 실행했을 때 한 번씩 Connection 관련 에러가 보이기도 했다. 

# Optimistic Lock
Optimistic Lock(낙관적인 락)은 실제로 Lock을 사용하지 않고, 버전을 이용함으로써 정합성을 맞추는 방법이다. 먼저 데이터를 읽은 후에 Update를 수행할 때 현재 내가 읽은 버전이 맞는지 쿼리에서 조건 처리를 하여 Update를 하게 된다. 만약 조회했던 버전에서 수정사항이 생긴 경우(버전이 바뀐 경우) 다시 조회 후에 작업을 수행하도록 해야 한다.

```
 @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT s FROM ProductStock s WHERE s.product.id = :id")
    fun findByProductIdWithOptimisticLock(@Param("id") id: Long): ProductStock?
```
```
@Service
class OptimisticLockService(private val productStockRepository: ProductStockRepository) {

    @Transactional
    fun decrease(productId: Long, quantity: Long) {
        val productStock = productStockRepository.findByProductIdWithOptimisticLock(productId)
        productStock?.decrease(quantity)
        productStock?.let { productStockRepository.saveAndFlush(it) }
    }
}

```
```
@Service
class OptimisticLockFacade(var optimisticLockService: OptimisticLockService) {

    fun decrease(profileId: Long, quantity: Long){
        while(true) {
            try {
                optimisticLockService.decrease(profileId, quantity)
                break
            } catch (e: Exception) {
                Thread.sleep(50)
            }
        }
    }
}
```
```
@Test
    fun `optimisticLockTest`() {

        val threadCount = 100;
        val product = productRepository.findById(productId).orElseThrow()
        val latch = CountDownLatch(threadCount)
        val executorService: ExecutorService = Executors.newFixedThreadPool(32);

        for (i in 1..threadCount) {
            executorService.submit {
                try {
                    optimisticLockFacade.decrease(product.id, 1);
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()

        val savedProductStock = productStockRepository.findByProduct_Id(productId);
        Assertions.assertThat(savedProductStock?.quantity).isEqualTo(0)

    }
```
![](https://velog.velcdn.com/images/rockstar/post/7edb3775-7e19-4768-b74e-e7ce7bb939b5/image.png)

재고 감소 로직과 버전이 안 맞을 때 재실행할 수 있도록 해주는 로직을 분리하기 위해서 Facade 디자인 패턴을 적용하였다. 

# Named Lock

Named Lock(네임드 락)은 이름을 가진 Metadata Locking이다. 이름을 가진 Lock을 획득한 후 해제할 때까지 다른 세션은 Lock을 획득할 수 없게 된다. 주의점은 Transaction이 종료될 때 이 Lock이 자동으로 해제되지 않기 때문에, 별도의 명령어로 해제를 수행해야 하거나 선점 시간이 끝날 때까지 기다려야 한다.

코드를 작성할 때, Lock을 획득하기 위한 쿼리와 Release를 하기 위한 쿼리를 작성해준다. Service는 기존 서비스의 서비스 코드를 이용하면 된다. 특별히 뭔가 추가되거나 그런 게 아니라 Key를 획득하는 쿼리와 Release하는 쿼리만 있으면 된다.

마지막으로는 역시 파사드패턴을 통해 Lock, Relase하는 코드와 재고 감소 로직을 분리시켜서 처리를 해줬다.
```
 @Query(value = "SELECT get_lock(:key, 3000)", nativeQuery = true)
    fun getLock(@Param("key") key: String)

    @Query(value = "SELECT release_lock(:key)", nativeQuery = true)
    fun releaseLock(@Param("key") key: String)
```
```
@Transactional
    fun decrease(profileId: Long, quantity: Long){

        try {
            lockRepository.getLock(profileId.toString())
            stockService.decrease(profileId, quantity)
        } finally {
            lockRepository.releaseLock(profileId.toString())
        }

    }
```
```
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
```
![](blob:https://velog.io/32c5342e-ed2f-4586-8d97-089548ae4622)

역시나 테스트를 잘 통과하는 걸 볼 수 있다. 큰 이슈 없이 잘 실행된다. 추가적으로 Connection Pool Size를 40 정도로 설정 후 테스트했으니 참고바란다. 
