/**
 * Copyright (c) 2013-2015, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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

import liquibase.Liquibase;

/**
 * Test a database creation with {@link Liquibase} change logs.
 * 
 * @author thierry.bouvet@mpsa.com
 *
 */
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
