package org.seedstack.liquibase.internal;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.seedstack.jpa.JpaUnit;
import org.seedstack.seed.it.SeedITRunner;
import org.seedstack.seed.transaction.Transactional;

@RunWith(SeedITRunner.class)
public class SeedLiquibaseTest {

	@Inject
	private EntityManager entityManager;

	@Test
	@Transactional
	@JpaUnit("my-jpa-unit")
	public void test() throws Exception {
		Query query = entityManager.createQuery("select count(*) from Department");
		Assertions.assertThat((Long) query.getSingleResult()).isEqualTo(0);
	}

}
