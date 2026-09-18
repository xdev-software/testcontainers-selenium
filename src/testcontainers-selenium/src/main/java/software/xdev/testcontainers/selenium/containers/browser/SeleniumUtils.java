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

import java.util.jar.Manifest;


/**
 * @deprecated Renamed to {@link SeleniumVersionDetector}
 */
@SuppressWarnings("checkstyle:IllegalIdentifierName")
@Deprecated(forRemoval = true)
public final class SeleniumUtils
{
	public static final String DEFAULT_SELENIUM_VERSION = SeleniumVersionDetector.DEFAULT_SELENIUM_VERSION;
	
	public static String getClasspathSeleniumVersion()
	{
		return SeleniumVersionDetector.getClasspathSeleniumVersion();
	}
	
	public static synchronized String determineClasspathSeleniumVersion()
	{
		return SeleniumVersionDetector.determineClasspathSeleniumVersion();
	}
	
	public static String getSeleniumVersionFromManifest(final Manifest manifest)
	{
		return SeleniumVersionDetector.getSeleniumVersionFromManifest(manifest);
	}
	
	private SeleniumUtils()
	{
	}
}
