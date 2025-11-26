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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.rnorth.ducttape.timeouts.Timeouts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;

import software.xdev.testcontainers.selenium.containers.recorder.RecordingContainer;
import software.xdev.testcontainers.selenium.containers.recorder.SeleniumRecordingContainer;


/**
 * A chrome/firefox/custom container based on SeleniumHQ's standalone container sets.
 */
@SuppressWarnings({"java:S119", "java:S2160", "PMD.GodClass"})
public class BrowserWebDriverContainer<SELF extends BrowserWebDriverContainer<SELF>>
	extends GenericContainer<SELF>
	implements TestLifecycleAware
{
	protected static final Logger LOG = LoggerFactory.getLogger(BrowserWebDriverContainer.class);
	
	public static final DockerImageName CHROME_IMAGE = DockerImageName.parse("selenium/standalone-chrome");
	// NOTE: Chrome has no ARM64 image (Why Google?) -> Provide option to use Chromium instead
	// https://github.com/SeleniumHQ/docker-selenium/discussions/2379
	public static final DockerImageName CHROMIUM_IMAGE = DockerImageName.parse("selenium/standalone-chromium");
	public static final DockerImageName FIREFOX_IMAGE = DockerImageName.parse("selenium/standalone-firefox");
	public static final DockerImageName EDGE_IMAGE = DockerImageName.parse("selenium/standalone-edge");
	
	protected static final Map<DockerImageName, String> WORKING_BROWSER_IMAGES_TRANSLATION =
		Collections.synchronizedMap(new HashMap<>());
	
	public static final int SELENIUM_PORT = 4444;
	public static final int VNC_PORT = 5900;
	public static final int NO_VNC_PORT = 7900;
	
	@SuppressWarnings("java:S2068")
	public static final String DEFAULT_VNC_PASSWORD = "secret";
	
	protected static final String TC_TEMP_DIR_PREFIX = "tc";
	
	protected static Boolean currentOsWindows; // You should use the method instead, this might be NULL
	
	protected boolean mapTimezoneIntoContainer = true;
	
	protected boolean validateImageEnabled = true;
	protected Duration validateImageGetTimeout = Duration.ofMinutes(5);
	
	// VNC
	protected boolean disableVNC = true;
	protected boolean exposeVNCPort;
	protected boolean enableNoVNC;
	
	// Recording
	protected Function<SELF, RecordingContainer<?>> recordingContainerSupplier = SeleniumRecordingContainer::new;
	protected RecordingContainer<?> recordingContainer;
	
	protected boolean startRecordingContainerManually;
	
	protected RecordingMode recordingMode = RecordingMode.SKIP;
	protected Path recordingDirectory;
	protected TestRecordingFileNameFactory testRecordingFileNameFactory = new DefaultTestRecordingFileNameFactory();
	protected Duration recordingSaveTimeout = Duration.ofMinutes(3);
	// Ensure that the current frame will be fully recorded (default record FPS = 15 -> 67ms per Frame)
	protected Duration beforeRecordingSaveWaitTime = Duration.ofMillis(70);
	
	public BrowserWebDriverContainer(final String dockerImageName)
	{
		this(DockerImageName.parse(dockerImageName));
	}
	
	public BrowserWebDriverContainer(final DockerImageName dockerImageName)
	{
		super(dockerImageName);
		this.waitStrategy = this.getDefaultWaitStrategy();
	}
	
	protected WaitStrategy getDefaultWaitStrategy()
	{
		return new WaitAllStrategy()
			.withStrategy(new LogMessageWaitStrategy()
				.withRegEx(".*(Started Selenium Standalone).*\n")
				.withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS)))
			.withStrategy(new HostPortWaitStrategy())
			.withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS));
	}
	
	// region Config
	public SELF withMapTimezoneIntoContainer(final boolean mapTimezoneIntoContainer)
	{
		this.mapTimezoneIntoContainer = mapTimezoneIntoContainer;
		return this.self();
	}
	
	public SELF withValidateImage(final boolean validateImage)
	{
		this.validateImageEnabled = validateImage;
		return this.self();
	}
	
	public SELF withValidateImageGetTimeout(final Duration validateImageGetTimeout)
	{
		this.validateImageGetTimeout = validateImageGetTimeout;
		return this.self();
	}
	
	// region VNC
	public SELF withDisableVNC(final boolean disableVNC)
	{
		this.disableVNC = disableVNC;
		return this.self();
	}
	
	public SELF withExposeVNCPort(final boolean exposeVNCPort)
	{
		this.exposeVNCPort = exposeVNCPort;
		return this.self();
	}
	
	public SELF withEnableNoVNC(final boolean enableNoVNC)
	{
		this.enableNoVNC = enableNoVNC;
		return this.self();
	}
	// endregion
	
	// region Recording
	public SELF withRecordingContainerSupplier(final Function<SELF, RecordingContainer<?>> recordingContainerSupplier)
	{
		this.recordingContainerSupplier = recordingContainerSupplier;
		return this.self();
	}
	
	public SELF withStartRecordingContainerManually(final boolean startRecordingContainerManually)
	{
		this.startRecordingContainerManually = startRecordingContainerManually;
		return this.self();
	}
	
	public SELF withRecordingMode(final RecordingMode recordingMode)
	{
		this.recordingMode = recordingMode;
		return this.self();
	}
	
	public SELF withRecordingDirectory(final Path recordingDirectory)
	{
		this.recordingDirectory = recordingDirectory;
		return this.self();
	}
	
	public SELF withTestRecordingFileNameFactory(final TestRecordingFileNameFactory testRecordingFileNameFactory)
	{
		this.testRecordingFileNameFactory = testRecordingFileNameFactory;
		return this.self();
	}
	
	public SELF withRecordingSaveTimeout(final Duration recordingSaveTimeout)
	{
		this.recordingSaveTimeout = recordingSaveTimeout;
		return this.self();
	}
	
	public SELF withBeforeRecordingSaveWaitTime(final Duration beforeRecordingSaveWaitTime)
	{
		this.beforeRecordingSaveWaitTime = beforeRecordingSaveWaitTime;
		return this.self();
	}
	
	// endregion
	
	// endregion
	
	@Override
	protected void configure()
	{
		this.configureRecording();
		this.configureTimezone();
		
		this.setCommand("/opt/bin/entry_point.sh");
		
		this.configureShm();
		
		/*
		 * Some unreliability of the selenium browser containers has been observed, so allow multiple attempts to
		 * start.
		 */
		this.setStartupAttempts(3);
		
		this.addExposedPorts(SELENIUM_PORT);
		this.configureVNC();
		
		this.validateImage();
	}
	
	@SuppressWarnings("java:S5443") // False positive, Files#createTempDirectory is safe and sets 700
	protected void configureRecording()
	{
		if(this.recordingMode == RecordingMode.SKIP)
		{
			return;
		}
		
		if(this.recordingDirectory == null)
		{
			try
			{
				this.recordingDirectory = Files.createTempDirectory(TC_TEMP_DIR_PREFIX);
			}
			catch(final IOException e)
			{
				// should never happen as per javadoc, since we use valid prefix
				throw new ContainerLaunchException("Exception while trying to create temp directory", e);
			}
		}
		
		// Recorder + Browser container must be able to communicate
		if(this.getNetwork() == null)
		{
			this.withNetwork(Network.SHARED);
		}
		
		this.recordingContainer = this.recordingContainerSupplier.apply(this.self());
	}
	
	protected void configureTimezone()
	{
		if(this.mapTimezoneIntoContainer)
		{
			String timeZone = System.getProperty("user.timezone");
			if(timeZone == null || timeZone.isEmpty())
			{
				timeZone = "Etc/UTC";
			}
			this.addEnv("TZ", timeZone);
		}
	}
	
	@SuppressWarnings("checkstyle:MagicNumber")
	protected void configureShm()
	{
		if(this.getShmSize() == null)
		{
			if(this.shouldDirectMountShm())
			{
				this.getBinds().add(new Bind("/dev/shm", new Volume("/dev/shm"), AccessMode.rw));
			}
			else
			{
				this.withSharedMemorySize(520_000_000L);
			}
		}
	}
	
	protected boolean shouldDirectMountShm()
	{
		return !isCurrentOsWindows();
	}
	
	protected static boolean isCurrentOsWindows()
	{
		if(currentOsWindows == null)
		{
			currentOsWindows = Optional.ofNullable(System.getProperty("os.name"))
				.map(osName -> osName.startsWith("Windows"))
				.orElse(false);
		}
		return !currentOsWindows;
	}
	
	protected void configureVNC()
	{
		if(this.disableVNC)
		{
			this.addEnv("SE_START_VNC", "false");
			return;
		}
		
		if(this.exposeVNCPort)
		{
			this.addExposedPort(VNC_PORT);
		}
		if(this.enableNoVNC)
		{
			this.addExposedPort(NO_VNC_PORT);
		}
	}
	
	// region Validate image
	// If testcontainers could implement the same method better or made stuff protected we wouldn't need reflection
	@SuppressWarnings("java:S3011")
	protected void validateImage()
	{
		if(!this.validateImageEnabled)
		{
			return;
		}
		
		// Sometimes Selenium images do not exist for the corresponding driver
		// e.g. https://github.com/SeleniumHQ/docker-selenium/issues/1979
		// In this case try to look for alternative images
		try
		{
			final Field fImage = GenericContainer.class.getDeclaredField("image");
			fImage.setAccessible(true);
			final RemoteDockerImage remoteDockerImage = (RemoteDockerImage)fImage.get(this);
			
			final Method mGetImageName = RemoteDockerImage.class.getDeclaredMethod("getImageName");
			mGetImageName.setAccessible(true);
			final DockerImageName currentImage = (DockerImageName)mGetImageName.invoke(remoteDockerImage);
			
			this.setDockerImageName(WORKING_BROWSER_IMAGES_TRANSLATION.computeIfAbsent(
				currentImage,
				this::validateImageOrPickAlternative));
		}
		catch(final Exception ex)
		{
			LOG.warn("Failed to validate image or pick alternative; Using default", ex);
		}
	}
	
	protected String validateImageOrPickAlternative(final DockerImageName initial)
	{
		RuntimeException prevEx = null;
		
		final List<String> versionParts = List.of(initial.getVersionPart().split("\\."));
		
		// Strategy: 1.2.3 -> 1.2 -> 1
		final List<String> tags = new ArrayList<>(List.of(initial.getVersionPart()));
		IntStream.range(0, versionParts.size() - 1)
			// Reverse (start with most specific version)
			.map(i -> versionParts.size() - 1 - i)
			// Pick version parts
			.mapToObj(i -> versionParts.stream().limit(i).collect(Collectors.joining(".")))
			.forEach(tags::add);
		
		for(final String currentTag : tags)
		{
			try
			{
				final DockerImageName current = initial.withTag(currentTag);
				Timeouts.getWithTimeout(
					(int)this.validateImageGetTimeout.toMillis(),
					TimeUnit.MILLISECONDS,
					new RemoteDockerImage(current)::get);
				if(!Objects.equals(currentTag, initial.getVersionPart()))
				{
					LOG.warn(
						"Unable to use {}; Selecting alternative {} due to",
						initial,
						current,
						prevEx);
				}
				return current.asCanonicalNameString();
			}
			catch(final RuntimeException rex)
			{
				if(prevEx != null)
				{
					rex.addSuppressed(prevEx);
				}
				prevEx = rex;
			}
		}
		assert prevEx != null;
		throw prevEx;
	}
	// endregion
	
	public String getVncAddress()
	{
		return !this.disableVNC && this.exposeVNCPort
			? "vnc://vnc:" + DEFAULT_VNC_PASSWORD + "@" + this.getHost() + ":" + this.getMappedPort(VNC_PORT)
			: null;
	}
	
	public String getNoVncAddress()
	{
		final String baseAddress = this.getNoVncAddressRaw();
		return baseAddress != null
			? baseAddress + this.getNoVncAutoLoginQueryParameterString()
			: null;
	}
	
	protected String getNoVncAutoLoginQueryParameterString()
	{
		return "?autoconnect=true&password=" + DEFAULT_VNC_PASSWORD;
	}
	
	public String getNoVncAddressRaw()
	{
		return !this.disableVNC && this.enableNoVNC
			? "http://" + this.getHost() + ":" + this.getMappedPort(NO_VNC_PORT)
			: null;
	}
	
	public URI getSeleniumAddressURI()
	{
		return URI.create("http://" + this.getHost() + ":" + this.getMappedPort(SELENIUM_PORT) + "/wd/hub");
	}
	
	@Override
	public void stop()
	{
		this.stopRecordingContainer();
		super.stop();
	}
	
	// region Recording
	@Override
	public void afterTest(final TestDescription description, final Optional<Throwable> throwable)
	{
		this.retainRecordingIfNeeded(description::getFilesystemFriendlyName, throwable.isEmpty());
	}
	
	protected void retainRecordingIfNeeded(final Supplier<String> testNameSupplier, final boolean succeeded)
	{
		// Should recording be retained?
		if(switch(this.recordingMode)
		{
			case RECORD_ALL -> false;
			case RECORD_FAILING -> succeeded;
			default -> true;
		})
		{
			return;
		}
		
		if(this.beforeRecordingSaveWaitTime != null)
		{
			try
			{
				Thread.sleep(this.beforeRecordingSaveWaitTime.toMillis());
			}
			catch(final InterruptedException e)
			{
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Got interrupted", e);
			}
		}
		
		// Get testname only when required to improve performance
		final String testName = testNameSupplier.get();
		try
		{
			final Path recording = Timeouts.getWithTimeout(
				(int)this.recordingSaveTimeout.toSeconds(),
				TimeUnit.SECONDS,
				() -> this.recordingContainer.saveRecordingToFile(
					this.recordingDirectory,
					this.testRecordingFileNameFactory.buildNameWithoutExtension(testName, succeeded))
			);
			LOG.info("Screen recordings for test {} will be stored at: {}", testName, recording);
		}
		catch(final org.rnorth.ducttape.TimeoutException te)
		{
			LOG.warn("Timed out while saving recording for test {}", testName, te);
		}
		catch(final Exception ex)
		{
			LOG.warn("Failed to save recording for test {}", testName, ex);
		}
	}
	
	@Override
	protected void containerIsStarted(final InspectContainerResponse containerInfo, final boolean reused)
	{
		if(!this.startRecordingContainerManually)
		{
			this.startRecordingContainer();
		}
	}
	
	public void startRecordingContainer()
	{
		if(this.recordingContainer != null)
		{
			this.recordingContainer.start();
		}
	}
	
	protected void stopRecordingContainer()
	{
		if(this.recordingContainer != null)
		{
			try
			{
				this.recordingContainer.stop();
			}
			catch(final Exception e)
			{
				LOG.warn("Failed to stop RecordingContainer", e);
			}
			this.recordingContainer = null;
		}
	}
	// endregion
	
	public String getContainerNameCleaned()
	{
		return this.getContainerName().replace("/", "");
	}
	
	public enum RecordingMode
	{
		SKIP,
		RECORD_ALL,
		RECORD_FAILING,
	}
	
	
	public interface TestRecordingFileNameFactory
	{
		String buildNameWithoutExtension(String testName, boolean succeeded);
	}
	
	
	public static class DefaultTestRecordingFileNameFactory implements TestRecordingFileNameFactory
	{
		public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
		
		public static final String PASSED = "PASSED";
		public static final String FAILED = "FAILED";
		
		@Override
		public String buildNameWithoutExtension(final String testName, final boolean succeeded)
		{
			return String.join(
				"-",
				succeeded ? PASSED : FAILED,
				testName,
				DTF.format(LocalDateTime.now(ZoneOffset.UTC)));
		}
	}
}
