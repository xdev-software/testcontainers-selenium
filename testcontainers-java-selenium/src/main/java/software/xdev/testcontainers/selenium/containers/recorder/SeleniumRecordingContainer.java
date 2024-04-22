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
package software.xdev.testcontainers.selenium.containers.recorder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import software.xdev.testcontainers.selenium.containers.browser.BrowserWebDriverContainer;


@SuppressWarnings("java:S2160")
public class SeleniumRecordingContainer extends RecordingContainer<SeleniumRecordingContainer>
{
	public static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("selenium/video");
	
	// https://github.com/SeleniumHQ/docker-selenium/blob/033f77c02dde9d61d1a4d44be7526ef689244606/Video/Dockerfile#L103-L110
	public static final String ENV_DISPLAY_CONTAINER_NAME = "DISPLAY_CONTAINER_NAME";
	public static final String ENV_SE_VIDEO_FILE_NAME = "SE_VIDEO_FILE_NAME";
	public static final String ENV_SE_SCREEN_WIDTH = "SE_SCREEN_WIDTH";
	public static final String ENV_SE_SCREEN_HEIGHT = "SE_SCREEN_HEIGHT";
	public static final String ENV_SE_FRAME_RATE = "SE_FRAME_RATE";
	public static final String ENV_SE_CODEC = "SE_CODEC";
	public static final String ENV_SE_PRESET = "SE_PRESET";
	
	protected BrowserWebDriverContainer<?> target;
	
	protected String displayContainerName;
	protected String videoFileName;
	protected boolean resolutionConfigured;
	protected String fileExtension = "mp4";
	
	public SeleniumRecordingContainer(final BrowserWebDriverContainer<?> target)
	{
		this(target, DEFAULT_IMAGE);
	}
	
	public SeleniumRecordingContainer(
		final BrowserWebDriverContainer<?> target,
		final DockerImageName dockerImageName)
	{
		super(dockerImageName);
		this.target = target;
		
		this.setWaitStrategy(new LogMessageWaitStrategy()
			.withRegEx(".*(success: video-ready entered RUNNING state).*\n")
			.withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS)));
	}
	
	// region Config
	public SeleniumRecordingContainer withDisplayContainerName(final String name)
	{
		this.displayContainerName = name;
		this.addEnv(ENV_DISPLAY_CONTAINER_NAME, name);
		return this;
	}
	
	public SeleniumRecordingContainer withVideoFileName(final String name)
	{
		if("auto".equals(name))
		{
			// Selenium Video recorder has an 'auto' mode:
			// https://github.com/SeleniumHQ/docker-selenium/blob/trunk/README.md#video-recording-and-uploading
			// https://github.com/SeleniumHQ/docker-selenium/blob/033f77c02dde9d61d1a4d44be7526ef689244606/Video/video.sh#L129-L208
			// However we currently don't support it due to various reasons:
			// * It requires some additional computation work for e.g. detecting the Selenium Session
			// * The video name is generic. We would have to search for it in the directory - which is not possible
			// inside a stopped container
			throw new IllegalArgumentException("'auto' is currently not supported");
		}
		this.videoFileName = name;
		this.addEnv(ENV_SE_VIDEO_FILE_NAME, name);
		return this;
	}
	
	public SeleniumRecordingContainer withDisplayResolution(final String width, final String height)
	{
		this.addEnv(ENV_SE_SCREEN_WIDTH, width);
		this.addEnv(ENV_SE_SCREEN_HEIGHT, height);
		this.resolutionConfigured = true;
		return this;
	}
	
	public SeleniumRecordingContainer withFileExtension(final String fileExtension)
	{
		this.fileExtension = fileExtension;
		return this;
	}
	
	public SeleniumRecordingContainer withFrameRate(final int frameRate)
	{
		return this.withFrameRate(String.valueOf(frameRate));
	}
	
	public SeleniumRecordingContainer withFrameRate(final String frameRate)
	{
		// Default is 15
		this.addEnv(ENV_SE_FRAME_RATE, frameRate);
		return this;
	}
	
	public SeleniumRecordingContainer withCodec(final String codec)
	{
		// Default is libx264
		this.addEnv(ENV_SE_CODEC, codec);
		return this;
	}
	
	/**
	 * @apiNote Be careful: May require additional escaping
	 */
	public SeleniumRecordingContainer withPreset(final String preset)
	{
		// Default is "-preset ultrafast"
		this.addEnv(ENV_SE_PRESET, preset);
		return this;
	}
	// endregion
	
	@Override
	protected void configure()
	{
		this.withNetwork(this.target.getNetwork());
		
		if(this.displayContainerName == null)
		{
			this.withDisplayContainerName(
				this.target.getNetworkAliases().stream()
					.findFirst()
					.orElseGet(() -> this.target.getContainerName().replace("/", "")));
		}
		if(this.videoFileName == null)
		{
			this.withVideoFileName("record-" + this.target.getContainerId() + "." + this.fileExtension);
		}
		if(!this.resolutionConfigured)
		{
			this.withDisplayResolution(
				this.target.getEnvMap().getOrDefault(ENV_SE_SCREEN_WIDTH, "1360"),
				this.target.getEnvMap().getOrDefault(ENV_SE_SCREEN_HEIGHT, "1020"));
		}
		
		super.configure();
	}
	
	@Override
	public Path saveRecordingToFile(final Path directory, final String fileNameWithoutExtension)
	{
		// Check if container was started
		if(this.getContainerId() == null)
		{
			return null;
		}
		
		// STOP CONTAINER - NO REMOVE, JUST STOP otherwise we lose the recording file
		this.stopNoRemove();
		
		// COPY FILE + RENAME
		final Path outFilePath = this.resolveOutputFile(directory, fileNameWithoutExtension);
		
		this.copyRecording(outFilePath);
		
		return outFilePath;
	}
	
	protected void stopNoRemove()
	{
		this.dockerClient.stopContainerCmd(this.getContainerId()).exec();
	}
	
	protected Path resolveOutputFile(final Path directory, final String fileNameWithoutExtension)
	{
		return directory.resolve(fileNameWithoutExtension
			+ "."
			+ Optional.ofNullable(this.videoFileName)
			.filter(f -> f.contains("."))
			.map(f -> f.substring(f.lastIndexOf(".") + 1))
			.orElse(""));
	}
	
	protected void copyRecording(final Path outFilePath)
	{
		this.copyFileFromContainer(
			"/videos/" + this.videoFileName,
			is -> Files.copy(is, outFilePath, StandardCopyOption.REPLACE_EXISTING));
	}
}
