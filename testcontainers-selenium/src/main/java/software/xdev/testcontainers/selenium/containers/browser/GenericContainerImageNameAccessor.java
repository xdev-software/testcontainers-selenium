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
