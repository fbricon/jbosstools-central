/*************************************************************************************
 * Copyright (c) 2012, 2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.central.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.central.JBossCentralActivator;
import org.jboss.tools.central.ShowJBossCentral;
import org.jboss.tools.central.actions.RefreshJBossBuzzHandler;
import org.jboss.tools.central.actions.RefreshJBossTutorialsHandler;
import org.jboss.tools.central.editors.JBossCentralEditor;
import org.jboss.tools.central.jobs.RefreshBuzzJob;
import org.jboss.tools.central.jobs.RefreshTutorialsJob;
import org.jboss.tools.central.model.FeedsEntry;
import org.jboss.tools.central.preferences.PreferenceKeys;
import org.jboss.tools.project.examples.model.ProjectExample;
import org.jboss.tools.project.examples.model.ProjectExampleCategory;
import org.jboss.tools.test.util.JobUtils;
import org.jboss.tools.test.util.xpl.EditorTestHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author snjeza
 * 
 */
public class CentralTest {

	private static JBossCentralEditor editor;
	public static final String ORG_ECLIPSE_UI_INTERNAL_INTROVIEW = "org.eclipse.ui.internal.introview";


	@BeforeClass
	public static void init() throws Exception {
		final IWorkbenchWindow window = PlatformUI
				.getWorkbench().getActiveWorkbenchWindow();
		final IWorkbenchPage page = window.getActivePage();
		IViewPart welcomeView = page.findView(ORG_ECLIPSE_UI_INTERNAL_INTROVIEW);
		if (welcomeView != null) {
			page.hideView(welcomeView);
		}
		editor = JBossCentralActivator.getJBossCentralEditor(true);
		new RefreshJBossTutorialsHandler().execute(null);
		new RefreshJBossBuzzHandler().execute(null);
	}

	@AfterClass
	public static void close() throws Exception {
		EditorTestHelper.closeAllEditors();
	}

	@Test
	public void testEditorOpen() throws Exception {
		assertNotNull("The Red Hat Central editor isn't open", editor);
	}

	@Test
	public void testTutorials() throws Exception {
		waitForJobs();
		Map<ProjectExampleCategory, List<ProjectExample>> categories = RefreshTutorialsJob.INSTANCE
				.getTutorialCategories();
		assertNotNull(categories);
		assertFalse("No tutorial found", categories.isEmpty());
	}

	@Test
	public void testBuzz() throws Exception {
		waitForJobs();
		List<FeedsEntry> buzz = RefreshBuzzJob.INSTANCE.getEntries();
		assertFalse("No buzz found", buzz.isEmpty());
	}

	@Test
	public void testCachingBuzz() throws Exception {
		waitForJobs();
		assertTrue("Buzz entries aren't cached", RefreshBuzzJob.INSTANCE
				.getCacheFile().exists());
	}
	
	@Test
	public void testShowOnStartup() throws Exception {
		EditorTestHelper.closeAllEditors();
		assertTrue("The Show On Startup property isn't set by default",
				JBossCentralActivator.getDefault().showJBossCentralOnStartup());
		new ShowJBossCentral().earlyStartup();
		waitForJobs();
		JobUtils.delay(1000);
		
		assertTrue("The Red Hat Central editor isn't open by default",
				hasOpenEditor());
		IEclipsePreferences prefs = JBossCentralActivator.getDefault()
				.getPreferences();
		prefs.putBoolean(PreferenceKeys.SHOW_JBOSS_CENTRAL_ON_STARTUP,
				false);
		assertFalse("The Show On Startup property isn't changed",
				JBossCentralActivator.getDefault().showJBossCentralOnStartup());
		EditorTestHelper.closeAllEditors();
		new ShowJBossCentral().earlyStartup();

		waitForJobs();
		
		JobUtils.delay(1000);
		assertFalse(
				"The Red Hat Central editor is open when the Show On Startup property is unchecked",
				hasOpenEditor());
		prefs.putBoolean(
				PreferenceKeys.SHOW_JBOSS_CENTRAL_ON_STARTUP,
				PreferenceKeys.SHOW_JBOSS_CENTRAL_ON_STARTUP_DEFAULT_VALUE);
	}

	private boolean hasOpenEditor() {
		IWorkbenchWindow[] windows = PlatformUI.getWorkbench()
				.getWorkbenchWindows();
		for (int i = 0; i < windows.length; i++) {
			IWorkbenchPage[] pages = windows[i].getPages();
			for (int j = 0; j < pages.length; j++) {
				IEditorReference[] editorReferences = pages[j]
						.getEditorReferences();
				if (editorReferences.length > 0) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static void waitForJobs() {

		try {
			PlatformUI.getWorkbench().getProgressService()
					.busyCursorWhile(new IRunnableWithProgress() {

						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							Job.getJobManager().join(
									JBossCentralActivator.JBOSS_CENTRAL_FAMILY,
									new NullProgressMonitor());
						}
					});
		} catch (InvocationTargetException e) {
			fail(e.getMessage());
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
	}

}
