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

package com.atlassian.connector.eclipse.internal.crucible.ui;

import com.atlassian.connector.eclipse.internal.crucible.core.CrucibleCorePlugin;
import com.atlassian.connector.eclipse.internal.crucible.core.CrucibleUtil;
import com.atlassian.theplugin.commons.crucible.api.model.Review;
import com.atlassian.theplugin.commons.crucible.api.model.ReviewBean;

import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;

/**
 * Utility class for the UI
 * 
 * @author Shawn Minto
 */
public final class CrucibleUiUtil {

	private CrucibleUiUtil() {
		// ignore
	}

	public static TaskRepository getCrucibleTaskRepository(String repositoryUrl) {
		if (repositoryUrl != null) {
			return TasksUi.getRepositoryManager().getRepository(CrucibleCorePlugin.CONNECTOR_KIND, repositoryUrl);
		}
		return null;
	}

	public static ITask getCrucibleTask(TaskRepository taskRepository, String taskId) {
		if (taskRepository != null && taskId != null) {
			return TasksUi.getRepositoryModel().getTask(taskRepository, taskId);
		}
		return null;
	}

	public static TaskRepository getCrucibleTaskRepository(Review review) {
		if (review != null && review instanceof ReviewBean) {
			String repositoryUrl = ((ReviewBean) review).getServerUrl();
			if (repositoryUrl != null) {
				return getCrucibleTaskRepository(repositoryUrl);
			}
		}
		return null;
	}

	public static ITask getCrucibleTask(Review review) {
		if (review != null) {
			TaskRepository taskRepository = getCrucibleTaskRepository(review);
			String taskId = CrucibleUtil.getTaskIdFromPermId(review.getPermId().getId());
			if (taskRepository != null && taskId != null) {
				return getCrucibleTask(taskRepository, taskId);
			}
		}

		return null;
	}

}