package com.atlassian.connector.eclipse.internal.directclickthrough.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.osgi.framework.BundleContext;

import com.atlassian.connector.eclipse.internal.directclickthrough.servlet.DirectClickThroughServlet;

/**
 * The activator class controls the plug-in life cycle
 */
public class DirectClickThroughUiPlugin extends AbstractUIPlugin {

	private Server embeddedServer;

	// The plug-in ID
	public static final String PLUGIN_ID = "com.atlassian.connector.eclipse.directclickthrough.ui";

	// The shared instance
	private static DirectClickThroughUiPlugin plugin;
	
	public static class EarlyStartup implements IStartup {

		public void earlyStartup() {
			// force early loading
		}
	}
	
	/**
	 * The constructor
	 */
	public DirectClickThroughUiPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if(!IDirectClickThroughPreferenceConstants.ENABLED.equals(event.getProperty())) { 
					return;
				}
				boolean enabled = Boolean.parseBoolean((String) event.getNewValue());
				if (enabled) {
					startEmbeddedServer();
				} else {
					stopEmbeddedServer();
				}
			}
		});
		
		if (getPreferenceStore().getBoolean(IDirectClickThroughPreferenceConstants.ENABLED)) {
			startEmbeddedServer();
		}
	}

	private void startEmbeddedServer() {
		if (embeddedServer != null) {
			stopEmbeddedServer();
		}
		
		Job serverJob = new Job("Start Embedded Web Server") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					embeddedServer = new Server();
					Connector connector = new SocketConnector();
					connector.setHost("127.0.0.1");
					connector.setPort(getPreferenceStore().getInt(IDirectClickThroughPreferenceConstants.PORT_NUMBER));
					embeddedServer.addConnector(connector);
					
					Context context = new Context(embeddedServer, "/", Context.NO_SESSIONS | Context.NO_SECURITY);
					context.addServlet(new ServletHolder(new DirectClickThroughServlet()), "/*");
					
					embeddedServer.start();
					return new Status(IStatus.OK, DirectClickThroughUiPlugin.PLUGIN_ID, "Started embedded Direct Click Through server");
				} catch (Exception e) {
					return new Status(IStatus.ERROR, DirectClickThroughUiPlugin.PLUGIN_ID, 
							"Unable to run embedded web server, Direct Click Through will not be available", e);
				}
			}
		};
		serverJob.schedule();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		if (getPreferenceStore().getBoolean(IDirectClickThroughPreferenceConstants.ENABLED)) {
			stopEmbeddedServer();
		}
		plugin = null;
		super.stop(context);
	}

	private void stopEmbeddedServer() {
		try {
			if (embeddedServer != null) {
				embeddedServer.stop();
				embeddedServer = null;
			}
		} catch(Exception e) {
			StatusHandler.log(new Status(IStatus.WARNING, DirectClickThroughUiPlugin.PLUGIN_ID, 
					"Unabled to stop embedded Direct Click Through server"));
		}
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static DirectClickThroughUiPlugin getDefault() {
		return plugin;
	}

}