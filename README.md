[![Latest version](https://img.shields.io/maven-central/v/software.xdev/testcontainers-java-selenium?logo=apache%20maven)](https://mvnrepository.com/artifact/software.xdev/testcontainers-java-selenium)
[![Build](https://img.shields.io/github/actions/workflow/status/xdev-software/testcontainers-java-selenium/checkBuild.yml?branch=develop)](https://github.com/xdev-software/testcontainers-java-selenium/actions/workflows/checkBuild.yml?query=branch%3Adevelop)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=xdev-software_testcontainers-java-selenium&metric=alert_status)](https://sonarcloud.io/dashboard?id=xdev-software_testcontainers-java-selenium)

# <img src="https://raw.githubusercontent.com/SeleniumHQ/seleniumhq.github.io/690acbad7b4bf4656f116274809765db64e6ccf7/website_and_docs/static/images/logos/webdriver.svg" height=24 /> testcontainers-java-selenium

A re-implementation of [Testcontainer Selenium/WebDriver](https://java.testcontainers.org/modules/webdriver_containers/) with the following improvements:
* It uses [Selenium's video recorder](https://github.com/SeleniumHQ/docker-selenium/blob/trunk/README.md#video-recording)
  * Doesn't require VNC 
    * No VNC Server started in the browser container (unless explicitly stated) → Saves memory
  * Uses [Selenium's implementation](https://github.com/SeleniumHQ/docker-selenium/tree/trunk/Video) and isn't [based](https://github.com/testcontainers/vnc-recorder) on [some python code from 2010](https://pypi.org/project/vnc2flv/#history)
    * Way more customization options for e.g. ``framerate``, ``codec``, ``preset`` ...
    * Uses ``mp4`` as default recording format (wider support in comparison to ``flv``)
    * [Renders while saving the video](https://github.com/SeleniumHQ/docker-selenium/blob/4c572afd1173b5bd49fa2def3b54ea552fccee85/Video/video.sh#L126) (not when finished which takes additional time)
  * Stops the recorder before saving the file so that there is no way that [it runs forever](https://github.com/testcontainers/testcontainers-java/discussions/6229).
* Automatically tries to select a alternative Selenium version for the docker image if it [doesn't exist](https://github.com/SeleniumHQ/docker-selenium/issues/1979).
* Added support for [NoVNC](https://github.com/SeleniumHQ/docker-selenium/blob/trunk/README.md#using-your-browser-no-vnc-client-is-needed) so that no dedicated VNC client is required
* Improve creation of video filenames
* Removed hard dependency on Selenium-Java.<br/>Only required when using ``CapabilitiesBrowserWebDriverContainer``
* Everything can be ``@Override``n if required
* Caches "Selenium version detection via classpath" so that it's not invoked everytime you build a new container

## Installation
[Installation guide for the latest release](https://github.com/xdev-software/testcontainers-java-selenium/releases/latest#Installation)

## Support
If you need support as soon as possible and you can't wait for any pull request, feel free to use [our support](https://xdev.software/en/services/support).

## Contributing
See the [contributing guide](./CONTRIBUTING.md) for detailed instructions on how to get started with our project.

## Dependencies and Licenses
View the [license of the current project](LICENSE) or the [summary including all dependencies](https://xdev-software.github.io/testcontainers-java-selenium/dependencies)

<sub>Disclaimer: This is not an official Testcontainers/Selenium product and not associated with them</sub>
