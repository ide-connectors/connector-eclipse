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

package com.atlassian.connector.eclipse.internal.crucible.ui.annotations;

import com.atlassian.connector.eclipse.internal.crucible.ui.CrucibleUiPlugin;
import com.atlassian.connector.eclipse.ui.team.CrucibleFile;
import com.atlassian.connector.eclipse.ui.team.TeamUiUtils;
import com.atlassian.theplugin.commons.crucible.ValueNotYetInitialized;
import com.atlassian.theplugin.commons.crucible.api.model.CrucibleFileInfo;
import com.atlassian.theplugin.commons.crucible.api.model.Review;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.AnnotationBarHoverManager;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to manage the annotation model for the open editors
 * 
 * @author Shawn Minto
 */
public final class CrucibleAnnotationModelManager {

	private static final Map<ITextEditor, IAnnotationHover> EDITOR_TO_HOVER_MAP = new HashMap<ITextEditor, IAnnotationHover>();

	private CrucibleAnnotationModelManager() {
		// ignore
	}

	private static final Object CRUCIBLE_ANNOTATION_MODEL_KEY = new Object();

	public static void attach(ITextEditor editor) {
		IEditorInput editorInput = editor.getEditorInput();
		CrucibleFile crucibleFile = TeamUiUtils.getCorrespondingCrucibleFileFromEditorInput(editorInput,
				CrucibleUiPlugin.getDefault().getActiveReviewManager().getActiveReview());

		attach(editor, crucibleFile, CrucibleUiPlugin.getDefault().getActiveReviewManager().getActiveReview());
	}

	public static void attach(ITextEditor editor, CrucibleFile crucibleFile, Review review) {
		if (!CrucibleUiPlugin.getDefault().getActiveReviewManager().isReviewActive() || crucibleFile == null
				|| review == null || CrucibleUiPlugin.getDefault().getActiveReviewManager().getActiveReview() != review) {
			return;
		}
		IDocumentProvider documentProvider = editor.getDocumentProvider();
		IEditorInput editorInput = editor.getEditorInput();
		if (documentProvider == null) {
			return;
		}
		IAnnotationModel annotationModel = documentProvider.getAnnotationModel(editorInput);
		if (!(annotationModel instanceof IAnnotationModelExtension)) {
			// we need to piggyback on another annotation mode
			return;
		}
		IAnnotationModelExtension annotationModelExtension = (IAnnotationModelExtension) annotationModel;

		IDocument document = documentProvider.getDocument(editorInput);

		IAnnotationModel crucibleAnnotationModel = annotationModelExtension.getAnnotationModel(CRUCIBLE_ANNOTATION_MODEL_KEY);
		if (crucibleAnnotationModel == null) {
			crucibleAnnotationModel = new CrucibleAnnotationModel(editor, editorInput, document, crucibleFile, review);
			annotationModelExtension.addAnnotationModel(CRUCIBLE_ANNOTATION_MODEL_KEY, crucibleAnnotationModel);

			addAnnotationHover(editor);

		} else if (crucibleAnnotationModel instanceof CrucibleAnnotationModel) {
			((CrucibleAnnotationModel) crucibleAnnotationModel).updateCrucibleFile(crucibleFile, review);
		}

	}

	private static void addAnnotationHover(ITextEditor editor) {

		if (editor instanceof AbstractTextEditor) {
			AbstractTextEditor textEditor = (AbstractTextEditor) editor;

			try {
				Method getSourceViewer = AbstractTextEditor.class.getDeclaredMethod("getSourceViewer");
				getSourceViewer.setAccessible(true);
				SourceViewer viewer = (SourceViewer) getSourceViewer.invoke(textEditor);

				if (viewer != null) {
					Field hoverManager = SourceViewer.class.getDeclaredField("fVerticalRulerHoveringController");
					hoverManager.setAccessible(true);
					AnnotationBarHoverManager manager = (AnnotationBarHoverManager) hoverManager.get(viewer);
					if (manager != null) {

						Field annotationHover = AnnotationBarHoverManager.class.getDeclaredField("fAnnotationHover");
						annotationHover.setAccessible(true);
						IAnnotationHover hover = (IAnnotationHover) annotationHover.get(manager);

						annotationHover.set(manager, new CrucibleAnnotationHover(hover));

						EDITOR_TO_HOVER_MAP.put(editor, hover);
					}
				}

			} catch (Exception e) {
				StatusHandler.log(new Status(IStatus.ERROR, CrucibleUiPlugin.PLUGIN_ID,
						"Unable to attach custom annotation hover", e));
			}

		}
	}

	private static void removeAnnotationHover(ITextEditor editor) {

		if (editor instanceof AbstractTextEditor) {
			AbstractTextEditor textEditor = (AbstractTextEditor) editor;

			IAnnotationHover annotationHover = EDITOR_TO_HOVER_MAP.remove(editor);
			if (annotationHover != null) {

				try {
					Method getSourceViewer = AbstractTextEditor.class.getDeclaredMethod("getSourceViewer");
					getSourceViewer.setAccessible(true);
					SourceViewer viewer = (SourceViewer) getSourceViewer.invoke(textEditor);

					if (viewer != null) {
						Field hoverManager = SourceViewer.class.getDeclaredField("fVerticalRulerHoveringController");
						hoverManager.setAccessible(true);
						AnnotationBarHoverManager manager = (AnnotationBarHoverManager) hoverManager.get(viewer);
						if (manager != null) {

							Field annotationHoverField = AnnotationBarHoverManager.class.getDeclaredField("fAnnotationHover");
							annotationHoverField.setAccessible(true);
							IAnnotationHover hover = (IAnnotationHover) annotationHoverField.get(manager);

							if (hover instanceof CrucibleAnnotationHover) {
								annotationHoverField.set(manager, annotationHover);
								((CrucibleAnnotationHover) hover).dispose();
							}

						}
					}

				} catch (Exception e) {
					StatusHandler.log(new Status(IStatus.ERROR, CrucibleUiPlugin.PLUGIN_ID,
							"Unable to detach custom annotation hover", e));
				}
			}

		}
	}

	public static void attachAllOpenEditors() {
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference editorReference : page.getEditorReferences()) {
					IWorkbenchPart editorPart = editorReference.getPart(false);
					if (editorPart instanceof ITextEditor) {
						attach((ITextEditor) editorPart);
					}
				}
			}
		}
	}

	public static void detach(ITextEditor editor) {
		IDocumentProvider documentProvider = editor.getDocumentProvider();
		IEditorInput editorInput = editor.getEditorInput();
		if (documentProvider == null) {
			return;
		}
		IAnnotationModel annotationModel = documentProvider.getAnnotationModel(editorInput);
		if (!(annotationModel instanceof IAnnotationModelExtension)) {
			// we need to piggyback on another annotation mode
			return;
		}

		IAnnotationModelExtension annotationModelExtension = (IAnnotationModelExtension) annotationModel;
		IAnnotationModel crucibleAnnotationModel = annotationModelExtension.getAnnotationModel(CRUCIBLE_ANNOTATION_MODEL_KEY);
		if (crucibleAnnotationModel instanceof CrucibleAnnotationModel) {
			((CrucibleAnnotationModel) crucibleAnnotationModel).clear();
		}
		annotationModelExtension.removeAnnotationModel(CRUCIBLE_ANNOTATION_MODEL_KEY);

		removeAnnotationHover(editor);
	}

	public static void dettachAllOpenEditors() {
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference editorReference : page.getEditorReferences()) {
					IWorkbenchPart editorPart = editorReference.getPart(false);
					if (editorPart instanceof ITextEditor) {
						detach((ITextEditor) editorPart);
					}
				}
			}
		}
	}

	public static void updateAllOpenEditors(Review activeReview) {
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference editorReference : page.getEditorReferences()) {
					IWorkbenchPart editorPart = editorReference.getPart(false);
					if (editorPart instanceof ITextEditor) {
						update((ITextEditor) editorPart, activeReview);
					}
				}
			}
		}
	}

	private static void update(ITextEditor editor, Review activeReview) {
		if (!CrucibleUiPlugin.getDefault().getActiveReviewManager().isReviewActive() || activeReview == null
				|| CrucibleUiPlugin.getDefault().getActiveReviewManager().getActiveReview() != activeReview) {
			return;
		}
		IDocumentProvider documentProvider = editor.getDocumentProvider();
		IEditorInput editorInput = editor.getEditorInput();
		if (documentProvider == null) {
			return;
		}
		IAnnotationModel iAnnotationModel = documentProvider.getAnnotationModel(editorInput);
		if (!(iAnnotationModel instanceof IAnnotationModelExtension)) {
			// we need to piggyback on another annotation mode
			return;
		}
		IAnnotationModelExtension annotationModelExtension = (IAnnotationModelExtension) iAnnotationModel;

		IAnnotationModel annotationModel = annotationModelExtension.getAnnotationModel(CRUCIBLE_ANNOTATION_MODEL_KEY);
		if (annotationModel instanceof CrucibleAnnotationModel) {
			CrucibleAnnotationModel crucibleAnnotationModel = (CrucibleAnnotationModel) annotationModel;

			CrucibleFile crucibleFile = crucibleAnnotationModel.getCrucibleFile();
			if (crucibleFile != null) {

				try {
					CrucibleFileInfo newFileInfo = activeReview.getFileByPermId(crucibleFile.getCrucibleFileInfo()
							.getPermId());
					if (newFileInfo != null) {
						crucibleAnnotationModel.updateCrucibleFile(new CrucibleFile(newFileInfo,
								crucibleFile.isOldFile()), activeReview);
					}
				} catch (ValueNotYetInitialized e) {
					StatusHandler.log(new Status(IStatus.ERROR, CrucibleUiPlugin.PLUGIN_ID, e.getMessage(), e));
				}
			}
		} else {
			// backup for if a file has been added and it is already open
			attach(editor);
		}

	}

}