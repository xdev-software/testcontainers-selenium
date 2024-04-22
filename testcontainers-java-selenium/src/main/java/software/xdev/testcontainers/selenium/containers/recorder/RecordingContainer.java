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
