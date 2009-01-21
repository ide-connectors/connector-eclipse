/*******************************************************************************
 * Copyright (c) 2009 Atlassian and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atlassian - initial API and implementation
 ******************************************************************************/

package com.atlassian.connector.eclipse.internal.crucible.core;

/**
 * This is a class to encapsulate all of the constants used in the crucible connector
 * 
 * @author Shawn Minto
 */
public final class CrucibleConstants {

	private CrucibleConstants() {
		// ignore
	}

	public static final String CLASSIFICATION_CUSTOM_FIELD_KEY = "classification";

	public static final String CRUCIBLE_URL_START = "cru/";

	public static final String CUSTOM_FILER_START = CRUCIBLE_URL_START + "?filter=custom&";

	public static final String KEY_FILTER_ID = "FilterId";

	public static final String PREDEFINED_FILER_START = CRUCIBLE_URL_START + "?filter=";

	public static final String RANK_CUSTOM_FIELD_KEY = "rank";

	public static final String HAS_CHANGED_TASKDATA_KEY = "hasChanged";

}