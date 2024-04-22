package software.xdev.testcontainers.selenium;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.lifecycle.TestDescription;

import software.xdev.testcontainers.selenium.containers.browser.BrowserWebDriverContainer;
import software.xdev.testcontainers.selenium.containers.browser.CapabilitiesBrowserWebDriverContainer;


class SimpleContainerTest
{
	@TempDir
	Path recordingDir;
	
	@MethodSource
	@ParameterizedTest
	@SuppressWarnings("resource")
	void simpleCheck(final Capabilities capabilities)
	{
		try(final CapabilitiesBrowserWebDriverContainer<?> browserContainer =
			new CapabilitiesBrowserWebDriverContainer<>(capabilities)
				.withRecordingMode(BrowserWebDriverContainer.RecordingMode.RECORD_ALL)
				.withRecordingDirectory(this.recordingDir))
		{
			browserContainer.start();
			final RemoteWebDriver remoteWebDriver =
				new RemoteWebDriver(browserContainer.getSeleniumAddressURI().toURL(), capabilities, false);
			
			remoteWebDriver.manage().window().maximize();
			remoteWebDriver.get(capabilities instanceof FirefoxOptions ? "about:support" : "chrome://version");
			remoteWebDriver.findElements(By.tagName("body"));
			
			remoteWebDriver.quit();
			
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
			
			final List<Path> recordedFiles = Files.list(this.recordingDir).toList();
			Assertions.assertEquals(1, recordedFiles.size());
			final Path recorded = recordedFiles.get(0);
			final String recordedFileName = recorded.getFileName().toString();
			
			Assertions.assertAll(
				() -> Assertions.assertTrue(recordedFileName.endsWith(".mp4")),
				() -> Assertions.assertTrue(recordedFileName.startsWith("PASSED-")),
				() -> Assertions.assertTrue(Files.size(recorded) > 0));
		}
		catch(final IOException ioe)
		{
			throw new UncheckedIOException(ioe);
		}
	}
	
	static Stream<Arguments> simpleCheck()
	{
		return Stream.of(
			Arguments.of(new FirefoxOptions()),
			Arguments.of(new ChromeOptions())
		);
	}
}
