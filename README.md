# Techsenger CEFFX

Techsenger CEFFX is a library for integrating the Chromium Embedded Framework into JavaFX applications. It is a port
of [JCEF](https://github.com/chromiumembedded/java-cef) (commit d3de827), migrated from Swing to JavaFX. Designed
specifically for JavaFX, CEFFX provides an optimized and efficient solution for working with Chromium on the platform.

CEFFX provides prebuilt native libraries, making it easy to integrate CEF into any JavaFX application without the need
to compile native code from source.

## Table of Contents
* [Demo](#demo)
* [Features](#features)
* [Related Projects](#projects)
    * [Chromium](#projects-chromium)
    * [CEF](#projects-cef)
    * [JCEF](#projects-jcef)
* [Dependencies](#dependencies)
* [Usage](#usage)
    * [Settings](#usage-settings)
    * [Threads](#usage-threads)
    * [Native Deployment](#usage-native-deployment)
        * [Automatic Deployment](#usage-automatic-deployment)
        * [Manual Deployment](#usage-manual-deployment)
* [Code building](#code-building)
* [Running Demo](#running-demo)
* [License](#license)
* [Contributing](#contributing)
* [Support Us](#support-us)

## Demo <a name="demo"></a>

<img width="1200" height="872" alt="Techsenger CEFFX" src="https://github.com/user-attachments/assets/b6b3ee9e-2bb2-4e42-9dd2-d8d3e1294d6a" />

## Features <a name="features"></a>

Key features include:

* The library uses only JavaFX classes for UI development.
* Dual-thread architecture — JavaFX thread and a dedicated CEF thread.
* Supports almost all features of Java CEF.
* Custom rendering capabilities.
* A demo application showcasing library features.
* Comprehensive documentation.

## Related Projects <a name="projects"></a>

### Chromium <a name="projects-chromium"></a>

[Chromium](https://github.com/chromium/chromium) is an open-source web browser project that serves as the foundation
for many modern browsers, including Google Chrome. It provides a full technology stack for rendering web pages,
including the Blink rendering engine, the V8 JavaScript engine, as well as subsystems for networking, graphics, and
multimedia. Chromium is developed as a high-performance and secure platform, offering support for modern web standards
and a rapid release cycle.

At the same time, Chromium is not a library or an embeddable engine in the traditional sense — it is a full-featured
browser project with a large codebase and a complex architecture. It can be used as a basis for other solutions, but
it is not typically distributed as a standalone component for direct embedding into applications. For such use cases,
higher-level wrappers are commonly used, which adapt its capabilities for integration into third-party applications.

### CEF <a name="projects-cef"></a>

The Chromium Embedded Framework ([CEF](https://github.com/chromiumembedded/cef)) is a simple framework for embedding
Chromium-based browsers in other applications. CEF insulates the user from the underlying Chromium and Blink code
complexity by offering production-quality stable APIs, release branches tracking specific Chromium releases,
and binary distributions. Most features in CEF have default implementations that provide rich functionality while
requiring little or no integration work from the user. There are currently over 100 million installed instances of CEF
around the world embedded in products from a wide range of companies and industries. Some use cases for CEF include:

* Embedding an HTML5-compliant Web browser control in an existing native application.
* Creating a light-weight native “shell” application that hosts a user interface developed primarily using Web technologies.
* Rendering Web content “off-screen” in applications that have their own custom drawing frameworks.
* Acting as a host for automated testing of existing Web properties and applications.

CEF supports a wide range of programming languages and operating systems and can be easily integrated into both new
and existing applications. It was designed from the ground up with both performance and ease of use in mind. The base
framework includes C and C++ programming interfaces exposed via native libraries that insulate the host application
from Chromium and Blink implementation details. It provides close integration between the browser and the host
application including support for custom plugins, protocols, JavaScript objects and JavaScript extensions.
The host application can optionally control resource loading, navigation, context menus, printing and more, while
taking advantage of the same performance and HTML5 technologies available in the Google Chrome Web browser.

### JCEF <a name="projects-jcef"></a>

The Java Chromium Embedded Framework ([JCEF](https://github.com/chromiumembedded/java-cef)) is a simple framework for
embedding Chromium-based browsers in other applications using the Java programming language. It provides a robust and
mature API that enables seamless integration of modern web technologies into Java applications, supporting features
such as JavaScript execution, DOM interaction, custom rendering, and fine-grained control over browser behavior.
Built as a Java wrapper around the Chromium Embedded Framework, JCEF allows developers to leverage the power of the
Chromium engine within Java applications.

At the same time, JCEF is designed around the Swing UI toolkit, which introduces limitations when used in modern
Java applications that rely on JavaFX. This architectural choice can make integration less efficient and more complex
in JavaFX-based environments, particularly when aiming for consistent rendering, threading, and UI behavior across
the application.

## Dependencies <a name="dependencies"></a>

This project will soon be available on Maven Central:

```
<dependency>
    <groupId>com.techsenger.ceffx</groupId>
    <artifactId>ceffx-natives</artifactId>
    <version>${ceffx.version}</version>
    <classifier>${ceffx.classifier}</classifier>
</dependency>
<dependency>
    <groupId>com.techsenger.ceffx</groupId>
    <artifactId>ceffx-core</artifactId>
    <version>${ceffx.version}</version>
</dependency>
```
The `ceffx.classifier` uses the same values as the OpenJFX classifiers:

* linux (built using the `ubuntu-22.04` runner)
* win (built using the `windows-2022` runner)
* mac (built using the `macos-15-intel` runner)
* mac-aarch64 (built using the `macos-15` runner)

Note that Maven can set the `classifier` using a `profile`. See an example in the demo [pom.xml](java/ceffx-demo/pom.xml).

To use snapshot versions, add our repository:

```
<repository>
    <id>repsy-snapshots</id>
    <url>https://repo.repsy.io/mvn/techsenger/snapshots</url>
</repository>
```

## Usage <a name="usage"></a>

### Settings <a name="usage-settings"></a>

Currently `Cef` is used with the following key settings:

```java
CefSettings settings = new CefSettings();
settings.windowless_rendering_enabled = true;
settings.multi_threaded_message_loop = true;
...
```
**windowless_rendering_enabled**. In native (non-OSR) mode, CEF needs a real OS-level window handle — an `HWND` on Windows,
`NSView` on macOS, or `XID` on Linux — to embed its browser window into. In Swing/AWT this is possible because components
like Canvas are heavyweight: each one has its own native peer, meaning the OS actually knows about them as separate
windows and can return a valid handle. CEF attaches itself to that handle and renders directly into it as a child window.

JavaFX, however, is built entirely on lightweight rendering — the whole `Stage` owns a single native window, and every
`Node` inside it is just painted onto that one surface by the JavaFX engine (Prism). There are no individual native
peers for `Node` objects, so there is no handle to give CEF. Without a valid OS window handle, CEF has nothing to attach
to, making windowless_rendering_enabled = false simply impossible in a JavaFX context. OSR must be used instead, where
CEF renders frames into a pixel buffer that JavaFX can then draw onto a Canvas or WritableImage.

**multi_threaded_message_loop**. When this setting is `true`, CEF spawns its own dedicated thread and runs a native
OS message loop there. This means CEF manages its own event processing independently, without requiring the host
application to do anything extra.

When set to `false`, CEF gives up control of the message loop and expects the host application to periodically call
`CefApp.doMessageLoopWork()` to drive CEF's event processing manually. This gives the host application full control
over when and how CEF processes its events, which is essential when integrating with frameworks that have their own
strict threading or event loop models.

Theoretically, both modes are supported in JavaFX. However, in practice when this flag is `false` does not work
correctly — OS events stop being delivered to the JavaFX platform entirely. The JavaFX Application Thread itself
remains alive and can be observed running, but no input or system events reach it. This issue is also
[acknowledged](https://github.com/chromiumembedded/java-cef/blob/d3de8278626c160d4631db3fda2df7b77cc1e5c4/native/context.cpp#L203)
by the JCEF developers, though in the context of windowed mode specifically. If anyone knows what the root cause of
this issue is, please let us know.

### Threads <a name="usage-threads"></a>

CEFFX introduces a dual-thread architecture. The first thread is the JavaFX Application Thread, on which all
interactions with the JavaFX platform must be performed (using `Platform.runLater(...)`). The second thread is the
CEF thread, on which all interactions with CEF must be executed (using `CefApp.runLater(...)`).

Performing CEF operations outside the CEF thread may lead to inconsistent behavior: some operations may work (always
or intermittently), while others may fail. In addition, this can result in CEF-related exceptions being thrown.

As a rule of thumb, if something does not work as expected, it is recommended to first check which thread is being used.

### Native Deployment <a name="usage-native-deployment"></a>

CEFFX provides prebuilt native libraries, making it easy to integrate CEF into any JavaFX application
without the need to compile it from source.

There are two ways to make CEF and the CEFFX native libraries available to your application, and this
applies the same way on Linux, Windows and macOS:

1. **Inside application package.** The CEF runtime and CEFFX natives are placed inside your application's own
   distributable package (for example, as part of a `jpackage` build step) before it is shipped. This is entirely up
   to whoever packages the application - CEFFX only provides the pieces (the CEF archive, the classified `ceffx-natives`
   jar, `NativeExtractor`); how and when they are combined into the final package is a packaging-time decision outside
   CEFFX's scope.
2. **Outside application package.** CEF and the CEFFX natives live in their own directory, independent of the
   application package - for example, in a per-user cache directory. This is also what makes local development
   (`mvn javafx:run`, with no packaged application at all) work the same way as a packaged one. There are two ways to
   set this up:
   - **Automatic Deployment** - using the `NativeDeployment` class, described below.
   - **Manual Deployment** - following the steps described below.

The remainder of this section covers deployment outside the application package, since bundling inside it is a
packaging concern specific to each application and is not prescribed by CEFFX.

#### Automatic Deployment <a name="usage-automatic-deployment"></a>

Use the `NativeDeployment` class provided by the `ceffx-natives` module. Give it a target directory and it takes care
of everything else - downloading the correct CEF distribution version, extracting it alongside the CEFFX native binaries,
and handling the platform-specific layout differences described in the Manual Deployment section below.

> This class is still under development; usage details will be documented here once finalized.

#### Manual Deployment <a name="usage-manual-deployment"></a>

If you prefer to control each step yourself, follow the steps below. This applies the same way on Linux,
Windows and macOS unless noted otherwise.

1. Download the minimal CEF distribution for the `cef.version` specified in `cef.properties` from
   [CEF](https://cef-builds.spotifycdn.com/index.html). Use the `Version Filter` to locate the correct version.
   Please note that other versions will not work, as CEFFX includes a built-in version check.
2. Create a directory on your system, for example: `/ceffx`.
3. Copy the contents of the CEF distribution archive into `/ceffx`:
   - **Linux and Windows** - copy the contents of the `Release` folder and the contents of the
     `Resources` folder into `/ceffx`.
   - **macOS** - copy the `Chromium Embedded Framework.framework` directory into `/ceffx/Frameworks/`.
     The framework is self-contained and already includes all resources, locales and `.pak` files,
     unlike the loose files shipped on Linux and Windows.

   After that, `/ceffx` should contain (platform-specific contents shown for each OS):

   **Linux**
```
   chrome_100_percent.pak
   chrome_200_percent.pak
   chrome-sandbox
   icudtl.dat
   libcef.so
   libEGL.so
   libGLESv2.so
   libvk_swiftshader.so
   libvulkan.so.1
   locales/
   resources.pak
   v8_context_snapshot.bin
   vk_swiftshader_icd.json
```

   **Windows**
```
   chrome_100_percent.pak
   chrome_200_percent.pak
   d3dcompiler_47.dll
   icudtl.dat
   libcef.dll
   libEGL.dll
   libGLESv2.dll
   locales/
   resources.pak
   snapshot_blob.bin
   v8_context_snapshot.bin
   vk_swiftshader_icd.json
   vulkan-1.dll
```

   **macOS** (both `mac` and `mac-aarch64`)
```
   Frameworks/
   └── Chromium Embedded Framework.framework/
```
   On macOS, CEF requires a `Frameworks` subdirectory relative to the helper app bundles - see step 4.
   The framework itself is self-contained, including all resources, locales and `.pak` files, unlike the
   loose files shipped on Linux and Windows.

4. On the first run, extract the native binaries from the `ceffx-natives` module into `/ceffx`, using
   `NativeExtractor`.

   **Linux and Windows** - this adds `libceffx.so`/`ceffx.dll` and the `ceffx_helper` executable, placed
   flat alongside the files from step 3.

   **macOS** - this adds `libceffx.dylib` directly into `/ceffx`, plus all five `ceffx Helper*.app`
   bundles (base, GPU, Plugin, Renderer, Alerts) into `/ceffx/Frameworks/`, alongside the framework
   copied there in step 3:
```
   /ceffx
   ├── libceffx.dylib
   └── Frameworks/
       ├── Chromium Embedded Framework.framework/
       ├── ceffx Helper.app/
       ├── ceffx Helper (GPU).app/
       ├── ceffx Helper (Plugin).app/
       ├── ceffx Helper (Renderer).app/
       └── ceffx Helper (Alerts).app/
```
   This `Frameworks` layout is required by CEF on macOS and is not optional - unlike Linux and Windows, there is no
   way to point CEF at a fully flat directory instead.

5. Configure your application:

   **Linux and Windows** - simply set the system property:
```
   -Djava.library.path=/ceffx
```
   CEF locates its helper executable and resources relative to the loaded library by default; no
   further configuration is required.

   **macOS** - set `java.library.path` to `/ceffx` as above; no further CEF-side configuration is
   required as long as the `Frameworks` layout from step 4 is in place. CEF finds the helper bundles and
   framework via the fixed `../Frameworks` relative path from `libceffx.dylib`, the same way it does
   inside a real `.app` bundle.

   **The same `/ceffx` layout works in both scenarios** - whether the application is launched directly
   (`mvn javafx:run`, no `.app` bundle exists at all) or from a `jpackage`-built `.app` bundle. Only the
   `Frameworks` subdirectory needs to physically exist; `/ceffx` itself does not need to be a real,
   signed `.app` bundle.


#### Known open item - helper bundle identifier (macOS)

CEFFX's bundled helper apps currently carry a fixed bundle identifier (`org.ceffx.ceffx.helper[.suffix]`)
baked in at build time. Chromium derives its Mach IPC rendezvous name from the **host application's**
bundle identifier and expects each helper's identifier to match it as `<host id>.helper[.suffix]`; a
mismatch causes subprocesses to fail with `No rendezvous client`.

Whether `--main-bundle-path` (above) resolves this automatically, or whether the helper `Info.plist`
files still need their `CFBundleIdentifier` rewritten per-application at extraction time, has not yet
been verified in practice. This will be tested and documented once confirmed.

## Code Building <a name="code-building"></a>

To build the library use the following commands.

1. Clone the repository

```
git clone https://github.com/techsenger/ceffx
# Enter the project root directory
cd ceffx
```

2. Build native code

```
# Create and enter the `build` directory (it is required by other tooling and should not be changed)
cd native && mkdir build && cd build

# Linux: Generate 64-bit Unix Makefiles
cmake -G "Unix Makefiles" -DCMAKE_BUILD_TYPE=Release ..
# Build using Make
make -j4

# MacOS: Generate 64-bit Xcode project files
cmake -G "Xcode" -DPROJECT_ARCH="x86_64" ..
# Open ceffx.xcodeproj in Xcode
# - Select Scheme > Edit Scheme and change the "Build Configuration" to "Release"
# - Select Product > Build

# MacOS: Generate ARM64 Xcode project files
cmake -G "Xcode" -DPROJECT_ARCH="arm64" ..
# Open ceffx.xcodeproj in Xcode
# - Select Scheme > Edit Scheme and change the "Build Configuration" to "Release"
# - Select Product > Build

# Windows: Generate 64-bit VS2022 project files
cmake -G "Visual Studio 17" -A x64 ..
# Open ceffx.sln in Visual Studio
# - Select Build > Configuration Manager and change the "Active solution configuration" to "Release"
# - Select Build > Build Solution
```
3. Build Java code

```
# Enter the java directory from the project root
cd java
# Compile Maven project
mvn install
```

## Running Demo <a name="running-demo"></a>

To run the demo execute the following commands in the root of the project:

    cd java && cd ceffx-demo
    mvn javafx:run

Please note, that debugger settings are in `ceffx-demo/pom.xml` file.

## License <a name="license"></a>

Techsenger CEFFX is licensed under the BSD 3-Clause License. See LICENSE file for details.

## Contributing <a name="contributing"></a>

We welcome all contributions. You can help by reporting bugs, suggesting improvements, or submitting pull requests
with fixes and new features. If you have any questions, feel free to reach out — we’ll be happy to assist you.

## Support Us <a name="support-us"></a>

You can support our open-source work through [GitHub Sponsors](https://github.com/sponsors/techsenger).
Your contribution helps us maintain projects, develop new features, and provide ongoing improvements.
Multiple sponsorship tiers are available, each offering different levels of recognition and benefits.
