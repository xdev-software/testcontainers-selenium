package software.xdev.testcontainers.selenium.containers.browser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.openqa.selenium.Capabilities;
import org.testcontainers.utility.DockerImageName;


/**
 * Separated from {@link BrowserWebDriverContainer} so that no Selenium Dependency is required
 */
@SuppressWarnings({"java:S119", "java:S2160"})
public class CapabilitiesBrowserWebDriverContainer<SELF extends CapabilitiesBrowserWebDriverContainer<SELF>>
	extends BrowserWebDriverContainer<SELF>
{
	protected static final Map<String, DockerImageName> BROWSER_DOCKER_IMAGES = new HashMap<>(Map.of(
		BrowserType.CHROME, CHROME_IMAGE,
		BrowserType.FIREFOX, FIREFOX_IMAGE,
		BrowserType.EDGE, EDGE_IMAGE));
	
	public CapabilitiesBrowserWebDriverContainer(final Capabilities capabilities)
	{
		super(getStandardImageForCapabilities(capabilities, SeleniumUtils.getClasspathSeleniumVersion()));
		this.waitStrategy = this.getDefaultWaitStrategy();
	}
	
	protected static DockerImageName getStandardImageForCapabilities(
		final Capabilities capabilities,
		final String seleniumVersion)
	{
		return Optional.ofNullable(BROWSER_DOCKER_IMAGES.get(Optional.ofNullable(capabilities)
				.map(Capabilities::getBrowserName)
				.orElse(BrowserType.CHROME)))
			.map(image -> image.withTag(seleniumVersion))
			.orElseThrow(() -> new UnsupportedOperationException(
				"Unsupported Browser name; Supported: " + String.join(", ", BROWSER_DOCKER_IMAGES.keySet())
			));
	}
	
	protected static final class BrowserType
	{
		public static final String CHROME = "chrome";
		public static final String FIREFOX = "firefox";
		public static final String EDGE = "MicrosoftEdge";
		
		private BrowserType()
		{
		}
	}
}

