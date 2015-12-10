/**
 * Copyright (c) 2013-2015, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.liquibase.internal;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.sql.DataSource;

import org.apache.commons.configuration.Configuration;
import org.seedstack.jdbc.internal.JdbcRegistry;
import org.seedstack.jpa.internal.JpaPlugin;
import org.seedstack.seed.SeedException;
import org.seedstack.seed.core.internal.application.ApplicationPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nuun.kernel.api.plugin.InitState;
import io.nuun.kernel.api.plugin.context.InitContext;
import io.nuun.kernel.core.AbstractPlugin;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;

/**
 * Plugin to alter/create a database with {@link Liquibase}.
 * 
 * @author thierry.bouvet@mpsa.com
 *
 */
public class LiquibasePlugin extends AbstractPlugin {

	private static final String CONTEXTS_CONFIG = "contexts";
	private static final String FAIL_ON_ERROR_CONFIG = "failOnError";
	private static final String CHANGELOG_CONFIG = "changelog";
	private static final String DATASOURCE_CONFIG = "datasource";
	private static final String JPA_PLUGIN = "org.seedstack.jpa.internal.JpaPlugin";
	private static final Logger LOGGER = LoggerFactory.getLogger(LiquibasePlugin.class);

	@Override
	public String name() {
		return "seed-liquibase-plugin";
	}

	@Override
	public InitState init(InitContext initContext) {
		Configuration configuration = initContext.dependency(ApplicationPlugin.class).getConfiguration();
		Configuration liquibaseConfiguration = configuration.subset("org.seedstack.liquibase");
		for (String changeset : liquibaseConfiguration.getProperty("changesets").toString().split(",")) {
			Configuration changeSetConfiguration = liquibaseConfiguration.subset("changeset." + changeset);

			try {
				applyChangeLog(changeSetConfiguration, initContext.dependency(JdbcRegistry.class));
			} catch (Exception e) {
				String ds = changeSetConfiguration.getString(DATASOURCE_CONFIG);
				String changelog = changeSetConfiguration.getString(CHANGELOG_CONFIG);
				if (changeSetConfiguration.getBoolean(FAIL_ON_ERROR_CONFIG, Boolean.TRUE)) {
					throw SeedException.wrap(e, LiquibaseErrorCode.ERROR_APPLY_CHANGES).put(CHANGELOG_CONFIG, changelog)
					        .put(DATASOURCE_CONFIG, ds);
				} else {
					LOGGER.warn("Changes [{}] not fully applied to the datasource [{}].", changelog, ds);
				}
			}
		}

		return InitState.INITIALIZED;
	}

	/**
	 * @param configuration
	 *            Liquibase {@link Configuration}
	 * @param jdbcRegistry
	 *            {@link JdbcRegistry} to get a {@link DataSource}.
	 * @throws Exception
	 */
	private void applyChangeLog(Configuration configuration, JdbcRegistry jdbcRegistry) throws Exception {
		String ds = configuration.getString(DATASOURCE_CONFIG);
		String changelog = configuration.getString(CHANGELOG_CONFIG);
		String contexts = configuration.getString(CONTEXTS_CONFIG);
		LOGGER.info("Apply changelog [{}] to the datasource [{}]", changelog, ds);
		Connection connection = null;
		Database database = null;
		try {
			connection = jdbcRegistry.getDataSource(ds).getConnection();
			connection.setAutoCommit(false);
			database = createDatabase(connection);
			Thread currentThread = Thread.currentThread();
			ClassLoader contextClassLoader = currentThread.getContextClassLoader();
			ResourceAccessor threadClFO = new ClassLoaderResourceAccessor(contextClassLoader);

			ResourceAccessor clFO = new ClassLoaderResourceAccessor();
			ResourceAccessor fsFO = new FileSystemResourceAccessor();
			Liquibase liquibase = new Liquibase(changelog, new CompositeResourceAccessor(clFO, fsFO, threadClFO),
			        database);

			try {
				liquibase.update(new Contexts(contexts));
				connection.commit();
			} catch (LiquibaseException e) {
				connection.rollback();
				throw e;
			}
		} finally {
			if (database != null) {
				database.close();
			} else if (connection != null) {
				connection.close();
			}
		}
	}

	/**
	 * Create a Liquibase {@link Database} object.
	 * 
	 * @param c
	 * @return a Database implementation retrieved from the
	 *         {@link DatabaseFactory}.
	 * @throws DatabaseException
	 */
	private Database createDatabase(Connection connection) throws DatabaseException {

		DatabaseConnection liquibaseConnection = new JdbcConnection(connection);
		Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(liquibaseConnection);
		return database;
	}

	@Override
	public Collection<Class<?>> requiredPlugins() {
		Collection<Class<?>> list = new ArrayList<Class<?>>();
		list.add(JdbcRegistry.class);
		list.add(ApplicationPlugin.class);
		return list;
	}

	@Override
	public Collection<Class<?>> dependentPlugins() {
		if (isAvailable(JPA_PLUGIN)) {
			Collection<Class<?>> list = new ArrayList<Class<?>>();
			list.add(JpaPlugin.class);
			return list;
		}
		return Collections.emptySet();
	}

	/**
	 * Check if a class exists in the classpath
	 * 
	 * @param dependency
	 *            class to look for.
	 * @return true if class exists in the classpath
	 */
	private boolean isAvailable(String dependency) {
		try {
			Class.forName(dependency);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

}
