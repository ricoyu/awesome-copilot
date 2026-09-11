package com.awesomecopilot.orm.it;

import com.awesomecopilot.orm.dao.JpaDao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * copilot-orm 集成测试基类。
 * <p>
 * 用真实 H2 内存数据库 + Hibernate 6, 不走 Spring 容器, 直接反射注入
 * {@link JpaDao} 的 EntityManagerFactory 字段。这样能覆盖:
 * <ul>
 *   <li>无 Spring 事务时自建 EntityManager 并在终端操作后释放(之前 P0-5 修复的路径)</li>
 *   <li>手动 begin()/commit()/rollback() 事务</li>
 *   <li>实体 CRUD、Criteria 查询、原生 SQL 查询、命名查询、分页、排序、类型转换</li>
 * </ul>
 * 每个测试方法开始前清空 book 表并插入一组固定数据, 方法结束回滚/关闭事务。
 *
 * @author Rico Yu
 */
public abstract class AbstractOrmIntegrationTest {

	protected static EntityManagerFactory emf;

	protected JpaDao jpaDao;

	/** 数据准备用, 绕开被测的 JpaDao 方法, 保证数据准备本身不依赖被测逻辑 */
	protected EntityManager seedEm;

	@BeforeAll
	static void initEmf() {
		emf = Persistence.createEntityManagerFactory("orm-test");
	}

	@BeforeEach
	void setupDao() {
		jpaDao = new JpaDao();
		inject(jpaDao, "entityManagerFactory", emf);
		// 逻辑删除默认关闭, 避免影响不相关测试
		inject(jpaDao, "logicalDeleteEnabled", false);
		// SQL 自动修复保持默认 true, 与生产配置一致
		seedEm = emf.createEntityManager();
		// 清空表并立即提交, 让后续自建 EM 的新连接能看到干净状态;
		// 旧写法只 begin 不 commit, delete 在未提交事务里对其他连接不可见, 造成跨测试数据残留
		seedEm.getTransaction().begin();
		seedEm.createNativeQuery("delete from book").executeUpdate();
		seedEm.getTransaction().commit();
	}

	@AfterEach
	void teardownDao() {
		if (seedEm != null && seedEm.getTransaction().isActive()) {
			seedEm.getTransaction().rollback();
		}
		if (seedEm != null && seedEm.isOpen()) {
			seedEm.close();
		}
		// 防止某个测试手动 begin 后没 commit, 残留活跃事务
		jpaDao.cleanupEntityManager();
	}

	/** 反射注入 private 字段 */
	protected static void inject(Object target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (Exception e) {
			throw new IllegalStateException("注入字段失败: " + fieldName, e);
		}
	}

	/** 通过 seedEm 插入一条 book 数据, 要求 seedEm 已处于 begin 的事务中 */
	protected void insertBook(String name, String author, String price, int stock,
	                          boolean published, LocalDateTime publishDate, String status) {
		seedEm.createNativeQuery("insert into book (name, author, price, stock, published, publish_date, status, "
				+ "create_time, update_time) values (?,?,?,?,?,?,?,?,?)")
				.setParameter(1, name)
				.setParameter(2, author)
				.setParameter(3, price)
				.setParameter(4, stock)
				.setParameter(5, published)
				.setParameter(6, publishDate)
				.setParameter(7, status)
				.setParameter(8, LocalDateTime.now())
				.setParameter(9, LocalDateTime.now())
				.executeUpdate();
	}

	/** 造一批标准测试数据(开启独立事务, 结束时提交) */
	protected void seedStandardBooks() {
		seedEm.getTransaction().begin();
		insertBook("Java编程思想", "Bruce", "98.50", 100, true,
				LocalDateTime.of(2020, 1, 1, 10, 0), "PUBLISHED");
		insertBook("深入理解JVM", "周志明", "88.00", 50, true,
				LocalDateTime.of(2021, 3, 15, 9, 0), "PUBLISHED");
		insertBook("Java并发实战", "Brian", "79.00", 0, false,
				LocalDateTime.of(2022, 5, 20, 8, 0), "DRAFT");
		insertBook("Effective Java", "Joshua", "108.00", 30, true,
				LocalDateTime.of(2023, 7, 1, 12, 0), "PUBLISHED");
		insertBook("Clean Code", "Robert", "66.00", 200, true,
				LocalDateTime.of(2024, 9, 9, 14, 0), "ARCHIVED");
		seedEm.getTransaction().commit();
	}
}
