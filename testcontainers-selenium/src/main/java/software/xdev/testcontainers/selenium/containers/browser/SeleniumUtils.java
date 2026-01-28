/*
 * Copyright © 2024 XDEV Software (https://xdev.software)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package software.xdev.testcontainers.selenium.containers.browser;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Utility methods for Selenium.
 * <p>
 * Forked from Testcontainers and enhanced with caching.
 * </p>
 */
public final class SeleniumUtils
{
	private static final Logger LOG = LoggerFactory.getLogger(SeleniumUtils.class);
	
	// as of 2026-01
	public static final String DEFAULT_SELENIUM_VERSION = "4.40.0";
	private static String cachedVersion;
	
	private SeleniumUtils()
	{
	}
	
	/**
	 * Based on the JARs detected on the classpath, determine which version of selenium-api is available.
	 *
	 * @return the detected version of Selenium API, or DEFAULT_SELENIUM_VERSION if it could not be determined
	 */
	public static String getClasspathSeleniumVersion()
	{
		if(cachedVersion != null)
		{
			return cachedVersion;
		}
		cachedVersion = determineClasspathSeleniumVersion();
		return cachedVersion;
	}
	
	public static synchronized String determineClasspathSeleniumVersion()
	{
		if(cachedVersion != null)
		{
			return cachedVersion;
		}
		
		final Set<String> seleniumVersions = new HashSet<>();
		try
		{
			final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			final Enumeration<URL> manifests = classLoader.getResources("META-INF/MANIFEST.MF");
			
			while(manifests.hasMoreElements())
			{
				final URL manifestURL = manifests.nextElement();
				try(final InputStream is = manifestURL.openStream())
				{
					final Manifest manifest = new Manifest();
					manifest.read(is);
					
					final String seleniumVersion = getSeleniumVersionFromManifest(manifest);
					if(seleniumVersion != null)
					{
						seleniumVersions.add(seleniumVersion);
						LOG.info("Selenium API version {} detected on classpath", seleniumVersion);
					}
				}
			}
		}
		catch(final Exception e)
		{
			LOG.debug("Failed to determine Selenium-Version from selenium-api JAR Manifest", e);
		}
		
		if(seleniumVersions.isEmpty())
		{
			LOG.warn(
				"Failed to determine Selenium version from classpath - will use default version of {}",
				DEFAULT_SELENIUM_VERSION
			);
			return DEFAULT_SELENIUM_VERSION;
		}
		
		final String foundVersion = seleniumVersions.iterator().next();
		if(seleniumVersions.size() > 1)
		{
			LOG.warn(
				"Multiple versions of Selenium API found on classpath - will select {}, but this may not be reliable",
				foundVersion
			);
		}
		
		return foundVersion;
	}
	
	/**
	 * Read Manifest to get Selenium Version.
	 *
	 * @param manifest manifest
	 * @return Selenium Version detected
	 */
	public static String getSeleniumVersionFromManifest(final Manifest manifest)
	{
		String seleniumVersion = null;
		final Attributes buildInfo = manifest.getAttributes("Build-Info");
		if(buildInfo != null)
		{
			seleniumVersion = buildInfo.getValue("Selenium-Version");
		}
		
		// Compatibility Selenium > 3.X
		if(seleniumVersion == null)
		{
			final Attributes seleniumInfo = manifest.getAttributes("Selenium");
			if(seleniumInfo != null)
			{
				seleniumVersion = seleniumInfo.getValue("Selenium-Version");
			}
		}
		return seleniumVersion;
	}
}
