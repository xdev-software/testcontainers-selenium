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

import java.nio.file.Path;
import java.util.concurrent.Future;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;


@SuppressWarnings("java:S119")
public abstract class RecordingContainer<SELF extends RecordingContainer<SELF>>
	extends GenericContainer<SELF>
{
	protected RecordingContainer(final DockerImageName dockerImageName)
	{
		super(dockerImageName);
	}
	
	protected RecordingContainer(final RemoteDockerImage image)
	{
		super(image);
	}
	
	protected RecordingContainer()
	{
	}
	
	protected RecordingContainer(final String dockerImageName)
	{
		super(dockerImageName);
	}
	
	protected RecordingContainer(final Future<String> image)
	{
		super(image);
	}
	
	public abstract Path saveRecordingToFile(Path directory, String fileNameWithoutExtension);
}
