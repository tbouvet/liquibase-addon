/**
 * Copyright (c) 2013-2015, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.liquibase.internal;

import org.seedstack.seed.ErrorCode;

/**
 * {@link ErrorCode} when create/alter a database.
 * 
 * @author thierry.bouvet@mpsa.com
 *
 */
public enum LiquibaseErrorCode implements ErrorCode {
	ERROR_APPLY_CHANGES
}
