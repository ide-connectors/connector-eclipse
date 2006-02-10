/*******************************************************************************
 * Copyright (c) 2006 - 2006 Mylar eclipse.org project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mylar project committers - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.internal.jira;

import org.eclipse.mylar.internal.tasklist.AbstractRepositoryTask;
import org.eclipse.mylar.internal.tasklist.ui.TaskListImages;
import org.eclipse.swt.graphics.Image;
import org.tigris.jira.core.model.Priority;

/**
 * @author Wesley Coelho (initial integration patch)
 * @author Mik Kersten
 */
public class JiraTask extends AbstractRepositoryTask {

	public enum PriorityLevel {
		BLOCKER, CRITICAL, MAJOR, MINOR, TRIVIAL;

		@Override
		public String toString() {
			switch (this) {
			case BLOCKER:
				return "P1";
			case CRITICAL:
				return "P2";
			case MAJOR:
				return "P3";
			case MINOR:
				return "P4";
			case TRIVIAL:
				return "P5";
			default:
				return "P5";
			}
		}

		public static PriorityLevel fromPriority(Priority priority) {
			if (priority == null) {
				return null;
			}
			String priorityId = priority.getId();
			if (priorityId == null)
				return null;
			if (priorityId.equals("1"))
				return BLOCKER;
			if (priorityId.equals("2"))
				return CRITICAL;
			if (priorityId.equals("3"))
				return MAJOR;
			if (priorityId.equals("4"))
				return MINOR;
			if (priorityId.equals("5"))
				return TRIVIAL;
			return null;
		}
	}
	
	/**
	 * The handle is also the task's Jira url
	 */
	public JiraTask(String handle, String label, boolean newTask) {
		super(handle, label, newTask);
		setUrl(handle);
	}

	public Image getIcon() {
		return TaskListImages.getImage(TaskListImages.TASK_WEB);
	}

	/** Priorities are not yet implemented */
	public String getPriority() {
		return "";
	}
}
