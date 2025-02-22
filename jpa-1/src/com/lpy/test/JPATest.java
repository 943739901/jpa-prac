package com.lpy.test;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.hibernate.ejb.QueryHints;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lpy.model.Category;
import com.lpy.model.Customer;
import com.lpy.model.Department;
import com.lpy.model.Item;
import com.lpy.model.Manager;
import com.lpy.model.Order;

/**
 * @author lipengyu
 * @date 2019/4/26 14:58
 */
public class JPATest {

    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private EntityTransaction transaction;


    @Before
    public void init() {
        entityManagerFactory = Persistence.createEntityManagerFactory("jpa-1");
        entityManager = entityManagerFactory.createEntityManager();
        transaction = entityManager.getTransaction();
        transaction.begin();
    }

    @After
    public void destory() {
        transaction.commit();
        entityManager.close();
        entityManagerFactory.close();
    }

    /**
     * 可以使用 JPQL 完成 UPDATE 和 DELETE 操作.
     */
    @Test
    public void testExecuteUpdate() {
        String jpql = "UPDATE Customer c SET c.lastName = ? WHERE c.id = ?";
        Query query = entityManager.createQuery(jpql).setParameter(1, "YYY").setParameter(2, 12);

        query.executeUpdate();
    }

    /**
     * 使用 jpql 内建的函数
     */
    @Test
    public void testJpqlFunction() {
        String jpql = "SELECT lower(c.email) FROM Customer c";

        List<String> emails = entityManager.createQuery(jpql).getResultList();
        System.out.println(emails);
    }

    /**
     * 查询所有 Customer 的 lastName 为 YY 的 Order
     */
    @Test
    public void testSubQuery() {
        String jpql = "SELECT o FROM Order o "
                + "WHERE o.customer = (SELECT c FROM Customer c WHERE c.lastName = ?)";

        Query query = entityManager.createQuery(jpql).setParameter(1, "YY");
        List<Order> orders = query.getResultList();
        System.out.println(orders.size());
    }

    /**
     * JPQL 的关联查询同 HQL 的关联查询.
     * 不加fetch会报错
     */
    @Test
    public void testLeftOuterJoinFetch() {
        String jpql = "FROM Customer c LEFT OUTER JOIN FETCH c.orders WHERE c.id = ?";

        Customer customer =
                (Customer) entityManager.createQuery(jpql).setParameter(1, 12).getSingleResult();
        System.out.println(customer.getLastName());
        System.out.println(customer.getOrders().size());

//		List<Object[]> result = entityManager.createQuery(jpql).setParameter(1, 12).getResultList();
//		System.out.println(result);
    }

    /**
     * 查询 order 数量大于 2 的那些 Customer
     */
    @Test
    public void testGroupBy() {
        String jpql = "SELECT o.customer FROM Order o "
                + "GROUP BY o.customer "
                + "HAVING count(o.id) >= 2";
        List<Customer> customers = entityManager.createQuery(jpql).getResultList();

        System.out.println(customers);
    }

    @Test
    public void testOrderBy() {
        String jpql = "FROM Customer c WHERE c.age > ? ORDER BY c.age DESC";
        Query query = entityManager.createQuery(jpql).setHint(QueryHints.HINT_CACHEABLE, true);

        //占位符的索引是从 1 开始
        query.setParameter(1, 1);
        List<Customer> customers = query.getResultList();
        System.out.println(customers.size());
    }

    /**
     * 使用 hibernate 的查询缓存.
     * 只会执行一次sql
     */
    @Test
    public void testQueryCache() {
        String jpql = "FROM Customer c WHERE c.age > ?";
        Query query = entityManager.createQuery(jpql).setHint(QueryHints.HINT_CACHEABLE, true);

        //占位符的索引是从 1 开始
        query.setParameter(1, 1);
        List<Customer> customers = query.getResultList();
        System.out.println(customers.size());

        query = entityManager.createQuery(jpql).setHint(QueryHints.HINT_CACHEABLE, true);

        //占位符的索引是从 1 开始
        query.setParameter(1, 1);
        customers = query.getResultList();
        System.out.println(customers.size());
    }

    /**
     * createNativeQuery 适用于本地 SQL
     */
    @Test
    public void testNativeQuery() {
        String sql = "SELECT age FROM jpa_cutomers WHERE id = ?";
        Query query = entityManager.createNativeQuery(sql).setParameter(1, 3);

        Object result = query.getSingleResult();
        System.out.println(result);
    }

    /**
     * createNamedQuery 适用于在实体类前使用 @NamedQuery 标记的查询语句
     */
    @Test
    public void testNamedQuery() {
        Query query = entityManager.createNamedQuery("testNamedQuery").setParameter(1, 3);
        Customer customer = (Customer) query.getSingleResult();

        System.out.println(customer);
    }

    /**
     * 默认情况下, 若只查询部分属性, 则将返回 Object[] 类型的结果. 或者 Object[] 类型的 List.
     * 也可以在实体类中创建对应的构造器, 然后再 JPQL 语句中利用对应的构造器返回实体类的对象.
     */
    @Test
    public void testPartlyProperties() {
        String jpql = "SELECT new Customer(c.lastName, c.age) FROM Customer c WHERE c.id > ?";
        List result = entityManager.createQuery(jpql).setParameter(1, 1).getResultList();

        System.out.println(result);
    }

    @Test
    public void testHelloJPQL() {
        String jpql = "FROM Customer c WHERE c.age > ?";
        Query query = entityManager.createQuery(jpql);

        //占位符的索引是从 1 开始
        query.setParameter(1, 1);
        List customers = query.getResultList();
        System.out.println(customers.size());
    }

    /**
     * 前提是数据库中有要查询的数据
     * 只查询一次则证明二级缓存生效
     * 记得加@Cacheable
     */
    @Test
    public void testSecondLevelCache() {
        Customer customer1 = entityManager.find(Customer.class, 2);

        transaction.commit();
        entityManager.close();

        entityManager = entityManagerFactory.createEntityManager();
        transaction = entityManager.getTransaction();
        transaction.begin();

        Customer customer2 = entityManager.find(Customer.class, 2);
    }


    /**
     * 对于关联的集合对象, 默认使用懒加载的策略.
     * 使用维护关联关系的一方获取, 还是使用不维护关联关系的一方获取, SQL 语句相同.
     *
     * 在知道一方id时，其实两表join即可
     */
    @Test
    public void testManyToManyFind() {
        Item item = entityManager.find(Item.class, 2);
        System.out.println(item.getItemName());

        System.out.println(item.getCategories().size());

//		Category category = entityManager.find(Category.class, 3);
//		System.out.println(category.getCategoryName());
//		System.out.println(category.getItems().size());
    }

    /**
     * 多对多的保存
     * 8条insert
     * 两条Item 两条Category 4条中间表
     */
    @Test
    public void testManyToManyPersist() {
        Item i1 = new Item();
        i1.setItemName("i-1");

        Item i2 = new Item();
        i2.setItemName("i-2");

        Category c1 = new Category();
        c1.setCategoryName("C-1");

        Category c2 = new Category();
        c2.setCategoryName("C-2");

        //设置关联关系
        i1.getCategories().add(c1);
        i1.getCategories().add(c2);

        i2.getCategories().add(c1);
        i2.getCategories().add(c2);

        c1.getItems().add(i1);
        c1.getItems().add(i2);

        c2.getItems().add(i1);
        c2.getItems().add(i2);

        //执行保存
        entityManager.persist(i1);
        entityManager.persist(i2);
        entityManager.persist(c1);
        entityManager.persist(c2);
    }

    /**
     * 总之： @OneToOne 维护关联关系的一方推荐lazy，不维护的一方推荐eager
     *
     *
     * 默认情况下, 若获取不维护关联关系的一方, 则也会通过左外连接获取其关联的对象.
     * 可以通过 @OneToOne 的 fetch 属性来修改加载策略. 但依然会再发送 SQL 语句来初始化其关联的对象
     * 这说明在不维护关联关系的一方, 不建议修改 fetch 属性.
     *
     *
     * 查询不维护关系的一方
     *      不维护为：LAZY 维护为：EAGER，则先查询自己本身后，再去join 条件为外键
     *      不维护为：LAZY 维护为：LAZY，则先查询自己本身后，不join，条件为外键
     *
     *      不维护为：EAGER，无论维护是EAGER或LAZY 都是一次查询，join 条件为主键
     */
    @Test
    public void testOneToOneFind2() {
        Manager mgr = entityManager.find(Manager.class, 1);
        System.out.println(mgr.getMgrName());
        System.out.println(mgr.getDept().getClass().getName());
    }

    /**
     * 默认情况下, 若获取维护关联关系的一方, 则会通过左外连接获取其关联的对象.
     * 但可以通过 @OntToOne 的 fetch 属性来修改加载策略.
     *
     * 不是很懂  ： dept.getMgr().getClass().getName() 和 dept.getMgr() 的区别
     * 查询维护关系的一方
     *      维护为:LAZY 无论不维护是EAGER或LAZY  都会调用查询三次，前两次根据两张表的主键查询，第三次会根据维护关系方的外键查询
     *      维护为:EAGER 无论不维护是EAGER或LAZY 都会查询维护关系的一方，会查询两次，两次全是join 条件分别是维护关系方的主键和外键
     */
    @Test
    public void testOneToOneFind() {
        Department dept = entityManager.find(Department.class, 1);
        System.out.println(dept.getDeptName());
        System.out.println(dept.getMgr().getClass().getName());
    }

    /**
     * 双向 1-1 的关联关系, 建议先保存不维护关联关系的一方, 即没有外键的一方, 这样不会多出 UPDATE 语句.
     */
    @Test
    public void testOneToOnePersistence() {
        Manager mgr = new Manager();
        mgr.setMgrName("M-BB");

        Department dept = new Department();
        dept.setDeptName("D-BB");

        //设置关联关系
        //mgr.setDept(dept);
        dept.setMgr(mgr);

        //执行保存操作
        entityManager.persist(mgr);
        entityManager.persist(dept);
    }

    @Test
    public void testOneToManyUpdate() {
        Customer customer = entityManager.find(Customer.class, 9);

        customer.getOrders().iterator().next().setOrderName("O-XXX-10");
    }

    //默认情况下, 若删除 1 的一端, 则会先把关联的 n 的一端的外键置空, 然后进行删除.
    //可以通过 @OneToMany 的 cascade 属性来修改默认的删除策略.
    @Test
    public void testOneToManyRemove() {
        Customer customer = entityManager.find(Customer.class, 8);
        entityManager.remove(customer);
    }


    //默认对关联的多的一方使用懒加载的加载策略.
    //可以使用 @OneToMany 的 fetch 属性来修改默认的加载策略
    @Test
    public void testOneToManyFind() {
        Customer customer = entityManager.find(Customer.class, 9);
        System.out.println(customer.getLastName());

        System.out.println(customer.getOrders().size());
    }

    /**
     * 若是双向 1-n 的关联关系, 执行保存时，则会至少多出 n 条 UPDATE 语句
     * 若先保存 n 的一端, 再保存 1 的一端, 默认情况下, 会多出 n 条 UPDATE 语句， 一共有2n 条UPDATE 语句.
     * 若先保存 1 的一端, 则会有 n 条 UPDATE 语句
     *
     * 所以在进行双向 1-n 关联关系时, 建议使用 n 的一方来维护关联关系, 而 1 的一方不维护关联关系, 这样会有效的减少 SQL 语句. 只剩insert语句了.
     * 注意: 若在 1 的一端的 @OneToMany 中使用 mappedBy 属性, 则 @OneToMany 端就不能再使用 @JoinColumn 属性了.
     *
     *
     * 单向 1-n 关联关系执行保存时, 一定会多出 UPDATE 语句.
     * 因为 n 的一端不维护关联关系，所以在插入时不会同时插入外键列.只能额外的update
     *
     * 因为是由 1 的一端去维护关联关系，在保存 n 的一方的时不会去保存外键列的值，所以会导致多出 n 条 UPDATE 语句. 和先后保存顺序无关。
     */
    @Test
    public void testOneToManyPersist() {
        Customer customer = new Customer();
        customer.setAge(18);
        customer.setBirth(new Date());
        customer.setCreatedTime(new Date());
        customer.setEmail("mm@163.com");
        customer.setLastName("MM");

        Order order1 = new Order();
        order1.setOrderName("O-MM-1");

        Order order2 = new Order();
        order2.setOrderName("O-MM-2");

        //建立关联关系
        customer.getOrders().add(order1);
        customer.getOrders().add(order2);

        order1.setCustomer(customer);
        order2.setCustomer(customer);

        //执行保存操作
        entityManager.persist(customer);

        entityManager.persist(order1);
        entityManager.persist(order2);
    }

    /**
     * EAGER 直接left join查询出来修改
     * LAZY 分两次查询再修改
     */
    /*@Test
    public void testManyToOneUpdate(){
        Order order = entityManager.find(Order.class, 2);
        order.getCustomer().setLastName("FFF");
    }

    *//**
     * 不能直接删除 1 的一端, 因为有外键约束.
     *
     * 还有其他的Order 在 关联 Customer
     *//*
    @Test
    public void testManyToOneRemove(){
//		Order order = entityManager.find(Order.class, 1);
//		entityManager.remove(order);

        Customer customer = entityManager.find(Customer.class, 7);
        entityManager.remove(customer);
    }

    *//** 默认情况下, 使用左外连接的方式来获取 n 的一端的对象和其关联的 1 的一端的对象.
     *  可使用 @ManyToOne 的 fetch 属性来修改默认的关联属性的加载策略
     *
     *  多对一 查询多的一方时 直接查询出一的一方
     *  一对多 查询一的一方时 直接查询出多的一方
     *  多对多 互相查
     *//*
     @Test
    public void testManyToOneFind(){
        Order order = entityManager.find(Order.class, 1);
        System.out.println(order.getOrderName());

        System.out.println(order.getCustomer().getLastName());
    }

    *//**
     * 保存多对一时, 建议先保存 1 的一端, 后保存 n 的一端, 这样不会多出额外的 UPDATE 语句.
     * 外键还没生成，会导致保存后还要更新外键
     * 配置了@JoinColumn(name = "customer_id") 会自动生成外键列
     *//*
    @Test
    public void testManyToOnePersist(){
        Customer customer = new Customer();
        customer.setAge(18);
        customer.setBirth(new Date());
        customer.setCreatedTime(new Date());
        customer.setEmail("gg@163.com");
        customer.setLastName("GG");

        Order order1 = new Order();
        order1.setOrderName("G-GG-1");

        Order order2 = new Order();
        order2.setOrderName("G-GG-2");

        //设置关联关系
        order1.setCustomer(customer);
        order2.setCustomer(customer);

        //执行保存操作
        entityManager.persist(customer);
        entityManager.persist(order1);
        entityManager.persist(order2);
    }*/


    /**
     * 再次发送查询语句，刷新结果
     */
    @Test
    public void testRefresh() {
        Customer customer = entityManager.find(Customer.class, 1);
        customer = entityManager.find(Customer.class, 1);

        entityManager.refresh(customer);
    }

    /**
     * 如下：正常情况下，在commit时才会执行update语句，然后提交食物
     * flush语句可以将sql提前执行，但不会提交事务，最终还是要在commit时提交事务
     * <p>
     * 同 hibernate 中 Session 的 flush 方法.
     */
    @Test
    public void testFlush() {
        Customer customer = entityManager.find(Customer.class, 2);
        System.out.println(customer);

        customer.setLastName("AA");

        entityManager.flush();
    }

    //若传入的是一个游离对象, 即传入的对象有 OID.
    //1. 若在 EntityManager 缓存中有对应的对象
    //2. JPA 会把游离对象的属性复制到查询到EntityManager 缓存中的对象中.
    //3. EntityManager 缓存中的对象执行 UPDATE.
    @Test
    public void testMerge4() {
        Customer customer = new Customer();
        customer.setAge(18);
        customer.setBirth(new Date());
        customer.setCreatedTime(new Date());
        customer.setEmail("dd@163.com");
        customer.setLastName("DD");

        customer.setId(4);
        Customer customer2 = entityManager.find(Customer.class, 4);

        entityManager.merge(customer);

        System.out.println(customer == customer2); //false
    }


    //若传入的是一个游离对象, 即传入的对象有 OID.
    //1. 若在 EntityManager 缓存中没有该对象
    //2. 若在数据库中也有对应的记录
    //3. JPA 会查询对应的记录, 然后返回该记录对一个的对象, 再然后会把游离对象的属性复制到查询到的对象中.
    //4. 对查询到的对象执行 update 操作.
    @Test
    public void testMerge3() {
        Customer customer = new Customer();
        customer.setAge(18);
        customer.setBirth(new Date());
        customer.setCreatedTime(new Date());
        customer.setEmail("ee@163.com");
        customer.setLastName("EE");

        customer.setId(4);

        Customer customer2 = entityManager.merge(customer);

        System.out.println(customer == customer2); //false
    }

    //若传入的是一个游离对象, 即传入的对象有 OID.
    //1. 若在 EntityManager 缓存中没有该对象
    //2. 若在数据库中也没有对应的记录
    //3. JPA 会创建一个新的对象, 然后把当前游离对象的属性复制到新创建的对象中
    //4. 对新创建的对象执行 insert 操作.
    @Test
    public void testMerge2() {
        Customer customer = new Customer();
        customer.setAge(18);
        customer.setBirth(new Date());
        customer.setCreatedTime(new Date());
        customer.setEmail("dd@163.com");
        customer.setLastName("DD");

        customer.setId(100);

        Customer customer2 = entityManager.merge(customer);

        System.out.println("customer#id:" + customer.getId());
        System.out.println("customer2#id:" + customer2.getId());
    }

    /**
     * 总的来说: 类似于 hibernate Session 的 saveOrUpdate 方法.
     */
    //1. 若传入的是一个临时对象
    //会创建一个新的对象, 把临时对象的属性复制到新的对象中, 然后对新的对象执行持久化操作. 所以
    //新的对象中有 id, 但以前的临时对象中没有 id.
    @Test
    public void merge() {
        Customer customer = new Customer();
        customer.setAge(18);
        customer.setBirth(new Date());
        customer.setCreatedTime(new Date());
        customer.setEmail("cc@163.com");
        customer.setLastName("CC");

        Customer customer2 = entityManager.merge(customer);

        System.out.println("customer#id:" + customer.getId());
        System.out.println("customer2#id:" + customer2.getId());
    }


    //类似于 hibernate 中 Session 的 delete 方法. 把对象对应的记录从数据库中移除
    //但注意: 该方法只能移除 持久化 对象. 而 hibernate 的 delete 方法实际上还可以移除 游离对象.
    @Test
    public void testRemove() {
        Customer customer = entityManager.find(Customer.class, 1);
        System.out.println(customer);
        entityManager.remove(customer);
    }

    //类似于 hibernate 的 save 方法. 使对象由临时状态变为持久化状态.
    //和 hibernate 的 save 方法的不同之处: 若对象有 id, 则不能执行 insert 操作, 而会抛出异常.
    @Test
    public void testPersist() {
        Customer customer = new Customer();
        customer.setAge(10);
        customer.setBirth(new Date());
        customer.setCreatedTime(new Date());
        customer.setEmail("333@qq.com");
        customer.setLastName("pp");

        Customer customer1 = new Customer();
        customer1.setAge(11);
        customer1.setBirth(new Date());
        customer1.setCreatedTime(new Date());
        customer1.setEmail("444@qq.com");
        customer1.setLastName("qq");

        entityManager.persist(customer);
        entityManager.persist(customer1);
        System.out.println(customer.getId());
        System.out.println(customer1.getId());
    }

    //类似于 hibernate 中 Session 的 load 方法
    @Test
    public void testGetReference() {
        Customer customer = entityManager.getReference(Customer.class, 1);
        System.out.println(customer.getClass().getName());
    }

    //类似于 hibernate 中 Session 的 get 方法.
    @Test
    public void testFind() {
        Customer customer = entityManager.find(Customer.class, 1);
        System.out.println(customer);
    }


}
