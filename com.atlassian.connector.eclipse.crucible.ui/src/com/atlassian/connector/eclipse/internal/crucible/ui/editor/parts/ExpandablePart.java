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

package com.atlassian.connector.eclipse.internal.crucible.ui.editor.parts;

import com.atlassian.connector.eclipse.internal.crucible.ui.IReviewAction;
import com.atlassian.connector.eclipse.internal.crucible.ui.IReviewActionListener;
import com.atlassian.connector.eclipse.internal.crucible.ui.editor.CrucibleReviewEditorPage;
import com.atlassian.connector.eclipse.ui.AtlassianImages;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.mylyn.internal.tasks.ui.editors.EditorUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;

import java.util.ArrayList;
import java.util.List;

/**
 * A UI part that is expandable like a tree
 * 
 * @author Shawn Minto
 */
public abstract class ExpandablePart {

	private Section commentSection;

	private boolean isExpanded;

	private final List<ExpandablePart> childrenParts;

	protected final CrucibleReviewEditorPage crucibleEditor;

	private IReviewActionListener actionListener;

	public ExpandablePart(CrucibleReviewEditorPage editor) {
		this.crucibleEditor = editor;
		childrenParts = new ArrayList<ExpandablePart>();
	}

	protected void addChildPart(ExpandablePart part) {
		childrenParts.add(part);
	}

	protected List<ExpandablePart> getChildrenParts() {
		return childrenParts;
	}

	public CrucibleReviewEditorPage getCrucibleEditor() {
		return crucibleEditor;
	}

	public Control createControl(Composite parent, final FormToolkit toolkit) {

		String headerText = getSectionHeaderText();

		int style = ExpandableComposite.LEFT_TEXT_CLIENT_ALIGNMENT;
		if (canExpand()) {
			style |= ExpandableComposite.TWISTIE;
		}

//		if (crucibleEditor == null) {
		style |= ExpandableComposite.EXPANDED;
//		}

		commentSection = toolkit.createSection(parent, style);
		commentSection.setText(headerText);
		commentSection.setTitleBarForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		GridData gd = GridDataFactory.fillDefaults().grab(true, false).create();
		if (!canExpand()) {
			gd.horizontalIndent = 9;
		}
		commentSection.setLayoutData(gd);

		final Composite actionsComposite = createSectionAnnotationsAndToolbar(commentSection, toolkit);

		final ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);

		ToolBar toolbarControl = toolBarManager.createControl(actionsComposite);
		toolkit.adapt(toolbarControl);

		if (commentSection.isExpanded() || crucibleEditor == null) {
			isExpanded = true;
			fillToolBar(toolBarManager, toolkit, isExpanded);
			if (canExpand()) {
				Composite composite = createSectionContents(commentSection, toolkit);
				commentSection.setClient(composite);
			}

		} else {
			fillToolBar(toolBarManager, toolkit, false);
			commentSection.addExpansionListener(new ExpansionAdapter() {
				@Override
				public void expansionStateChanged(ExpansionEvent e) {
					isExpanded = e.getState();
					fillToolBar(toolBarManager, toolkit, isExpanded);

					if (commentSection.getClient() == null) {
						try {
							if (crucibleEditor != null) {
								crucibleEditor.setReflow(false);
							}
							Composite composite = createSectionContents(commentSection, toolkit);

							if (crucibleEditor != null && crucibleEditor.getMenu() != null) {
								crucibleEditor.setMenu(composite, crucibleEditor.getMenu());
							}
							commentSection.setClient(composite);
						} finally {
							if (crucibleEditor != null) {
								crucibleEditor.setReflow(true);
							}
						}
						if (crucibleEditor != null) {
							crucibleEditor.reflow();
						}
					}
				}
			});
		}
		return commentSection;
	}

	protected boolean canExpand() {
		return true;
	}

	public Section getSection() {
		return commentSection;
	}

	private void fillToolBar(ToolBarManager toolbarManager, FormToolkit toolkit, boolean expanded) {
		List<IReviewAction> toolbarActions = getToolbarActions(expanded);

//		for (Control control : actionsComposite.getChildren()) {
//			if (control instanceof ImageHyperlink) {
//				control.setMenu(null);
//				control.dispose();
//			}
//		}

		toolbarManager.removeAll();

		if (toolbarActions != null) {

			for (final IReviewAction action : toolbarActions) {
				action.setActionListener(actionListener);
				toolbarManager.add(action);
//				ImageHyperlink link = createActionHyperlink(actionsComposite, toolkit, action);
//				if (!action.isEnabled()) {
//					link.setEnabled(false);
//				}
			}
		}
		toolbarManager.markDirty();
		toolbarManager.update(true);
//		actionsComposite.getParent().layout();
	}

	protected ImageHyperlink createActionHyperlink(Composite actionsComposite, FormToolkit toolkit, final IAction action) {
		ImageHyperlink link = toolkit.createImageHyperlink(actionsComposite, SWT.NONE);
		if (action.getImageDescriptor() != null) {
			link.setImage(AtlassianImages.getImage(action.getImageDescriptor()));
		} else {
			link.setText(action.getText());
		}
		link.setToolTipText(action.getToolTipText());
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				action.run();
			}
		});
		return link;
	}

	/**
	 * @return A composite that image hyperlinks can be placed on
	 */
	protected Composite createSectionAnnotationsAndToolbar(Section section, FormToolkit toolkit) {

		Composite toolbarComposite = toolkit.createComposite(section);
		section.setTextClient(toolbarComposite);
		RowLayout rowLayout = new RowLayout();
		rowLayout.marginTop = 0;
		rowLayout.marginBottom = 0;
		toolbarComposite.setLayout(rowLayout);

		Composite annotationsComposite = toolkit.createComposite(toolbarComposite);

		rowLayout = new RowLayout();
		rowLayout.marginTop = 0;
		rowLayout.marginBottom = 0;
		rowLayout.spacing = 0;

		annotationsComposite.setLayout(rowLayout);

		ImageDescriptor annotationImage = getAnnotationImage();
		if (annotationImage != null) {
			Label imageLabel = toolkit.createLabel(annotationsComposite, "");
			imageLabel.setImage(AtlassianImages.getImage(annotationImage));
		}

		String annotationsText = getAnnotationText();
		if (annotationsText == null) {
			annotationsText = "";
		}
		toolkit.createLabel(annotationsComposite, annotationsText);

		createCustomAnnotations(annotationsComposite, toolkit);

//		Composite actionsComposite = toolkit.createComposite(toolbarComposite);
//		actionsComposite.setBackground(null);
//		rowLayout = new RowLayout();
//		rowLayout.marginTop = 0;
//		rowLayout.marginBottom = 0;
//		actionsComposite.setLayout(rowLayout);

		return toolbarComposite;
	}

	public boolean isExpanded() {
		return isExpanded && areChildrenExpanded();
	}

	private boolean areChildrenExpanded() {
		for (ExpandablePart child : childrenParts) {
			if (!child.isExpanded()) {
				return false;
			}
		}
		return true;
	}

	public void setExpanded(boolean expanded) {
		if (expanded != commentSection.isExpanded()) {
			EditorUtil.toggleExpandableComposite(expanded, commentSection);
		}
		for (ExpandablePart child : childrenParts) {
			child.setExpanded(expanded);
		}
	}

	public void hookCustomActionRunListener(IReviewActionListener actionRunListener) {
		this.actionListener = actionRunListener;
	}

	public IReviewActionListener getActionListener() {
		return actionListener;
	}

	protected void createCustomAnnotations(Composite toolbarComposite, FormToolkit toolkit) {
		// default do nothing
	}

	protected abstract List<IReviewAction> getToolbarActions(boolean expanded);

	protected abstract String getAnnotationText();

	protected abstract ImageDescriptor getAnnotationImage();

	protected abstract String getSectionHeaderText();

	protected abstract Composite createSectionContents(Section section, FormToolkit toolkit);

}
