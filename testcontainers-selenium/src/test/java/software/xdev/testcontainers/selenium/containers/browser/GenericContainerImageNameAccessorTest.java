package software.xdev.testcontainers.selenium.containers.browser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;


class GenericContainerImageNameAccessorTest
{
	static final String IMAGE = "alpine:3";
	static final DockerImageName DOCKER_IMAGE_NAME = DockerImageName.parse(IMAGE);
	
	@ParameterizedTest
	@MethodSource
	void reflectionStillWorks(final GenericContainer<?> container)
	{
		assertEquals(
			DOCKER_IMAGE_NAME,
			assertDoesNotThrow(() -> GenericContainerImageNameAccessor.getImageName(container)));
	}
	
	static Stream<Arguments> reflectionStillWorks()
	{
		return Stream.of(
				new GenericContainer<>(IMAGE),
				new GenericContainer<>(CompletableFuture.completedFuture(IMAGE)))
			.map(Arguments::of);
	}
}
