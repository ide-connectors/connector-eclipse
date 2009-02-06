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

package com.atlassian.connector.eclipse.internal.crucible.ui.dialogs;

import com.atlassian.connector.eclipse.internal.crucible.core.CrucibleCorePlugin;
import com.atlassian.connector.eclipse.internal.crucible.core.client.CrucibleClient;
import com.atlassian.connector.eclipse.internal.crucible.ui.CrucibleUiPlugin;
import com.atlassian.connector.eclipse.internal.crucible.ui.operations.AddCommentRemoteOperation;
import com.atlassian.connector.eclipse.ui.team.CrucibleFile;
import com.atlassian.theplugin.commons.crucible.api.model.Comment;
import com.atlassian.theplugin.commons.crucible.api.model.CustomField;
import com.atlassian.theplugin.commons.crucible.api.model.CustomFieldBean;
import com.atlassian.theplugin.commons.crucible.api.model.CustomFieldDef;
import com.atlassian.theplugin.commons.crucible.api.model.CustomFieldValue;
import com.atlassian.theplugin.commons.crucible.api.model.Review;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.source.LineRange;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dialog shown to the user when they add a comment to a review
 * 
 * @author Thomas Ehrnhoefer
 * @author Shawn Minto
 */
public class CrucibleAddCommentDialog extends ProgressDialog {

	public class AddCommentRunnable implements IRunnableWithProgress {

		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			try {
				monitor.beginTask("Adding comment", IProgressMonitor.UNKNOWN);
				if (newComment.length() > 0) {

					AddCommentRemoteOperation operation = new AddCommentRemoteOperation(monitor, review, client,
							crucibleFile, newComment);
					operation.setDefect(defect);
					operation.setDraft(draft);
					operation.setCustomFields(customFieldSelections);
					operation.setCommentLines(commentLines);
					operation.setParentComment(parentComment);

					try {
						client.execute(operation);
					} catch (CoreException e) {
						StatusHandler.log(new Status(IStatus.ERROR, CrucibleUiPlugin.PLUGIN_ID,
								"Unable to post Comment", e));
					}
					client.getReview(getTaskRepository(), getTaskId(), true, monitor);
				}
			} catch (CoreException e) {
				throw new InvocationTargetException(e);

			}

		}
	}

	private final Review review;

	private final Comment replyToComment;

	private final String shellTitle;

	private final TaskRepository taskRepository;

	private final String taskKey;

	private final String taskId;

	private final CrucibleClient client;

	private final LineRange commentLines;

	private final Comment parentComment;

	private final CrucibleFile crucibleFile;

	private static final String SAVE_LABEL = "&Save";

	private static final String DRAFT_LABEL = "Save as &Draft";

	private static final String DEFECT_LABEL = "Defect";

	private final boolean edit = false;

	private final HashMap<CustomFieldDef, ComboViewer> customCombos;

	private final HashMap<String, CustomField> customFieldSelections;

	private boolean draft = false;

	private boolean defect = false;

	private Text commentText;

	private String newComment;

	private Button defectButton;

	public CrucibleAddCommentDialog(Shell parentShell, String shellTitle, Review review, CrucibleFile file,
			Comment replyToComment, LineRange lineRange, String taskKey, String taskId, TaskRepository taskRepository,
			CrucibleClient client) {
		super(parentShell);
		this.crucibleFile = file;
		this.parentComment = replyToComment;
		this.commentLines = lineRange;
		this.shellTitle = shellTitle;
		this.review = review;
		this.replyToComment = replyToComment;
		this.taskKey = taskKey;
		this.taskId = taskId;
		this.taskRepository = taskRepository;
		this.client = client;
		customCombos = new HashMap<CustomFieldDef, ComboViewer>();
		customFieldSelections = new HashMap<String, CustomField>();
	}

	@Override
	protected Control createPageControls(Composite parent) {
		//CHECKSTYLE:MAGIC:OFF
		getShell().setText(shellTitle);
		setTitle(shellTitle);

		if (replyToComment == null) {
			setMessage("Create a new comment");
		} else {
			setMessage("Reply to a comment from: " + replyToComment.getAuthor().getDisplayName());
		}

		//CHECKSTYLE:MAGIC:OFF
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		commentText = new Text(composite, SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		GridData textGridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL
				| GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_FILL);
		textGridData.heightHint = 80;
		commentText.setLayoutData(textGridData);

		//CHECKSTYLE:MAGIC:OFF
		((GridLayout) parent.getLayout()).makeColumnsEqualWidth = false;
		// create buttons according to (implicit) reply type
		if (replyToComment == null) { //"defect button" needed if new comment
			composite = new Composite(composite, SWT.NONE);
			composite.setLayout(new GridLayout(1, false));
			GridDataFactory.fillDefaults().grab(true, true).align(SWT.RIGHT, SWT.CENTER).span(3, 1).applyTo(composite);
			createDefectButton(composite);
			addCustomFields(composite);
		}

		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, SWT.DEFAULT).applyTo(composite);
		return composite;
		//CHECKSTYLE:MAGIC:ON
	}

	@Override
	protected Collection<? extends Control> getDisableableControls() {
		Set<Control> controls = new HashSet<Control>(super.getDisableableControls());
		if (customCombos.size() > 0) {
			for (ComboViewer viewer : customCombos.values()) {
				controls.add(viewer.getControl());
			}
		}

		if (defectButton != null) {
			controls.add(defectButton);
		}

		return controls;
	}

	protected void processFields() {
		newComment = commentText.getText();
		if (defect) { //process custom field selection only when defect is selected
			for (CustomFieldDef field : customCombos.keySet()) {
				CustomFieldValue customValue = (CustomFieldValue) customCombos.get(field).getElementAt(
						customCombos.get(field).getCombo().getSelectionIndex());
				if (customValue != null) {
					CustomFieldBean bean = new CustomFieldBean();
					bean.setConfigVersion(field.getConfigVersion());
					bean.setValue(customValue.getName());
					customFieldSelections.put(field.getName(), bean);
				}
			}
		}
	}

	private void addCustomFields(Composite parent) {
		if (review == null) {
			return;
		}
		List<CustomFieldDef> customFields = CrucibleCorePlugin.getDefault().getReviewCache().getMetrics(
				review.getMetricsVersion());
		if (customFields == null) {
			StatusHandler.log(new Status(IStatus.ERROR, CrucibleCorePlugin.PLUGIN_ID,
					"Metrics are for review version are not cached: " + review.getMetricsVersion() + " "
							+ review.getName(), new Exception()));
		} else {
			for (CustomFieldDef customField : customFields) {
				createCombo(parent, customField, 0);
			}
		}
	}

	protected Button createDefectButton(Composite parent) {
		// increment the number of columns in the button bar
		((GridLayout) parent.getLayout()).numColumns++;
		defectButton = new Button(parent, SWT.CHECK);
		defectButton.setText(DEFECT_LABEL);
		defectButton.setFont(JFaceResources.getDialogFont());
		defectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				defect = !defect;
				//toggle combos
				for (CustomFieldDef field : customCombos.keySet()) {
					customCombos.get(field).getCombo().setEnabled(defect);
				}
			}
		});
		return defectButton;
	}

	protected void createCombo(Composite parent, final CustomFieldDef customField, int selection) {
		((GridLayout) parent.getLayout()).numColumns++;
		Label label = new Label(parent, SWT.NONE);
		label.setText("Select " + customField.getName());
		((GridLayout) parent.getLayout()).numColumns++;
		ComboViewer comboViewer = new ComboViewer(parent);
		comboViewer.setContentProvider(new ArrayContentProvider());
		comboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				CustomFieldValue fieldValue = (CustomFieldValue) element;
				return fieldValue.getName();
			}
		});
		comboViewer.setInput(customField.getValues());
		comboViewer.getCombo().setEnabled(false);
		customCombos.put(customField, comboViewer);
	}

	public void addComment() {

		try {
			newComment = commentText.getText();
			processFields();
			setMessage("");
			run(true, false, new AddCommentRunnable());
		} catch (InvocationTargetException e) {
			StatusHandler.log(new Status(IStatus.ERROR, CrucibleUiPlugin.PLUGIN_ID, e.getMessage(), e));
			setErrorMessage("Unable to summarize the review");
			return;
		} catch (InterruptedException e) {
			StatusHandler.log(new Status(IStatus.ERROR, CrucibleUiPlugin.PLUGIN_ID, e.getMessage(), e));
			setErrorMessage("Unable to summarize the review");
			return;
		}

		setReturnCode(Window.OK);
		close();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {

		createButton(parent, IDialogConstants.CLIENT_ID + 2, SAVE_LABEL, false).addSelectionListener(
				new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						addComment();
					}
				});
		if (!edit) { //if it is a new reply, saving as draft is possible
			createButton(parent, IDialogConstants.CLIENT_ID + 2, DRAFT_LABEL, false).addSelectionListener(
					new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							draft = true;
							addComment();
						}
					});
		}
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false).addSelectionListener(
				new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						cancelPressed();
					}
				});
	}

	public void cancelAddComment() {
		setReturnCode(Window.CANCEL);
		close();
	}

	public String getTaskKey() {
		return taskKey;
	}

	public String getTaskId() {
		return taskId;
	}

	public TaskRepository getTaskRepository() {
		return taskRepository;
	}
}