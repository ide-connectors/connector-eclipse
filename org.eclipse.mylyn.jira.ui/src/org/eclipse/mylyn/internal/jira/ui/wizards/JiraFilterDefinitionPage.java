/*******************************************************************************
 * Copyright (c) 2004, 2009 Brock Janiczak and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Brock Janiczak - initial API and implementation
 *     Eugene Kuleshov - improvements
 *     Tasktop Technologies - improvements
 *******************************************************************************/

package org.eclipse.mylyn.internal.jira.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.jira.core.JiraClientFactory;
import org.eclipse.mylyn.internal.jira.core.model.Component;
import org.eclipse.mylyn.internal.jira.core.model.IssueType;
import org.eclipse.mylyn.internal.jira.core.model.JiraStatus;
import org.eclipse.mylyn.internal.jira.core.model.Priority;
import org.eclipse.mylyn.internal.jira.core.model.Project;
import org.eclipse.mylyn.internal.jira.core.model.Resolution;
import org.eclipse.mylyn.internal.jira.core.model.Version;
import org.eclipse.mylyn.internal.jira.core.model.filter.ComponentFilter;
import org.eclipse.mylyn.internal.jira.core.model.filter.ContentFilter;
import org.eclipse.mylyn.internal.jira.core.model.filter.CurrentUserFilter;
import org.eclipse.mylyn.internal.jira.core.model.filter.DateFilter;
import org.eclipse.mylyn.internal.jira.core.model.filter.DateRangeFilter;
import org.eclipse.mylyn.internal.jira.core.model.filter.FilterDefinition;
import org.eclipse.mylyn.internal.jira.core.model.filter.IssueTypeFilter;
import org.eclipse.mylyn.internal.jira.core.model.filter.NobodyFilter;
import org.eclipse.mylyn.internal.jira.core.model.filter.PriorityFilter;
import org.eclipse.mylyn.internal.jira.core.model.filter.ProjectFilter;
import org.eclipse.mylyn.internal.jira.core.model.filter.ResolutionFilter;
import org.eclipse.mylyn.internal.jira.core.model.filter.SpecificUserFilter;
import org.eclipse.mylyn.internal.jira.core.model.filter.StatusFilter;
import org.eclipse.mylyn.internal.jira.core.model.filter.UserFilter;
import org.eclipse.mylyn.internal.jira.core.model.filter.UserInGroupFilter;
import org.eclipse.mylyn.internal.jira.core.model.filter.VersionFilter;
import org.eclipse.mylyn.internal.jira.core.service.FilterDefinitionConverter;
import org.eclipse.mylyn.internal.jira.core.service.JiraClient;
import org.eclipse.mylyn.internal.jira.core.service.JiraException;
import org.eclipse.mylyn.internal.jira.core.util.JiraUtil;
import org.eclipse.mylyn.internal.jira.ui.JiraUiPlugin;
import org.eclipse.mylyn.internal.provisional.commons.ui.DatePicker;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

/**
 * @author Brock Janiczak
 * @author Eugene Kuleshov (layout and other improvements)
 * @author Mik Kersten (generalized for search dialog)
 * @author Steffen Pingel
 * @author Thomas Ehrnhoefer (multiple projects selection)
 */
public class JiraFilterDefinitionPage extends AbstractRepositoryQueryPage {

	final static class ComponentLabelProvider implements ILabelProvider {

		public void addListener(ILabelProviderListener listener) {
		}

		public void dispose() {
		}

		public Image getImage(Object element) {
			return null;
		}

		public String getText(Object element) {
			if (element instanceof Placeholder) {
				return ((Placeholder) element).getText();
			}
			return ((Component) element).getName();
		}

		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		public void removeListener(ILabelProviderListener listener) {
		}

	}

	private static final class Placeholder {
		private final String text;

		public Placeholder(String text) {
			this.text = text;
		}

		public String getText() {
			return this.text;
		}
	}

	final static class VersionLabelProvider implements ILabelProvider, IColorProvider {

		public void addListener(ILabelProviderListener listener) {

		}

		public void dispose() {
		}

		public Color getBackground(Object element) {
			if (element instanceof Placeholder) {
				return Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
			}
			return null;
		}

		public Color getForeground(Object element) {
			return null;
		}

		public Image getImage(Object element) {
			return null;
		}

		public String getText(Object element) {
			if (element instanceof Placeholder) {
				return ((Placeholder) element).getText();
			}
			return ((Version) element).getName();
		}

		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		public void removeListener(ILabelProviderListener listener) {
		}

	}

	private static final String PAGE_NAME = "Jira" + "SearchPage"; //$NON-NLS-1$ //$NON-NLS-2$

	private static final String SEARCH_URL_ID = PAGE_NAME + ".SEARCHURL"; //$NON-NLS-1$

	private static final Project[] NO_PROJECTS = new Project[0];

	final Placeholder ALL_PROJECTS = new Placeholder(Messages.JiraFilterDefinitionPage_All_Projects);

	final Placeholder ANY_ASSIGNEE = new Placeholder(Messages.JiraFilterDefinitionPage_Any);

	final Placeholder ANY_COMPONENT = new Placeholder(Messages.JiraFilterDefinitionPage_Any);

	final Placeholder ANY_FIX_VERSION = new Placeholder(Messages.JiraFilterDefinitionPage_Any);

	final Placeholder ANY_ISSUE_TYPE = new Placeholder(Messages.JiraFilterDefinitionPage_Any);

	final Placeholder ANY_PRIORITY = new Placeholder(Messages.JiraFilterDefinitionPage_Any);

	// attributes

	final Placeholder ANY_REPORTED_VERSION = new Placeholder(Messages.JiraFilterDefinitionPage_Any);

	final Placeholder ANY_REPORTER = new Placeholder(Messages.JiraFilterDefinitionPage_Any);

	final Placeholder ANY_RESOLUTION = new Placeholder(Messages.JiraFilterDefinitionPage_Any);

	final Placeholder ANY_STATUS = new Placeholder(Messages.JiraFilterDefinitionPage_Any);

	private Text assignee;

	private ComboViewer assigneeType;

	private final JiraClient client;

	private ListViewer components;

	private DatePicker createdEndDatePicker;

	private DatePicker createdStartDatePicker;

	final Placeholder CURRENT_USER_ASSIGNEE = new Placeholder(Messages.JiraFilterDefinitionPage_Current_User);

	final Placeholder CURRENT_USER_REPORTER = new Placeholder(Messages.JiraFilterDefinitionPage_Current_User);

	private DatePicker dueEndDatePicker;

	private DatePicker dueStartDatePicker;

	private boolean firstTime = true;

	private ListViewer fixFor;

	private ListViewer issueType;

	final Placeholder NO_COMPONENT = new Placeholder(Messages.JiraFilterDefinitionPage_No_Component);

	final Placeholder NO_FIX_VERSION = new Placeholder(Messages.JiraFilterDefinitionPage_No_Fix_Version);

	final Placeholder NO_PROJECT = new Placeholder(Messages.JiraFilterDefinitionPage_No_Project);

	final Placeholder NO_REPORTED_VERSION = new Placeholder(Messages.JiraFilterDefinitionPage_No_Version);

	final Placeholder NO_REPORTER = new Placeholder(Messages.JiraFilterDefinitionPage_No_Reporter);

	private ListViewer priority;

	private ListViewer project;

	private Text queryString;

	final Placeholder RELEASED_VERSION = new Placeholder(Messages.JiraFilterDefinitionPage_Released_Versions);

	private ListViewer reportedIn;

	private Text reporter;

	private ComboViewer reporterType;

	private ListViewer resolution;

	private Button searchComments;

	private Button searchDescription;

	private Button searchEnvironment;

	private Button searchSummary;

	final Placeholder SPECIFIC_GROUP_ASSIGNEE = new Placeholder(Messages.JiraFilterDefinitionPage_Specified_Group);

	final Placeholder SPECIFIC_GROUP_REPORTER = new Placeholder(Messages.JiraFilterDefinitionPage_Specified_Group);

	final Placeholder SPECIFIC_USER_ASSIGNEE = new Placeholder(Messages.JiraFilterDefinitionPage_Specified_User);

	final Placeholder SPECIFIC_USER_REPORTER = new Placeholder(Messages.JiraFilterDefinitionPage_Specified_User);

	private ListViewer status;

	private Text titleText;

	final Placeholder UNASSIGNED = new Placeholder(Messages.JiraFilterDefinitionPage_Unassigned);

	final Placeholder UNRELEASED_VERSION = new Placeholder(Messages.JiraFilterDefinitionPage_Unreleased_Versions);

	final Placeholder UNRESOLVED = new Placeholder(Messages.JiraFilterDefinitionPage_Unresolved);

	private DatePicker updatedEndDatePicker;

	private DatePicker updatedStartDatePicker;

	private FilterDefinition workingCopy;

	public JiraFilterDefinitionPage(TaskRepository repository) {
		this(repository, null);
	}

	public JiraFilterDefinitionPage(TaskRepository repository, IRepositoryQuery query) {
		super(Messages.JiraFilterDefinitionPage_JIRA_Query, repository, query);
		this.client = JiraClientFactory.getDefault().getJiraClient(repository);
		if (query != null) {
			this.workingCopy = (FilterDefinition) JiraUtil.getQuery(repository, client, query, false);
		} else {
			this.workingCopy = new FilterDefinition();
		}
		setDescription(Messages.JiraFilterDefinitionPage_Add_search_filters_to_define_query);
		setPageComplete(false);
	}

	@SuppressWarnings("unchecked")
	void applyChanges() {
		if (this.queryString.getText().length() > 0 || this.searchSummary.getSelection()
				|| this.searchDescription.getSelection() || this.searchEnvironment.getSelection()
				|| this.searchComments.getSelection()) {
			workingCopy.setContentFilter(new ContentFilter(this.queryString.getText(),
					this.searchSummary.getSelection(), this.searchDescription.getSelection(),
					this.searchEnvironment.getSelection(), this.searchComments.getSelection()));
		} else {
			workingCopy.setContentFilter(null);
		}

		IStructuredSelection projectSelection = (IStructuredSelection) this.project.getSelection();
		if (projectSelection.isEmpty()) {
			workingCopy.setProjectFilter(null);
		} else {
			boolean selectionContainsAll = false;
			boolean selectionContainsNone = false;
			List<Project> selectedProjects = new ArrayList<Project>();
			for (Iterator i = projectSelection.iterator(); i.hasNext();) {
				Object selection = i.next();
				if (ALL_PROJECTS.equals(selection)) {
					selectionContainsAll = true;
				} else if (NO_PROJECT.equals(selection)) {
					selectionContainsNone = true;
				} else if (selection instanceof Project) {
					selectedProjects.add((Project) selection);
				}
			}
			if (selectionContainsAll) {
				workingCopy.setProjectFilter(null);
			} else if (selectedProjects.size() > 0) {
				workingCopy.setProjectFilter(new ProjectFilter(
						selectedProjects.toArray(new Project[selectedProjects.size()])));
			} else if (selectionContainsNone) {
				workingCopy.setProjectFilter(new ProjectFilter(new Project[0]));
			} else {
				workingCopy.setProjectFilter(null);
			}
		}

		IStructuredSelection reportedInSelection = (IStructuredSelection) reportedIn.getSelection();
		if (reportedInSelection.isEmpty()) {
			workingCopy.setReportedInVersionFilter(null);
		} else {
			boolean selectionContainsReleased = false;
			boolean selectionContainsUnreleased = false;
			boolean selectionContainsAll = false;
			boolean selectionContainsNone = false;

			List<Version> selectedVersions = new ArrayList<Version>();

			for (Iterator i = reportedInSelection.iterator(); i.hasNext();) {
				Object selection = i.next();
				if (ANY_REPORTED_VERSION.equals(selection)) {
					selectionContainsAll = true;
				} else if (NO_REPORTED_VERSION.equals(selection)) {
					selectionContainsNone = true;
				} else if (RELEASED_VERSION.equals(selection)) {
					selectionContainsReleased = true;
				} else if (UNRELEASED_VERSION.equals(selection)) {
					selectionContainsUnreleased = true;
				} else if (selection instanceof Version) {
					selectedVersions.add((Version) selection);
				}
			}

			if (selectionContainsAll) {
				workingCopy.setReportedInVersionFilter(null);
			} else if (selectionContainsNone) {
				workingCopy.setReportedInVersionFilter(new VersionFilter(new Version[0]));
			} else if (selectionContainsReleased || selectionContainsUnreleased) {
				workingCopy.setReportedInVersionFilter(new VersionFilter(selectionContainsReleased,
						selectionContainsUnreleased));
			} else if (selectedVersions.size() > 0) {
				workingCopy.setReportedInVersionFilter(new VersionFilter(
						selectedVersions.toArray(new Version[selectedVersions.size()])));
			} else {
				workingCopy.setReportedInVersionFilter(null);
			}
		}

		IStructuredSelection fixForSelection = (IStructuredSelection) fixFor.getSelection();
		if (fixForSelection.isEmpty()) {
			workingCopy.setFixForVersionFilter(null);
		} else {
			boolean selectionContainsReleased = false;
			boolean selectionContainsUnreleased = false;
			boolean selectionContainsAll = false;
			boolean selectionContainsNone = false;

			List<Version> selectedVersions = new ArrayList<Version>();

			for (Iterator i = fixForSelection.iterator(); i.hasNext();) {
				Object selection = i.next();
				if (ANY_FIX_VERSION.equals(selection)) {
					selectionContainsAll = true;
				} else if (NO_FIX_VERSION.equals(selection)) {
					selectionContainsNone = true;
				} else if (RELEASED_VERSION.equals(selection)) {
					selectionContainsReleased = true;
				} else if (UNRELEASED_VERSION.equals(selection)) {
					selectionContainsUnreleased = true;
				} else if (selection instanceof Version) {
					selectedVersions.add((Version) selection);
				}
			}

			if (selectionContainsAll) {
				workingCopy.setFixForVersionFilter(null);
			} else if (selectionContainsNone) {
				workingCopy.setFixForVersionFilter(new VersionFilter(new Version[0]));
			} else if (selectionContainsReleased || selectionContainsUnreleased) {
				workingCopy.setFixForVersionFilter(new VersionFilter(selectionContainsReleased,
						selectionContainsUnreleased));
			} else if (selectedVersions.size() > 0) {
				workingCopy.setFixForVersionFilter(new VersionFilter(
						selectedVersions.toArray(new Version[selectedVersions.size()])));
			} else {
				workingCopy.setFixForVersionFilter(null);
			}
		}

		IStructuredSelection componentsSelection = (IStructuredSelection) components.getSelection();
		if (componentsSelection.isEmpty()) {
			workingCopy.setComponentFilter(null);
		} else {

			boolean selectionContainsAll = false;
			boolean selectionContainsNone = false;
			List<Component> selectedComponents = new ArrayList<Component>();

			for (Iterator i = componentsSelection.iterator(); i.hasNext();) {
				Object selection = i.next();
				if (ANY_COMPONENT.equals(selection)) {
					selectionContainsAll = true;
				} else if (NO_COMPONENT.equals(selection)) {
					selectionContainsNone = true;
				} else if (selection instanceof Component) {
					selectedComponents.add((Component) selection);
				}
			}

			if (selectionContainsAll) {
				workingCopy.setComponentFilter(null);
			} else if (selectedComponents.size() > 0) {
				workingCopy.setComponentFilter(new ComponentFilter(
						selectedComponents.toArray(new Component[selectedComponents.size()])));
			} else if (selectionContainsNone) {
				workingCopy.setComponentFilter(new ComponentFilter(new Component[0]));
			} else {
				workingCopy.setComponentFilter(null);
			}
		}

		// attributes

		// TODO support standard and subtask issue types
		IStructuredSelection issueTypeSelection = (IStructuredSelection) issueType.getSelection();
		if (issueTypeSelection.isEmpty()) {
			workingCopy.setIssueTypeFilter(null);
		} else {
			boolean isAnyIssueTypeSelected = false;

			List<IssueType> selectedIssueTypes = new ArrayList<IssueType>();

			for (Iterator i = issueTypeSelection.iterator(); i.hasNext();) {
				Object selection = i.next();
				if (ANY_ISSUE_TYPE.equals(selection)) {
					isAnyIssueTypeSelected = true;
				} else if (selection instanceof IssueType) {
					selectedIssueTypes.add((IssueType) selection);
				}
			}

			if (isAnyIssueTypeSelected) {
				workingCopy.setIssueTypeFilter(null);
			} else {
				workingCopy.setIssueTypeFilter(new IssueTypeFilter(
						selectedIssueTypes.toArray(new IssueType[selectedIssueTypes.size()])));
			}
		}

		IStructuredSelection reporterSelection = (IStructuredSelection) reporterType.getSelection();
		if (reporterSelection.isEmpty()) {
			workingCopy.setReportedByFilter(null);
		} else {
			if (ANY_REPORTER.equals(reporterSelection.getFirstElement())) {
				workingCopy.setReportedByFilter(null);
			} else if (NO_REPORTER.equals(reporterSelection.getFirstElement())) {
				workingCopy.setReportedByFilter(new NobodyFilter());
			} else if (CURRENT_USER_REPORTER.equals(reporterSelection.getFirstElement())) {
				workingCopy.setReportedByFilter(new CurrentUserFilter());
			} else if (SPECIFIC_GROUP_REPORTER.equals(reporterSelection.getFirstElement())) {
				workingCopy.setReportedByFilter(new UserInGroupFilter(reporter.getText()));
			} else if (SPECIFIC_USER_REPORTER.equals(reporterSelection.getFirstElement())) {
				workingCopy.setReportedByFilter(new SpecificUserFilter(reporter.getText()));
			} else {
				workingCopy.setReportedByFilter(null);
			}
		}

		IStructuredSelection assigneeSelection = (IStructuredSelection) assigneeType.getSelection();
		if (assigneeSelection.isEmpty()) {
			workingCopy.setAssignedToFilter(null);
		} else {
			if (ANY_ASSIGNEE.equals(assigneeSelection.getFirstElement())) {
				workingCopy.setAssignedToFilter(null);
			} else if (UNASSIGNED.equals(assigneeSelection.getFirstElement())) {
				workingCopy.setAssignedToFilter(new NobodyFilter());
			} else if (CURRENT_USER_ASSIGNEE.equals(assigneeSelection.getFirstElement())) {
				workingCopy.setAssignedToFilter(new CurrentUserFilter());
			} else if (SPECIFIC_GROUP_ASSIGNEE.equals(assigneeSelection.getFirstElement())) {
				workingCopy.setAssignedToFilter(new UserInGroupFilter(assignee.getText()));
			} else if (SPECIFIC_USER_ASSIGNEE.equals(assigneeSelection.getFirstElement())) {
				workingCopy.setAssignedToFilter(new SpecificUserFilter(assignee.getText()));
			} else {
				workingCopy.setAssignedToFilter(null);
			}
		}

		IStructuredSelection statusSelection = (IStructuredSelection) status.getSelection();
		if (statusSelection.isEmpty()) {
			workingCopy.setStatusFilter(null);
		} else {
			boolean isAnyStatusSelected = false;

			List<JiraStatus> selectedStatuses = new ArrayList<JiraStatus>();

			for (Iterator i = statusSelection.iterator(); i.hasNext();) {
				Object selection = i.next();
				if (ANY_STATUS.equals(selection)) {
					isAnyStatusSelected = true;
				} else if (selection instanceof JiraStatus) {
					selectedStatuses.add((JiraStatus) selection);
				}
			}

			if (isAnyStatusSelected) {
				workingCopy.setStatusFilter(null);
			} else {
				workingCopy.setStatusFilter(new StatusFilter(
						selectedStatuses.toArray(new JiraStatus[selectedStatuses.size()])));
			}
		}

		IStructuredSelection resolutionSelection = (IStructuredSelection) resolution.getSelection();
		if (resolutionSelection.isEmpty()) {
			workingCopy.setResolutionFilter(null);
		} else {
			boolean isAnyResolutionSelected = false;

			List<Resolution> selectedResolutions = new ArrayList<Resolution>();

			for (Iterator i = resolutionSelection.iterator(); i.hasNext();) {
				Object selection = i.next();
				if (ANY_RESOLUTION.equals(selection)) {
					isAnyResolutionSelected = true;
				} else if (selection instanceof Resolution) {
					selectedResolutions.add((Resolution) selection);
				}
			}

			if (isAnyResolutionSelected) {
				workingCopy.setResolutionFilter(null);
			} else {
				workingCopy.setResolutionFilter(new ResolutionFilter(
						selectedResolutions.toArray(new Resolution[selectedResolutions.size()])));
			}
		}

		IStructuredSelection prioritySelection = (IStructuredSelection) priority.getSelection();
		if (prioritySelection.isEmpty()) {
			workingCopy.setPriorityFilter(null);
		} else {
			boolean isAnyPrioritiesSelected = false;

			List<Priority> selectedPriorities = new ArrayList<Priority>();

			for (Iterator i = prioritySelection.iterator(); i.hasNext();) {
				Object selection = i.next();
				if (ANY_PRIORITY.equals(selection)) {
					isAnyPrioritiesSelected = true;
				} else if (selection instanceof Priority) {
					selectedPriorities.add((Priority) selection);
				}
			}

			if (isAnyPrioritiesSelected) {
				workingCopy.setPriorityFilter(null);
			} else {
				workingCopy.setPriorityFilter(new PriorityFilter(
						selectedPriorities.toArray(new Priority[selectedPriorities.size()])));
			}
		}

		workingCopy.setDueDateFilter(getRangeFilter(dueStartDatePicker, dueEndDatePicker));

		workingCopy.setCreatedDateFilter(getRangeFilter(createdStartDatePicker, createdEndDatePicker));

		workingCopy.setUpdatedDateFilter(getRangeFilter(updatedStartDatePicker, updatedEndDatePicker));
	}

	@Override
	public void applyTo(IRepositoryQuery query) {
		applyChanges();
		if (titleText != null) {
			query.setSummary(titleText.getText());
		}
		JiraUtil.setQuery(getTaskRepository(), query, workingCopy);
	}

	private void createComponentsViewer(Composite c) {
		components = new ListViewer(c, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 50;
		gridData.widthHint = 90;
		components.getControl().setLayoutData(gridData);

		components.setContentProvider(new IStructuredContentProvider() {
			private Object[] currentElements;

			public void dispose() {
			}

			public Object[] getElements(Object inputElement) {
				return currentElements;
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				Project[] projects = (Project[]) newInput;
				if (projects == null || projects.length == 0) {
					currentElements = new Object[] { ANY_COMPONENT };
				} else {
					Set<Object> elements = new LinkedHashSet<Object>();
					elements.add(ANY_COMPONENT);
					elements.add(NO_COMPONENT);
					for (Project project : projects) {
						if (project != null) {
							elements.addAll(Arrays.asList(project.getComponents()));
						}
					}
					currentElements = elements.toArray(new Object[elements.size()]);
				}
			}

		});
		components.setLabelProvider(new ComponentLabelProvider());
		components.setInput(NO_PROJECTS);
	}

	public void createControl(final Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(3, false));
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		setControl(c);

		if (!inSearchContainer()) {
			Label lblName = new Label(c, SWT.NONE);
			final GridData gridData = new GridData();
			lblName.setLayoutData(gridData);
			lblName.setText(Messages.JiraFilterDefinitionPage_Query_Title);

			titleText = new Text(c, SWT.BORDER);
			titleText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
			titleText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
//					validatePage();
					setPageComplete(isPageComplete());
				}
			});
		}

		SashForm sashForm = new SashForm(c, SWT.VERTICAL);
		GridData gd_sashForm = new GridData(SWT.FILL, SWT.FILL, false, true, 3, 1);
		gd_sashForm.heightHint = 200;
		gd_sashForm.widthHint = 500;
		sashForm.setLayoutData(gd_sashForm);

		{
			SashForm cc = new SashForm(sashForm, SWT.HORIZONTAL);

			{
				Composite comp = new Composite(cc, SWT.NONE);
				GridLayout gridLayout = new GridLayout(1, false);
				gridLayout.marginWidth = 0;
				comp.setLayout(gridLayout);

				Label label = new Label(comp, SWT.NONE);
				label.setText(Messages.JiraFilterDefinitionPage_Project);
				createProjectsViewer(comp);
			}

			{
				Composite comp = new Composite(cc, SWT.NONE);
				GridLayout gridLayout = new GridLayout(1, false);
				gridLayout.marginWidth = 0;
				comp.setLayout(gridLayout);

				new Label(comp, SWT.NONE).setText(Messages.JiraFilterDefinitionPage_Fix_For);
				createFixForViewer(comp);
			}

			{
				Composite comp = new Composite(cc, SWT.NONE);
				GridLayout gridLayout = new GridLayout(1, false);
				gridLayout.marginWidth = 0;
				comp.setLayout(gridLayout);

				new Label(comp, SWT.NONE).setText(Messages.JiraFilterDefinitionPage_In_Components);
				createComponentsViewer(comp);
			}

			{
				Composite comp = new Composite(cc, SWT.NONE);
				GridLayout gridLayout = new GridLayout(1, false);
				gridLayout.marginWidth = 0;
				comp.setLayout(gridLayout);

				Label label = new Label(comp, SWT.NONE);
				label.setText(Messages.JiraFilterDefinitionPage_Reported_In);
				createReportedInViewer(comp);
			}
			cc.setWeights(new int[] { 1, 1, 1, 1 });
		}

		{
			SashForm cc = new SashForm(sashForm, SWT.NONE);

			ISelectionChangedListener selectionChangeListener = new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					// validatePage();
				}
			};

			{
				Composite comp = new Composite(cc, SWT.NONE);
				GridLayout gridLayout = new GridLayout();
				gridLayout.marginHeight = 0;
				gridLayout.marginWidth = 0;
				comp.setLayout(gridLayout);

				Label typeLabel = new Label(comp, SWT.NONE);
				typeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
				typeLabel.setText(Messages.JiraFilterDefinitionPage_Type);

				issueType = new ListViewer(comp, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL);
				GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
				gridData.heightHint = 50;
				gridData.widthHint = 90;
				issueType.getList().setLayoutData(gridData);

				issueType.setLabelProvider(new LabelProvider() {

					@Override
					public String getText(Object element) {
						if (element instanceof Placeholder) {
							return ((Placeholder) element).getText();
						}

						return ((IssueType) element).getName();
					}

				});

				issueType.addSelectionChangedListener(selectionChangeListener);
			}

			{
				Composite comp = new Composite(cc, SWT.NONE);
				GridLayout gridLayout = new GridLayout();
				gridLayout.marginHeight = 0;
				gridLayout.marginWidth = 0;
				comp.setLayout(gridLayout);

				Label statusLabel = new Label(comp, SWT.NONE);
				statusLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
				statusLabel.setText(Messages.JiraFilterDefinitionPage_Status);

				status = new ListViewer(comp, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL);
				GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
				gridData.heightHint = 50;
				gridData.widthHint = 90;
				status.getList().setLayoutData(gridData);

				status.setLabelProvider(new LabelProvider() {

					@Override
					public String getText(Object element) {
						if (element instanceof Placeholder) {
							return ((Placeholder) element).getText();
						}

						return ((JiraStatus) element).getName();
					}

				});

				status.addSelectionChangedListener(selectionChangeListener);
			}

			{
				Composite comp = new Composite(cc, SWT.NONE);
				GridLayout gridLayout = new GridLayout();
				gridLayout.marginHeight = 0;
				gridLayout.marginWidth = 0;
				comp.setLayout(gridLayout);

				Label resolutionLabel = new Label(comp, SWT.NONE);
				resolutionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
				resolutionLabel.setText(Messages.JiraFilterDefinitionPage_Resolution);

				resolution = new ListViewer(comp, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL);
				GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
				gridData.heightHint = 50;
				gridData.widthHint = 90;
				resolution.getList().setLayoutData(gridData);

				resolution.setLabelProvider(new LabelProvider() {

					@Override
					public String getText(Object element) {
						if (element instanceof Placeholder) {
							return ((Placeholder) element).getText();
						}

						return ((Resolution) element).getName();
					}

				});

				resolution.addSelectionChangedListener(selectionChangeListener);
			}

			{
				Composite comp = new Composite(cc, SWT.NONE);
				GridLayout gridLayout = new GridLayout();
				gridLayout.marginHeight = 0;
				gridLayout.marginWidth = 0;
				comp.setLayout(gridLayout);

				Label priorityLabel = new Label(comp, SWT.NONE);
				priorityLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
				priorityLabel.setText(Messages.JiraFilterDefinitionPage_Priority);

				priority = new ListViewer(comp, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL);
				GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
				gridData.heightHint = 50;
				gridData.widthHint = 90;
				priority.getList().setLayoutData(gridData);

				priority.setLabelProvider(new LabelProvider() {

					@Override
					public String getText(Object element) {
						if (element instanceof Placeholder) {
							return ((Placeholder) element).getText();
						}

						return ((Priority) element).getName();
					}

				});
				priority.addSelectionChangedListener(selectionChangeListener);
			}

			cc.setWeights(new int[] { 1, 1, 1, 1 });
		}
		sashForm.setWeights(new int[] { 1, 1 });

		createUpdateButton(c);

		Label lblQuery = new Label(c, SWT.NONE);
		final GridData gd_lblQuery = new GridData();
		gd_lblQuery.verticalIndent = 7;
		lblQuery.setLayoutData(gd_lblQuery);
		lblQuery.setText(Messages.JiraFilterDefinitionPage_Query);
		queryString = new Text(c, SWT.BORDER);
		final GridData gd_queryString = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
		gd_queryString.verticalIndent = 7;
		queryString.setLayoutData(gd_queryString);
		// TODO put content assist here and a label describing what is
		// available

		queryString.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				// validatePage();
			}
		});

		Label lblFields = new Label(c, SWT.NONE);
		lblFields.setText(Messages.JiraFilterDefinitionPage_FILEDS);
		lblFields.setLayoutData(new GridData());

		{
			SelectionAdapter selectionAdapter = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					// validatePage();
				}
			};

			Composite comp = new Composite(c, SWT.NONE);
			comp.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
			comp.setLayout(new FillLayout());

			searchSummary = new Button(comp, SWT.CHECK);
			searchSummary.setText(Messages.JiraFilterDefinitionPage_Summary);
			searchSummary.addSelectionListener(selectionAdapter);

			searchDescription = new Button(comp, SWT.CHECK);
			searchDescription.setText(Messages.JiraFilterDefinitionPage_Description);
			searchDescription.addSelectionListener(selectionAdapter);

			searchComments = new Button(comp, SWT.CHECK);
			searchComments.setText(Messages.JiraFilterDefinitionPage_Comments);
			searchComments.addSelectionListener(selectionAdapter);

			searchEnvironment = new Button(comp, SWT.CHECK);
			searchEnvironment.setText(Messages.JiraFilterDefinitionPage_Environment);
			searchEnvironment.addSelectionListener(selectionAdapter);
		}

		{
			Label reportedByLabel = new Label(c, SWT.NONE);
			reportedByLabel.setText(Messages.JiraFilterDefinitionPage_Reported_By);

			reporterType = new ComboViewer(c, SWT.BORDER | SWT.READ_ONLY);
			GridData gridData_1 = new GridData(SWT.FILL, SWT.CENTER, false, false);
			gridData_1.widthHint = 133;
			reporterType.getControl().setLayoutData(gridData_1);

			reporterType.setContentProvider(new IStructuredContentProvider() {

				public void dispose() {
				}

				public Object[] getElements(Object inputElement) {
					return new Object[] { ANY_REPORTER, NO_REPORTER, CURRENT_USER_REPORTER, SPECIFIC_USER_REPORTER,
							SPECIFIC_GROUP_REPORTER };
				}

				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				}

			});

			reporterType.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					return ((Placeholder) element).getText();
				}
			});

			reporterType.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
					if (SPECIFIC_USER_REPORTER.equals(selection) || SPECIFIC_GROUP_REPORTER.equals(selection)) {
						reporter.setEnabled(true);
					} else {
						reporter.setEnabled(false);
						reporter.setText(""); //$NON-NLS-1$
					}
					// validatePage();
				}
			});

			reporterType.setInput(client);

			reporter = new Text(c, SWT.BORDER);
			reporter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			reporter.setEnabled(false);

			reporter.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					// validatePage();
				}
			});
		}

		{
			Label assignedToLabel = new Label(c, SWT.NONE);
			assignedToLabel.setText(Messages.JiraFilterDefinitionPage_Assigned_To);

			assigneeType = new ComboViewer(c, SWT.BORDER | SWT.READ_ONLY);
			GridData gridData_2 = new GridData(SWT.FILL, SWT.CENTER, false, false);
			gridData_2.widthHint = 133;
			assigneeType.getCombo().setLayoutData(gridData_2);

			assigneeType.setContentProvider(new IStructuredContentProvider() {

				public void dispose() {
				}

				public Object[] getElements(Object inputElement) {
					return new Object[] { ANY_ASSIGNEE, UNASSIGNED, CURRENT_USER_ASSIGNEE, SPECIFIC_USER_ASSIGNEE,
							SPECIFIC_GROUP_ASSIGNEE };
				}

				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				}

			});

			assigneeType.setLabelProvider(new LabelProvider() {

				@Override
				public String getText(Object element) {
					return ((Placeholder) element).getText();
				}

			});

			assigneeType.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
					if (SPECIFIC_USER_ASSIGNEE.equals(selection) || SPECIFIC_GROUP_ASSIGNEE.equals(selection)) {
						assignee.setEnabled(true);
					} else {
						assignee.setEnabled(false);
						assignee.setText(""); //$NON-NLS-1$
					}
					// validatePage();
				}
			});

			assigneeType.setInput(client);

			assignee = new Text(c, SWT.BORDER);
			assignee.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			assignee.setEnabled(false);
			assignee.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					// validatePage();
				}
			});
		}

		{
			Label createdLabel = new Label(c, SWT.NONE);
			createdLabel.setText(Messages.JiraFilterDefinitionPage_Created);

			Composite composite = new Composite(c, SWT.NONE);
			FillLayout fillLayout = new FillLayout();
			fillLayout.spacing = 5;
			composite.setLayout(fillLayout);
			composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

			createdStartDatePicker = new DatePicker(composite, SWT.BORDER,
					Messages.JiraFilterDefinitionPage__start_date_, true, 0);
			createdEndDatePicker = new DatePicker(composite, SWT.BORDER, Messages.JiraFilterDefinitionPage__end_date_,
					true, 0);
		}

		{
			Label updatedLabel = new Label(c, SWT.NONE);
			updatedLabel.setText(Messages.JiraFilterDefinitionPage_Updated);

			Composite composite = new Composite(c, SWT.NONE);
			FillLayout fillLayout = new FillLayout();
			fillLayout.spacing = 5;
			composite.setLayout(fillLayout);
			composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

			updatedStartDatePicker = new DatePicker(composite, SWT.BORDER,
					Messages.JiraFilterDefinitionPage__start_date_, true, 0);
			updatedEndDatePicker = new DatePicker(composite, SWT.BORDER, Messages.JiraFilterDefinitionPage__end_date_,
					true, 0);
		}

		{
			Label dueDateLabel = new Label(c, SWT.NONE);
			dueDateLabel.setText(Messages.JiraFilterDefinitionPage_Due_Date);

			Composite composite = new Composite(c, SWT.NONE);
			FillLayout fillLayout = new FillLayout();
			fillLayout.spacing = 5;
			composite.setLayout(fillLayout);
			composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

			dueStartDatePicker = new DatePicker(composite, SWT.BORDER, Messages.JiraFilterDefinitionPage__start_date_,
					true, 0);
			dueEndDatePicker = new DatePicker(composite, SWT.BORDER, Messages.JiraFilterDefinitionPage__end_date_,
					true, 0);
		}

		{
			// Label maxHitsLabel = new Label(c, SWT.NONE);
			// maxHitsLabel.setText("Maximum Results:");
			//
			// Composite composite = new Composite(c, SWT.NONE);
			// FillLayout fillLayout = new FillLayout();
			// fillLayout.spacing = 5;
			// composite.setLayout(fillLayout);
			// composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false,
			// false, 2, 1));
		}

		// new FillLayout()f validation here
		// if (isNew) {
		loadDefaults();
		// } else {

		// }
		Dialog.applyDialogFont(parent);
	}

//	@Override
//	public boolean isPageComplete() {
//		// XXX workaround for bad implementation of AbstractRepositoryQueryPage.isPageComplete()
//		String errorMessage = getErrorMessage();
//		boolean isPageComplete = super.isPageComplete();
//		if (errorMessage != null) {
//			setErrorMessage(errorMessage);
//		}
//		return isPageComplete;
//	}
//
//	void validatePage() {
//		if (title != null && !title.getText().equals("")) {
//			setErrorMessage(null);
//			setPageComplete(true);
//		} else {
//			setErrorMessage("Query title is mandatory");
//			setPageComplete(false);
//		}
//	}

	private void createFixForViewer(Composite c) {
		fixFor = new ListViewer(c, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 50;
		gridData.widthHint = 90;
		fixFor.getControl().setLayoutData(gridData);

		fixFor.setContentProvider(new IStructuredContentProvider() {
			private Object[] currentElements;

			public void dispose() {
			}

			public Object[] getElements(Object inputElement) {
				return currentElements;
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				Project[] projects = (Project[]) newInput;
				if (projects == null || projects.length == 0) {
					currentElements = new Object[] { ANY_FIX_VERSION };
				} else {
					List<Object> elements = new ArrayList<Object>();
					elements.add(ANY_FIX_VERSION);
					elements.add(NO_FIX_VERSION);

					Set<Version> releasedVersions = new LinkedHashSet<Version>();
					Set<Version> unreleasedVersions = new LinkedHashSet<Version>();

					for (Project project : projects) {
						if (project != null) {
							releasedVersions.addAll(Arrays.asList(project.getReleasedVersions()));
							unreleasedVersions.addAll(Arrays.asList(project.getUnreleasedVersions()));
						}
					}

					elements.add(RELEASED_VERSION);
					elements.addAll(releasedVersions);
					elements.add(UNRELEASED_VERSION);
					elements.addAll(unreleasedVersions);

					currentElements = elements.toArray(new Object[elements.size()]);
				}
			}
		});
		fixFor.setLabelProvider(new VersionLabelProvider());
		fixFor.setInput(NO_PROJECTS);
	}

	private void createProjectsViewer(Composite c) {
		project = new ListViewer(c, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 50;
		gridData.widthHint = 90;
		project.getControl().setLayoutData(gridData);

		project.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Placeholder) {
					return ((Placeholder) element).getText();
				}
				return ((Project) element).getName();
			}
		});

		project.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = ((IStructuredSelection) event.getSelection());
				List<Project> selectedProjects = new ArrayList<Project>();
				if (!selection.isEmpty()) {
					for (Iterator<?> i = selection.iterator(); i.hasNext();) {
						Object sel = i.next();
						if (!(sel instanceof Placeholder)) {
							selectedProjects.add((Project) sel);
						}
					}
				}
				updateCurrentProjects(selectedProjects.toArray(new Project[selectedProjects.size()]));
				// validatePage();
			}
		});
	}

	private void createReportedInViewer(Composite c) {
		reportedIn = new ListViewer(c, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 50;
		gridData.widthHint = 90;
		reportedIn.getControl().setLayoutData(gridData);

		reportedIn.setContentProvider(new IStructuredContentProvider() {
			private Object[] currentElements;

			public void dispose() {
			}

			public Object[] getElements(Object inputElement) {
				return currentElements;
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				Project[] projects = (Project[]) newInput;
				if (projects == null || projects.length == 0) {
					currentElements = new Object[] { ANY_REPORTED_VERSION };
				} else {
					List<Object> elements = new ArrayList<Object>();
					elements.add(ANY_REPORTED_VERSION);
					elements.add(NO_REPORTED_VERSION);

					Set<Object> releasedVersions = new LinkedHashSet<Object>();
					Set<Object> unreleasedVersions = new LinkedHashSet<Object>();

					for (Project project : projects) {
						if (project != null) {
							releasedVersions.addAll(Arrays.asList(project.getReleasedVersions()));
							unreleasedVersions.addAll(Arrays.asList(project.getUnreleasedVersions()));
						}
					}

					elements.add(RELEASED_VERSION);
					elements.addAll(releasedVersions);
					elements.add(UNRELEASED_VERSION);
					elements.addAll(unreleasedVersions);

					currentElements = elements.toArray(new Object[elements.size()]);
				}
			}

		});
		reportedIn.setLabelProvider(new VersionLabelProvider());
		reportedIn.setInput(NO_PROJECTS);
	}

	protected void createUpdateButton(final Composite control) {
		Button updateButton = new Button(control, SWT.PUSH);
		updateButton.setText(Messages.JiraFilterDefinitionPage_Update_Attributes_from_Repository);
		updateButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 3, 1));
		updateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				applyChanges();
				updateAttributesFromRepository(true);
				loadFromWorkingCopy();
			}
		});
	}

	@Override
	public IDialogSettings getDialogSettings() {
		IDialogSettings settings = JiraUiPlugin.getDefault().getDialogSettings();
		IDialogSettings dialogSettings = settings.getSection(PAGE_NAME);
		if (dialogSettings == null) {
			dialogSettings = settings.addNewSection(PAGE_NAME);
		}
		return dialogSettings;
	}

	@Override
	public String getQueryTitle() {
		return (titleText != null) ? titleText.getText() : ""; //$NON-NLS-1$
	}

	private DateRangeFilter getRangeFilter(DatePicker startDatePicker, DatePicker endDatePicker) {
		Calendar startDate = startDatePicker.getDate();
		Calendar endDate = endDatePicker.getDate();
		return new DateRangeFilter(startDate == null ? null : startDate.getTime(), endDate == null ? null
				: endDate.getTime());
	}

	private void initializeContentProviders() {
		project.setContentProvider(new IStructuredContentProvider() {

			public void dispose() {
			}

			public Object[] getElements(Object inputElement) {
				JiraClient server = (JiraClient) inputElement;
				Object[] elements = new Object[server.getCache().getProjects().length + 1];
				elements[0] = ALL_PROJECTS;
				System.arraycopy(server.getCache().getProjects(), 0, elements, 1,
						server.getCache().getProjects().length);
				return elements;
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

		});
		project.setInput(client);

		issueType.setContentProvider(new IStructuredContentProvider() {

			public void dispose() {
			}

			public Object[] getElements(Object inputElement) {
				JiraClient server = (JiraClient) inputElement;
				Object[] elements = new Object[server.getCache().getIssueTypes().length + 1];
				elements[0] = ANY_ISSUE_TYPE;
				System.arraycopy(server.getCache().getIssueTypes(), 0, elements, 1,
						server.getCache().getIssueTypes().length);

				return elements;
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		issueType.setInput(client);

		status.setContentProvider(new IStructuredContentProvider() {

			public void dispose() {
			}

			public Object[] getElements(Object inputElement) {
				JiraClient server = (JiraClient) inputElement;
				Object[] elements = new Object[server.getCache().getStatuses().length + 1];
				elements[0] = ANY_STATUS;
				System.arraycopy(server.getCache().getStatuses(), 0, elements, 1,
						server.getCache().getStatuses().length);

				return elements;
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		status.setInput(client);

		resolution.setContentProvider(new IStructuredContentProvider() {

			public void dispose() {
			}

			public Object[] getElements(Object inputElement) {
				JiraClient server = (JiraClient) inputElement;
				Object[] elements = new Object[server.getCache().getResolutions().length + 2];
				elements[0] = ANY_RESOLUTION;
				elements[1] = UNRESOLVED;
				System.arraycopy(server.getCache().getResolutions(), 0, elements, 2,
						server.getCache().getResolutions().length);

				return elements;
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		resolution.setInput(client);

		priority.setContentProvider(new IStructuredContentProvider() {

			public void dispose() {
			}

			public Object[] getElements(Object inputElement) {
				JiraClient client = (JiraClient) inputElement;
				Object[] elements = new Object[client.getCache().getPriorities().length + 1];
				elements[0] = ANY_PRIORITY;
				System.arraycopy(client.getCache().getPriorities(), 0, elements, 1,
						client.getCache().getPriorities().length);

				return elements;
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		priority.setInput(client);
	}

	private void initializePage() {
		updateAttributesFromRepository(false);
		if (inSearchContainer()) {
			restoreWidgetValues();
		}

		if (titleText != null && getQuery() != null) {
			titleText.setText(getQuery().getSummary());
		}

		loadFromWorkingCopy();
	}

	private void loadDefaults() {
		// project.setSelection(new StructuredSelection(new Placeholder("All
		// Projects")));
		searchSummary.setSelection(true);
		searchDescription.setSelection(true);

		// issueType.setSelection(new StructuredSelection(ANY_ISSUE_TYPE));
		// reporterType.setSelection(new StructuredSelection(ANY_REPORTER));
		// assigneeType.setSelection(new StructuredSelection(ANY_ASSIGNEE));
		// status.setSelection(new StructuredSelection(ANY_STATUS));
		// resolution.setSelection(new StructuredSelection(ANY_RESOLUTION));
		// priority.setSelection(new StructuredSelection(ANY_PRIORITY));
	}

	private void loadFromWorkingCopy() {
		if (workingCopy.getProjectFilter() != null) {
			project.setSelection(new StructuredSelection(workingCopy.getProjectFilter().getProjects()), true);
		} else {
			project.setSelection(new StructuredSelection(ALL_PROJECTS), true);
		}

		if (workingCopy.getFixForVersionFilter() != null) {
			if (workingCopy.getFixForVersionFilter().hasNoVersion()) {
				fixFor.setSelection(new StructuredSelection(new Object[] { NO_FIX_VERSION }));
			} else if (workingCopy.getFixForVersionFilter().isReleasedVersions()
					&& workingCopy.getFixForVersionFilter().isUnreleasedVersions()) {
				fixFor.setSelection(new StructuredSelection(new Object[] { RELEASED_VERSION, UNRELEASED_VERSION }),
						true);
			} else if (workingCopy.getFixForVersionFilter().isReleasedVersions()) {
				fixFor.setSelection(new StructuredSelection(RELEASED_VERSION), true);
			} else if (workingCopy.getFixForVersionFilter().isUnreleasedVersions()) {
				fixFor.setSelection(new StructuredSelection(UNRELEASED_VERSION), true);
			} else {
				fixFor.setSelection(new StructuredSelection(workingCopy.getFixForVersionFilter().getVersions()), true);
			}
		} else {
			fixFor.setSelection(new StructuredSelection(ANY_FIX_VERSION), true);
		}

		if (workingCopy.getReportedInVersionFilter() != null) {
			if (workingCopy.getReportedInVersionFilter().hasNoVersion()) {
				reportedIn.setSelection(new StructuredSelection(new Object[] { NO_REPORTED_VERSION }), true);
			} else if (workingCopy.getReportedInVersionFilter().isReleasedVersions()
					&& workingCopy.getReportedInVersionFilter().isUnreleasedVersions()) {
				reportedIn.setSelection(new StructuredSelection(new Object[] { RELEASED_VERSION, UNRELEASED_VERSION }),
						true);
			} else if (workingCopy.getReportedInVersionFilter().isReleasedVersions()) {
				reportedIn.setSelection(new StructuredSelection(RELEASED_VERSION), true);
			} else if (workingCopy.getReportedInVersionFilter().isUnreleasedVersions()) {
				reportedIn.setSelection(new StructuredSelection(UNRELEASED_VERSION), true);
			} else {
				reportedIn.setSelection(
						new StructuredSelection(workingCopy.getReportedInVersionFilter().getVersions()), true);
			}
		} else {
			reportedIn.setSelection(new StructuredSelection(ANY_REPORTED_VERSION), true);
		}

		if (workingCopy.getContentFilter() != null) {
			this.queryString.setText(workingCopy.getContentFilter().getQueryString());
			this.searchComments.setSelection(workingCopy.getContentFilter().isSearchingComments());
			this.searchDescription.setSelection(workingCopy.getContentFilter().isSearchingDescription());
			this.searchEnvironment.setSelection(workingCopy.getContentFilter().isSearchingEnvironment());
			this.searchSummary.setSelection(workingCopy.getContentFilter().isSearchingSummary());
		}

		if (workingCopy.getComponentFilter() != null) {
			if (workingCopy.getComponentFilter().hasNoComponent()) {
				components.setSelection(new StructuredSelection(NO_COMPONENT), true);
			} else {
				components.setSelection(new StructuredSelection(workingCopy.getComponentFilter().getComponents()), true);
			}
		} else {
			components.setSelection(new StructuredSelection(ANY_COMPONENT), true);
		}

		// attributes

		if (workingCopy.getIssueTypeFilter() != null) {
			issueType.setSelection(new StructuredSelection(workingCopy.getIssueTypeFilter().getIsueTypes()), true);
		} else {
			issueType.setSelection(new StructuredSelection(ANY_ISSUE_TYPE), true);
		}

		if (workingCopy.getReportedByFilter() != null) {
			UserFilter reportedByFilter = workingCopy.getReportedByFilter();
			if (reportedByFilter instanceof NobodyFilter) {
				reporterType.setSelection(new StructuredSelection(NO_REPORTER));
			} else if (reportedByFilter instanceof CurrentUserFilter) {
				reporterType.setSelection(new StructuredSelection(CURRENT_USER_REPORTER));
			} else if (reportedByFilter instanceof SpecificUserFilter) {
				reporterType.setSelection(new StructuredSelection(SPECIFIC_USER_REPORTER));
				reporter.setText(((SpecificUserFilter) reportedByFilter).getUser());
			} else if (reportedByFilter instanceof UserInGroupFilter) {
				reporterType.setSelection(new StructuredSelection(SPECIFIC_GROUP_REPORTER));
				reporter.setText(((UserInGroupFilter) reportedByFilter).getGroup());
			}
		} else {
			reporterType.setSelection(new StructuredSelection(ANY_REPORTER));
		}

		if (workingCopy.getAssignedToFilter() != null) {
			UserFilter assignedToFilter = workingCopy.getAssignedToFilter();
			if (assignedToFilter instanceof NobodyFilter) {
				assigneeType.setSelection(new StructuredSelection(UNASSIGNED));
			} else if (assignedToFilter instanceof CurrentUserFilter) {
				assigneeType.setSelection(new StructuredSelection(CURRENT_USER_ASSIGNEE));
			} else if (assignedToFilter instanceof SpecificUserFilter) {
				assigneeType.setSelection(new StructuredSelection(SPECIFIC_USER_ASSIGNEE));
				assignee.setText(((SpecificUserFilter) assignedToFilter).getUser());
			} else if (assignedToFilter instanceof UserInGroupFilter) {
				assigneeType.setSelection(new StructuredSelection(SPECIFIC_GROUP_ASSIGNEE));
				assignee.setText(((UserInGroupFilter) assignedToFilter).getGroup());
			}
		} else {
			assigneeType.setSelection(new StructuredSelection(ANY_ASSIGNEE));
		}

		if (workingCopy.getStatusFilter() != null) {
			status.setSelection(new StructuredSelection(workingCopy.getStatusFilter().getStatuses()), true);
		} else {
			status.setSelection(new StructuredSelection(ANY_STATUS), true);
		}

		if (workingCopy.getResolutionFilter() != null) {
			Resolution[] resolutions = workingCopy.getResolutionFilter().getResolutions();
			if (resolutions.length == 0) {
				resolution.setSelection(new StructuredSelection(UNRESOLVED), true);
			} else {
				resolution.setSelection(new StructuredSelection(resolutions), true);
			}
		} else {
			resolution.setSelection(new StructuredSelection(ANY_RESOLUTION), true);
		}

		if (workingCopy.getPriorityFilter() != null) {
			priority.setSelection(new StructuredSelection(workingCopy.getPriorityFilter().getPriorities()), true);
		} else {
			priority.setSelection(new StructuredSelection(ANY_PRIORITY), true);
		}

		setDateRange(workingCopy.getCreatedDateFilter(), createdStartDatePicker, createdEndDatePicker);
		setDateRange(workingCopy.getUpdatedDateFilter(), updatedStartDatePicker, updatedEndDatePicker);
		setDateRange(workingCopy.getDueDateFilter(), dueStartDatePicker, dueEndDatePicker);
	}

	@Override
	public boolean performSearch() {
		if (inSearchContainer()) {
			saveState();
		}
		return super.performSearch();
	}

	private boolean restoreWidgetValues() {
		IDialogSettings settings = getDialogSettings();
		String searchUrl = settings.get(SEARCH_URL_ID + "." + getTaskRepository().getRepositoryUrl()); //$NON-NLS-1$
		if (searchUrl == null) {
			return false;
		}
		FilterDefinitionConverter converter = new FilterDefinitionConverter(getTaskRepository().getCharacterEncoding());
		this.workingCopy = converter.toFilter(client, searchUrl, false);
		return true;
	}

	@Override
	public void saveState() {
		String repoId = "." + getTaskRepository().getRepositoryUrl(); //$NON-NLS-1$
		IDialogSettings settings = getDialogSettings();
		settings.put(SEARCH_URL_ID + repoId, createQuery().getUrl());
	}

	private void setDateRange(DateFilter dateFilter, DatePicker startDatePicker, DatePicker endDatePicker) {
		if (dateFilter instanceof DateRangeFilter) {
			DateRangeFilter rangeFilter = (DateRangeFilter) dateFilter;

			if (rangeFilter.getFromDate() != null) {
				Calendar c1 = Calendar.getInstance();
				c1.setTime(rangeFilter.getFromDate());
				startDatePicker.setDate(c1);
			} else {
				startDatePicker.setDate(null);
			}

			if (rangeFilter.getToDate() != null) {
				Calendar c2 = Calendar.getInstance();
				c2.setTime(rangeFilter.getToDate());
				endDatePicker.setDate(c2);
			} else {
				endDatePicker.setDate(null);
			}
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (getSearchContainer() != null) {
			getSearchContainer().setPerformActionEnabled(true);
		}

		if (visible && firstTime) {
			firstTime = false;
			if (!client.getCache().hasDetails()) {
				// delay the execution so the dialog's progress bar is visible
				// when the attributes are updated
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						if (getControl() != null && !getControl().isDisposed()) {
							initializePage();
						}
					}
				});
			} else {
				// no remote connection is needed to get attributes therefore do
				// not use delayed execution to avoid flickering
				initializePage();
			}
		}
	}

	private void updateAttributesFromRepository(final boolean force) {
		if (!client.getCache().hasDetails() || force) {
			try {
				IRunnableWithProgress runnable = new IRunnableWithProgress() {
					// FIXME review error handling
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						JiraClient client = JiraClientFactory.getDefault().getJiraClient(getTaskRepository());
						try {
							client.getCache().refreshDetails(monitor);
						} catch (OperationCanceledException e) {
							throw new InterruptedException();
						} catch (JiraException e) {
							showWarning(NLS.bind( //
									Messages.JiraFilterDefinitionPage_Error_updating_attributes_X, e.getMessage()));
						} catch (Exception e) {
							String msg = NLS.bind( //
									Messages.JiraFilterDefinitionPage_Error_updating_attributes_X, e.getMessage());
							showWarning(msg);
							StatusHandler.log(new org.eclipse.core.runtime.Status(IStatus.ERROR,
									JiraUiPlugin.ID_PLUGIN, msg, e));
						}
					}

					private void showWarning(final String msg) {
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								JiraFilterDefinitionPage.this.setErrorMessage(msg);
							}
						});
					}
				};

				if (getContainer() != null) {
					getContainer().run(true, true, runnable);
				} else if (getSearchContainer() != null) {
					getSearchContainer().getRunnableContext().run(true, true, runnable);
				} else {
					IProgressService service = PlatformUI.getWorkbench().getProgressService();
					service.busyCursorWhile(runnable);
				}
			} catch (InvocationTargetException e) {
				return;
			} catch (InterruptedException e) {
				return;
			}
		}

		initializeContentProviders();
	}

	void updateCurrentProjects(Project[] projects) {
		this.fixFor.setInput(projects);
		this.components.setInput(projects);
		this.reportedIn.setInput(projects);
	}
}
