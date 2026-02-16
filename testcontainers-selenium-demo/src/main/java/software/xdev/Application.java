package software.xdev;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.lifecycle.TestDescription;

import software.xdev.testcontainers.selenium.containers.browser.BrowserWebDriverContainer;
import software.xdev.testcontainers.selenium.containers.browser.CapabilitiesBrowserWebDriverContainer;


public final class Application
{
	@SuppressWarnings("resource") // It's getting closed...
	public static void main(final String[] args)
	{
		final Path recordingDir = Path.of("target/records");
		// noinspection ResultOfMethodCallIgnored
		recordingDir.toFile().mkdirs();
		
		for(final Capabilities capabilities : List.of(new ChromeOptions(), new FirefoxOptions()))
		{
			try(final var browserContainer = new CapabilitiesBrowserWebDriverContainer<>(capabilities)
				.withRecordingMode(BrowserWebDriverContainer.RecordingMode.RECORD_ALL)
				.withRecordingDirectory(recordingDir))
			{
				browserContainer.start();
				final RemoteWebDriver remoteWebDriver =
					new RemoteWebDriver(browserContainer.getSeleniumAddressURI().toURL(), capabilities, false);
				
				remoteWebDriver.manage().window().maximize();
				
				remoteWebDriver.get(capabilities instanceof FirefoxOptions ? "about:support" : "chrome://version");
				Thread.sleep(1000); // Simulate Test work
				remoteWebDriver.findElements(By.tagName("body"));
				
				remoteWebDriver.quit();
				
				// Wait a moment until everything is safe on tape
				Thread.sleep(100);
				
				browserContainer.afterTest(new TestDescription()
				{
					@Override
					public String getTestId()
					{
						return "demo-" + capabilities.getBrowserName();
					}
					
					@Override
					public String getFilesystemFriendlyName()
					{
						return "demo-" + capabilities.getBrowserName();
					}
				}, Optional.empty());
			}
			catch(final InterruptedException ite)
			{
				Thread.currentThread().interrupt();
			}
			catch(final IOException ioe)
			{
				throw new UncheckedIOException(ioe);
			}
		}
	}
	
	private Application()
	{
	}
}
