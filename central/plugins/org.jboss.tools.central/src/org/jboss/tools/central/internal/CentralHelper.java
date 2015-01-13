/*************************************************************************************
 * Copyright (c) 2008-2015 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.central.internal;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.central.JBossCentralActivator;
import org.jboss.tools.foundation.core.ecf.URLTransportUtility;
import org.jboss.tools.foundation.core.properties.PropertiesHelper;
import org.jboss.tools.project.examples.internal.UnArchiver;

public class CentralHelper {
	
	public static final String JBOSS_CENTRAL_WEBPAGE_URL_KEY = "jboss.central.webpage.url";

	private CentralHelper() {}
	
	public static String getCentralUrl(IProgressMonitor monitor) throws CoreException {
		String remoteUrl = System.getProperty(JBOSS_CENTRAL_WEBPAGE_URL_KEY);
		if (remoteUrl == null) {
			remoteUrl = PropertiesHelper.getPropertiesProvider().getValue(JBOSS_CENTRAL_WEBPAGE_URL_KEY, "http://central-fredapp.rhcloud.com/");
		}
		return getCentralUrl(remoteUrl, monitor);
	}

	public static String getCentralUrl(String remoteUrl, IProgressMonitor monitor) throws CoreException {
		StringBuilder url = new StringBuilder();
		if (remoteUrl.endsWith(".zip")) {
			//download it
			URI uri;
			try {
				uri = new URI(remoteUrl);
			} catch (URISyntaxException e) {
				IStatus status = new Status(IStatus.ERROR, JBossCentralActivator.PLUGIN_ID, "Central page has an invalid URL", e);
				throw new CoreException(status);
			}
			Path zip;
			if (uri.getScheme() == null){
				zip = Paths.get(remoteUrl).toAbsolutePath();
			}
			else if ("file".equals(uri.getScheme())){
				zip = Paths.get(uri).toAbsolutePath();
			} else {
				//download it if needed
				zip = downloadIfNeeded(uri, monitor);
			}
			Path centralFolder =  getCentralFolder();
			Path localCentralPage;
			try {
				localCentralPage = extractIfNeeded(zip, centralFolder, false, monitor);
			} catch (IOException e) {
				IStatus status = new Status(IStatus.ERROR, JBossCentralActivator.PLUGIN_ID, "Unable to open "+zip, e);
				throw new CoreException(status);
			}
			url.append(localCentralPage);
		} else {
			url.append(remoteUrl);
		}
		String _url = url.toString();
		if (!_url.endsWith("index.html")){
			if (!_url.endsWith("/")) {
				url.append("/");
			}
			url.append("index.html"); 
		}
		return url.toString();
	}

	
	private static Path getCentralFolder() {
		IPath location = JBossCentralActivator.getDefault().getStateLocation();
		String path = location.append("versions").toOSString();
		return Paths.get(path);
	}

	static Path downloadIfNeeded(URI uri, IProgressMonitor monitor) throws CoreException {
		String url = uri.toString();
		int lifespan = URLTransportUtility.CACHE_FOREVER;//url.contains("-SNAPSHOT")?URLTransportUtility.CACHE_UNTIL_EXIT:;
		File zip = new URLTransportUtility().getCachedFileForURL(url, "Download central", lifespan, monitor);
		return zip.toPath();
	}

	private static Path extractIfNeeded(Path zip, Path centralFolder, boolean overwrite, IProgressMonitor monitor) throws IOException {
		String sha1 = DigestUtils.sha1(zip).substring(0, 6);
		Path destinationFolder = centralFolder.resolve(sha1);
		//if already extracted :
		if (overwrite) {
			FileUtils.deleteDirectory(destinationFolder.toFile());
		}
		boolean extracted = Files.isDirectory(destinationFolder);
		if (!extracted) {
			UnArchiver unarchiver = UnArchiver.create(zip.toFile(),  destinationFolder.toFile());
			unarchiver.extract(monitor);
		}
		Path extractedFile = destinationFolder.resolve("index.html");
		if (!Files.isRegularFile(extractedFile)) {
			if (extracted && !overwrite) {
				extractIfNeeded(zip, centralFolder, true, monitor);
			} else {
				throw new IOException(extractedFile + " can not be found");
			}
		}
		return extractedFile ;
	}

}
