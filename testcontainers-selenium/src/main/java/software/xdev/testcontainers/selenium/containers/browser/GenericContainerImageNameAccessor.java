package software.xdev.testcontainers.selenium.containers.browser;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;


/**
 * Extracts the {@link DockerImageName} from {@link GenericContainer} without pulling it.
 * <p>
 * Uses reflection because there is no official way to access it.
 * </p>
 */
public final class GenericContainerImageNameAccessor
{
	// Cache to improve performance
	private static boolean initialized;
	private static Field fImage;
	private static Method mGetImageName;
	
	public static DockerImageName getImageName(final GenericContainer<?> container) throws Exception
	{
		if(!initialized)
		{
			fImage = GenericContainer.class.getDeclaredField("image");
			fImage.setAccessible(true);
			
			mGetImageName = RemoteDockerImage.class.getDeclaredMethod("getImageName");
			mGetImageName.setAccessible(true);
			
			initialized = true;
		}
		
		final RemoteDockerImage remoteDockerImage = (RemoteDockerImage)fImage.get(container);
		
		return (DockerImageName)Objects.requireNonNull(mGetImageName.invoke(remoteDockerImage));
	}
	
	private GenericContainerImageNameAccessor()
	{
	}
}
