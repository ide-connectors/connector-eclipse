package com.atlassian.connector.eclipse.internal.directclickthrough.servlet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.jira.core.JiraCorePlugin;
import org.eclipse.mylyn.internal.provisional.commons.ui.WorkbenchUtil;
import org.eclipse.mylyn.internal.tasks.ui.OpenRepositoryTaskJob;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.atlassian.connector.eclipse.internal.crucible.core.CrucibleCorePlugin;
import com.atlassian.connector.eclipse.internal.directclickthrough.ui.DirectClickThroughImages;
import com.atlassian.connector.eclipse.internal.directclickthrough.ui.DirectClickThroughUiPlugin;
import com.atlassian.connector.eclipse.ui.team.TeamUiUtils;
import com.atlassian.theplugin.commons.util.MiscUtil;
import com.atlassian.theplugin.commons.util.StringUtil;

@SuppressWarnings("serial")
public class DirectClickThroughServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
	
		final String path = req.getPathInfo();
		
		if ("/icon".equals(path)) {
			writeIcon(resp);
			return;
		} 
		
		bringEclipseToFront();
		
		if ("/file".equals(path)) {
			writeIcon(resp);
			handleOpenFileRequest(req);
		} else if ("/issue".equals(path)) {
			writeIcon(resp);
			handleOpenIssueRequest(req);
		} else if ("/review".equals(path)) {
			writeIcon(resp);
			handleOpenReviewRequest(req);
		} else {
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
			StatusHandler.log(new Status(IStatus.WARNING, DirectClickThroughUiPlugin.PLUGIN_ID, 
					NLS.bind("Direct Click Through server received unknown command: [{0}]", path)));
		}
	}

	private void handleOpenFileRequest(final HttpServletRequest req) {
		final String file = StringUtil.removePrefixSlashes(req.getParameter("file"));
		final String path = StringUtil.removeSuffixSlashes(req.getParameter("path"));
		final String vcsRoot = StringUtil.removeSuffixSlashes(req.getParameter("vcs_root"));
		final String line = req.getParameter("line");
		if (file != null) {
			openRequestedFile(path, file, vcsRoot, line);
		}
	}

	@SuppressWarnings("restriction")
	private void openRequestedFile(final String path, final String file, final String vcsRoot, final String line) {
		final List<IResource> resources = MiscUtil.buildArrayList();
		final int[] matchLevel = new int[] {0};
		
		Job searchAndOpenJob = new Job("Search and open files requested using Direct Click Through") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Searching for files", IProgressMonitor.UNKNOWN);
				try {
					ResourcesPlugin.getWorkspace().getRoot().accept(new IResourceVisitor() {
						private int matchingLastSegments(IPath firstPath, IPath secondPath) {
							int firstPathLen = firstPath.segmentCount();
							int secondPathLen = secondPath.segmentCount();
							int max = Math.min(firstPathLen, secondPathLen);
							int count = 0;
							for (int i = 1; i <= max; i++) {
								if (!firstPath.segment(firstPathLen - i).equals(secondPath.segment(secondPathLen - i))) {
									return count;
								}
								count++;
							}
							return count;
						}
						
						public boolean visit(IResource resource) throws CoreException {
							if (!(resource instanceof IFile)) {
								return true; // skip it if it's not a file, but check its members
							}
							
				            int matchCount = matchingLastSegments(new Path(path == null ? file : path + "/" + file), resource.getLocation());
				            if (matchCount > 0) {
				            	if (matchCount > matchLevel[0]) {
				            		resources.clear();
				            		matchLevel[0] = matchCount;
				            	}
				            	if (matchCount == matchLevel[0]) {
				            		resources.add(resource);
				            	}
				            }
				            
							return true; // visit also members of this resource
						}
					});
				} catch (CoreException e) {
					return new Status(IStatus.ERROR, DirectClickThroughUiPlugin.PLUGIN_ID, "Direct Click Through server failed to find matching files", e);
				} finally {
					monitor.done();
				}
				return new Status(IStatus.OK, DirectClickThroughUiPlugin.PLUGIN_ID, "Search finished");
			}
		};
		
		searchAndOpenJob.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (!event.getResult().isOK()) {
					return;
				}
		        if (resources.size() > 0) {
		        	if (resources.size() == 1) {
		        		openFile(resources.iterator().next(), line);
		        	} else {
		        		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
							public void run() {
								final ListDialog ld = new ListDialog(WorkbenchUtil.getShell());
				    			ld.setAddCancelButton(true);
				    			ld.setContentProvider(new ArrayContentProvider());
				    			ld.setInput(resources.toArray(new IResource[resources.size()]));
				    			ld.setTitle("Select File to Open");
				    			ld.setLabelProvider(new LabelProvider() {
				    				@Override
				    				public String getText(Object element) {
				    					if (element instanceof IResource) {
				    						return ((IResource) element).getFullPath().toString();
				    					}
				    					return super.getText(element);
				    				}
				    			});
				    			ld.setMessage("Direct Click Through request matches multiple files.\n"
				    					+ "Please select apropriate file that will be open in editor.");

				    			if (ld.open() == Window.OK) {
				    				final Object[] result = ld.getResult();
				    				if (result != null && result.length > 0) {
				    					for(Object selected : result) {
				    						if (selected instanceof IResource) {
				    							openFile((IResource) selected, line);
				    						}
				    					}
				    				}
				    			}
							}
						});
		        	}
		        } else {
			        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
						public void run() {
							new MessageDialog(WorkbenchUtil.getShell(), "Unable to open file", null,
					        		"Unable to open file requested using Direct Click Through. Either file is missing or all projects are closed.",
					    			MessageDialog.INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0).open();
						}
					});
		        }
			}
		});
		
		searchAndOpenJob.schedule();
    }

	private void openFile(final IResource resource, final String line) {
    	Assert.isNotNull(resource);

    	final IEditorPart editor = TeamUiUtils.openLocalResource(resource);
    	
    	if (line != null && line.length() > 0 && editor instanceof ITextEditor) {
			try {
				final int l = Integer.parseInt(line);
				if (Display.getCurrent() != null) {
					gotoLine((ITextEditor) editor, l);
				} else {
					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
						public void run() {
							gotoLine((ITextEditor) editor, l);
						}
					});
				}
			} catch (NumberFormatException e) {
				StatusHandler.log(new Status(IStatus.WARNING, DirectClickThroughUiPlugin.PLUGIN_ID,
						NLS.bind("Wrong line number format when requesting to open file in the IDE [{0}]", line), e));
			}
			
    	}
	}

	/**
	 * Jumps to the given line.
	 *
	 * @param line the line to jump to
	 */
	private void gotoLine(ITextEditor editor, int line) {
		IDocumentProvider provider= editor.getDocumentProvider();
		IDocument document= provider.getDocument(editor.getEditorInput());
		try {

			int start= document.getLineOffset(line);
			editor.selectAndReveal(start, 0);

			IWorkbenchPage page= editor.getSite().getPage();
			page.activate(editor);
		} catch (BadLocationException x) {
			// ignore
		}
	}

	@SuppressWarnings("restriction")
	private void handleOpenReviewRequest(final HttpServletRequest req) {
		final String reviewKey = req.getParameter("review_key");
		final String serverUrl = req.getParameter("server_url");

		if (reviewKey == null || serverUrl == null) {
			StatusHandler.log(new Status(IStatus.WARNING, DirectClickThroughUiPlugin.PLUGIN_ID, "Cannot open issue: review_key or server_url parameter is null"));
		}

		try {
			new OpenRepositoryTaskJob(CrucibleCorePlugin.CONNECTOR_KIND, serverUrl, reviewKey, null, null).schedule();
		} catch(NoClassDefFoundError e) {
			StatusHandler.log(new Status(IStatus.ERROR, DirectClickThroughUiPlugin.PLUGIN_ID, 
					"Direct Click Through failed to open review because Atlassian Crucible & FishEye Integration is missing"));
		}
	}

	@SuppressWarnings("restriction")
	private void handleOpenIssueRequest(final HttpServletRequest req) {
		final String issueKey = req.getParameter("issue_key");
		final String serverUrl = req.getParameter("server_url");
		
		if (issueKey == null || serverUrl == null) {
			StatusHandler.log(new Status(IStatus.WARNING, DirectClickThroughUiPlugin.PLUGIN_ID, "Cannot open issue: issue_key or server_url parameter is null"));
		}

		try {
			new OpenRepositoryTaskJob(JiraCorePlugin.CONNECTOR_KIND, serverUrl, issueKey, null, null).schedule();
		} catch (NoClassDefFoundError e) {
			StatusHandler.log(new Status(IStatus.ERROR, DirectClickThroughUiPlugin.PLUGIN_ID, 
				"Direct Click Through failed to open issue because Mylyn JIRA Connector is missing"));
		}
	}

	private static void bringEclipseToFront() {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				for (Shell shell : PlatformUI.getWorkbench().getDisplay().getShells()) {
					shell.setVisible(true);
					shell.setActive();
					shell.setFocus();
					shell.setMinimized(false);
					shell.forceActive();
					shell.forceFocus();
				}
			}
		});
		/*
		WindowManager.getInstance().getFrame(project).setVisible(true);

		String osName = System.getProperty("os.name");
		osName = osName.toLowerCase();

		if (osName.contains("windows") || osName.contains("mac os x")) {
			WindowManager.getInstance().getFrame(project).setAlwaysOnTop(true);
			WindowManager.getInstance().getFrame(project).setAlwaysOnTop(false);

		} else { //for linux
			WindowManager.getInstance().getFrame(project).toFront();
		}

		// how to set focus???
		WindowManager.getInstance().getFrame(project).setFocusable(true);
		WindowManager.getInstance().getFrame(project).setFocusableWindowState(true);
		WindowManager.getInstance().getFrame(project).requestFocus();
		WindowManager.getInstance().getFrame(project).requestFocusInWindow();
		*/
	}

	private void writeIcon(final HttpServletResponse response) throws IOException {
		InputStream icon = new BufferedInputStream(
			new URL(DirectClickThroughImages.BASE_URL, DirectClickThroughImages.PATH_ECLIPSE).openStream());
		try {
			response.setContentType("image/gif");
			response.setStatus(HttpServletResponse.SC_OK);
			
			OutputStream output = response.getOutputStream();
			for(int b=icon.read(); b!=-1; b=icon.read()) {
				output.write(b);
			}
		} finally {
			try { icon.close(); } catch(Exception e) { /* ignore */ }
		}
	}
}