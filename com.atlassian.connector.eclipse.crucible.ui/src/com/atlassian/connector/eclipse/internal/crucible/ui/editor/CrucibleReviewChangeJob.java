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

package com.atlassian.connector.eclipse.internal.crucible.ui.editor;

import com.atlassian.connector.eclipse.internal.crucible.core.CrucibleCorePlugin;
import com.atlassian.connector.eclipse.internal.crucible.core.CrucibleRepositoryConnector;
import com.atlassian.connector.eclipse.internal.crucible.core.client.CrucibleClient;
import com.atlassian.connector.eclipse.internal.crucible.ui.CrucibleUiPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.mylyn.tasks.core.TaskRepository;

/**
 * Job to submit or retrieve changes for a review
 * 
 * @author Shawn Minto
 */
public abstract class CrucibleReviewChangeJob extends Job {

	private IStatus status;

	private final TaskRepository taskRepository;

	protected CrucibleReviewChangeJob(String name, TaskRepository taskRepository) {
		super(name);
		this.taskRepository = taskRepository;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		setStatus(Status.CANCEL_STATUS);
		CrucibleRepositoryConnector connector = CrucibleCorePlugin.getRepositoryConnector();
		CrucibleClient client = connector.getClientManager().getClient(taskRepository);
		if (client == null) {
			return new Status(IStatus.ERROR, CrucibleUiPlugin.PLUGIN_ID, "Unable to get client, please try to refresh");
		}
		try {
			IStatus result = execute(client, monitor);
			setStatus(result);
		} catch (CoreException e) {
			setStatus(e.getStatus());
		}
		return Status.OK_STATUS;
	}

	protected abstract IStatus execute(CrucibleClient client, IProgressMonitor monitor) throws CoreException;

	public IStatus getStatus() {
		return status;
	}

	protected void setStatus(IStatus status) {
		this.status = status;
	}

}