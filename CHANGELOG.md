# 1.2.5
* Update default Selenium version to `4.36.0`
* Updated dependencies

# 1.2.4
* New option ``beforeRecordingSaveWaitTime`` in ``BrowserWebDriverContainer``
  * If not ``null``: Waits the amount of specified time before saving the recording
  * This way no frames showing a test failure might get accidentally lost
  * Default value is set to ``70ms`` which is 1 full frame when recording at the default 15 FPS
* Only compute name for recording when required

# 1.2.3
* Make some constants externally accessible
* Provide chromium image (Chrome doesn't work on ARM64)

# 1.2.2
* [Browser] Improve SHM configuration

# 1.2.1
* Migrated deployment to _Sonatype Maven Central Portal_ [#155](https://github.com/xdev-software/standard-maven-template/issues/155)
* Updated dependencies

# 1.2.0
* Remove testcontainer's dependency [onto JUnit 4](https://github.com/xdev-software/testcontainers-junit4-mock/?tab=readme-ov-file)
* Update default Selenium version to ``4.32.0``
* Updated dependencies

# 1.1.0
* Make it easier to use different Selenium images
* Update default Selenium version to ``4.25.0``
* Updated dependencies

# 1.0.2
* Updated dependencies

# 1.0.1
Rename artifactId from ``testcontainers-java-selenium`` to ``testcontainers-selenium``

# 1.0.0
<i>Initial release</i>
