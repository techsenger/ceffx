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
    * [Threads and Message Loops](#usage-threads-and-loops)
    * [Native Deployment](#usage-native-deployment)
        * [NativeProps](#usage-native-props)
        * [NativeExtractor](#usage-native-extractor)
        * [NativeDeployer](#usage-native-deployer)
        * [Path resolution reference](#usage-path-reference)
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

`Cef` is recommended to be used with the following key settings:

```java
CefSettings settings = new CefSettings();
settings.windowless_rendering_enabled = true;
// Linux and Windows
settings.multi_threaded_message_loop = true;
settings.external_message_pump = false;
// macOS
settings.multi_threaded_message_loop = false;
settings.external_message_pump = true;
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

Per CEF's own documentation, `multi_threaded_message_loop` is only supported on Windows and Linux; on macOS it must
be `false` (see `external_message_pump` below).

On Windows and Linux, `multi_threaded_message_loop` theoretically should work with either boolean value. However, in
practice, setting it to `false` there does not work correctly — OS events stop being delivered to the JavaFX
platform entirely. The JavaFX Application Thread itself remains alive and can be observed running, but no input or
system events reach it. This issue is also
[acknowledged](https://github.com/chromiumembedded/java-cef/blob/d3de8278626c160d4631db3fda2df7b77cc1e5c4/native/context.cpp#L203)
by the JCEF developers, though in the context of windowed mode specifically. If anyone knows what the root cause of
this issue is, please let us know.

**external_message_pump**. When this setting is `true`, CEF uses the external message pump mode. CEF does not create
its own message loop thread. Instead, CEF notifies the host through `OnScheduleMessagePumpWork()` when
`CefDoMessageLoopWork()` should be called and provides the requested delay.

In CEFFX, the external message pump is used on macOS because CEF does not support
`multi_threaded_message_loop` on that platform. CEFFX receives the pump requests from CEF and schedules
`CefDoMessageLoopWork()` on its dedicated CEF thread.

The important distinction is that CEFFX drives the external message pump, but does not implement CEF's message
loop itself. CEF determines when message-pump work is required; CEFFX is responsible for scheduling and executing
that work.

The following table describes the possible combinations of the `multi_threaded_message_loop` and `external_message_pump`
parameters:

| multi_<br>threaded_<br>message_loop | external_<br>message_<br>pump | Message loop owner | Message pump driver | Description |
|---|---|---|---|---|
| `false` | `false` | Application | Application | CEF does not create a message loop thread and never calls `OnScheduleMessagePumpWork` in `CefBrowserProcessHandler`. The application must explicitly call `CefApp.doMessageLoopWork(long)` on its own schedule, with no notification from CEF about when work is needed. CEFFX provides no built-in driver for this mode - unlike `external_message_pump`, where `pump()` handles it automatically. |
| `true` | `false` | CEF | CEF | CEF creates and manages its own message loop thread. The application does not need to call `CefApp.doMessageLoopWork(long)`, and `OnScheduleMessagePumpWork` in `CefBrowserProcessHandler` is not called either, since CEF is not relying on the host to drive anything. |
| `false` | `true` | CEF | CEFFX | CEF does not create its own message loop thread. CEF calls `OnScheduleMessagePumpWork` in `CefBrowserProcessHandler` to tell the host when and after what delay to call `CefApp.doMessageLoopWork(long)`; CEFFX schedules and performs those calls automatically. |
| `true` | `true` | — | — | Invalid. These options must not be enabled simultaneously. |

### Threads and Message Loops <a name="usage-threads-and-loops"></a>

CEFFX introduces a dual-thread architecture. The first thread is the JavaFX Application Thread, on which all interactions
 with the JavaFX platform must be performed (using `Platform.runLater(...)`). The second thread is the CEF thread, on
which all interactions with CEF must be executed (using `CefApp.runLater(...)`).

There are two message loops running side by side: one for JavaFX, one for CEF. A message loop is code that keeps calling
 a single function over and over, for as long as the application runs, checking each time whether there's anything
pending to handle - and doing it if so - before returning immediately either way.

**The JavaFX loop** runs on the JavaFX Application Thread and drives everything JavaFX-related - mouse clicks, keyboard
 input, layout, drawing nodes on screen. This is managed entirely by JavaFX itself; you never touch it directly.

**The CEF loop** is what keeps CEF itself running - processing network responses, executing JavaScript, composing
rendered frames, and delivering all of that as callbacks back into your code (like `onTitleChange` when a page's title
changes). Unlike the JavaFX loop, CEF gives applications a choice in how this loop is driven - and that choice is where
the word "pump" comes in.

A **message pump** is a single call to the function that does one iteration of the CEF loop - `CefDoMessageLoopWork()`.
Calling it once checks whether CEF has anything pending right now and handles it if so, then returns. On Windows and
Linux, CEF pumps itself: it runs its loop on its own native thread, calling that function over and over with no help
from the application. On macOS, that mode isn't available, so the application has to call the pump function itself -
on the CEF thread CEFFX maintains for this purpose - one call at a time, whenever CEF asks for one. The settings above

The two loops never call into each other directly. Whenever work needs to cross from one side to the
other, it does so explicitly, by scheduling a task on the target thread:

- From JavaFX code, `CefApp.runLater(...)` hands a task to the CEF thread - for example, creating a new
  browser or navigating to a URL.
- From CEF callbacks (which run on the CEF thread), `Platform.runLater(...)` hands a task back to the
  JavaFX Application Thread - for example, updating a tab's title when the page's `<title>` changes:

```java
  client.addDisplayHandler(new CefDisplayHandlerAdapter() {
      @Override
      public void onTitleChange(CefBrowser browser, String title) {
          Platform.runLater(() -> tabPort.onTitleChanged(title));
      }
  });
```

Performing CEF operations outside the CEF thread, or touching JavaFX nodes outside the JavaFX Application
Thread, may lead to inconsistent behavior - some operations may work (always or intermittently), while
others may fail, sometimes with a CEF-related exception and sometimes silently. As a rule of thumb, if
something does not work as expected, first check which thread is being used.

### Native Deployment <a name="usage-native-deployment"></a>

CEFFX provides prebuilt native libraries, making it easy to integrate CEF into any JavaFX application without the need
to compile it from source.

CEF itself does not impose a single required deployment layout - instead it exposes a range of settings and mechanisms
(see [Path resolution reference](#usage-path-reference)) that different applications can combine
differently depending on their own needs. There is no one canonical configuration: natives can live inside the
application's own distributable package (for example, placed there during a `jpackage` build step) or outside it
entirely, in a directory of the application's own choosing - a per-user cache folder, a temp directory, wherever suits
the application. That directory can be populated once, statically, ahead of time, or dynamically, on demand, the first
time the application runs.

This range of choices shaped the API `ceffx-natives` provides: rather than a single fixed deployment procedure, the
module exposes a small, low-level building block (`NativeExtractor`) plus one ready-made, opinionated way of using it
(`NativeDeployer`) - not the only correct way to deploy CEFFX, just the one this project provides out of the box.
Applications with different needs are free to build their own deployment logic directly on top of `NativeExtractor`
and `NativeProps`.

### NativeProps <a name="usage-native-props"></a>

`NativeProps`, provided by the `ceffx-natives` module, exposes the CEF version and platform information
bundled into this classifier's jar as a set of static constants:

```java
NativeProps.CEF_VERSION          // e.g. "146.0.10+g8219561+chromium-146.0.7680.179"
NativeProps.CEF_PLATFORM         // e.g. "linux64", "macosx64", "macosarm64", "windows64"
NativeProps.CEF_DISTRIBUTION     // e.g. "cef_binary_146.0.10+g8219561+chromium-146.0.7680.179_linux64"
NativeProps.CEF_BASE_URL         // e.g. "https://cef-builds.spotifycdn.com/"
NativeProps.CEF_DOWNLOAD_URL     // CEF_BASE_URL + CEF_DISTRIBUTION + ".tar.bz2"
NativeProps.CEF_DOWNLOAD_URL_MIN // CEF_BASE_URL + CEF_DISTRIBUTION + "_minimal.tar.bz2"
NativeProps.CEFFX_CLASSIFIER     // e.g. "linux", "mac", "mac-aarch64", "win"
```

These values are read once, at class initialization, from a `native.properties` resource shipped inside this
classifier's jar alongside `NativeExtractor`'s payload. Both `NativeDeployer` and any custom deployment logic built
directly on `NativeExtractor` can rely on `NativeProps` to know exactly which CEF version and platform the jar was built
for, without hardcoding that information separately.

### NativeExtractor <a name="usage-native-extractor"></a>

`NativeExtractor`, provided by the `ceffx-natives` module, extracts the CEFFX native payload bundled
inside this classifier's jar - native libraries and, on macOS, the helper app bundles - flat into a target
directory on disk:

```java
List<String> extracted = NativeExtractor.extract(targetDir);
```

The returned, unmodifiable list contains the relative path of every file that was extracted, in the order
they were copied. A second overload reports progress as extraction proceeds:

```java
NativeExtractor.extract(targetDir, progress -> {
    System.out.printf("Extracting: %.0f%%%n", progress * 100);
});
```

`NativeExtractor` is intentionally minimal: it only copies files exactly as they are laid out in the jar, with no
platform-specific knowledge of any kind - not even on macOS, where CEF expects a particular directory structure relative
to the extracted files (see `NativeDeployer` below and the Path resolution reference). Arranging the extracted files
into whatever layout the runtime environment requires is left entirely to the caller, whether that caller is
`NativeDeployer` or custom deployment logic built directly on top of `NativeExtractor`.

This split carries negligible cost in practice: renaming or moving a file or directory within the same volume is a
cheap, near-instant operation on Linux, macOS and Windows alike - the underlying file system just updates a directory
entry, without copying any actual file content, regardless of file size. A caller that needs a different layout than the
flat one `NativeExtractor` produces can rearrange the extracted files afterward at effectively no extra cost, as
`NativeDeployer` does on macOS.

### NativeDeployer <a name="usage-native-deployer"></a>

`NativeDeployer`, provided by the `ceffx-natives` module, is CEFFX's own external, dynamic deployment strategy built
on top of `NativeExtractor` - downloading the CEF distribution archive, extracting it, and extracting the CEFFX native
payload alongside it, arranging everything into the layout each platform requires. It is not the only valid way to
deploy CEFFX (see `NativeExtractor` above), just the one this project provides out of the box.

```java
NativeDeployer.deploy(targetDir, (operation, progress) -> {
    System.out.printf("%s: %.0f%%%n", operation, progress * 100);
});
```

Deployment consists of three independently tracked steps - `DOWNLOAD_CEF`, `EXTRACT_CEF` and `EXTRACT_CEFFX` - each
recording what it produced in `targetDir`, so repeated calls are cheap and safe:

```java
var status = NativeDeployer.getStatus(targetDir);
```

`getStatus` returns which steps have already completed, and `deploy` skips whatever is already done. This means `deploy`
can safely be called on every application startup: after the first run it becomes close to a no-op, and if a previous
run was interrupted partway through, the next call resumes only the unfinished steps.

To force one or more steps to re-run regardless of their current status - for example after upgrading to a
newer CEFFX version - pass them explicitly:

```java
NativeDeployer.deploy(targetDir, listener, EnumSet.allOf(NativeDeployer.Operation.class));
```

Forcing one operation does not cascade to any operation it would normally depend on - forcing `DOWNLOAD_CEF` alone
does not force `EXTRACT_CEF` to re-extract the newly downloaded archive. Combine operations explicitly (typically all
three, for a full clean redeploy).

**JPMS users:** `NativeDeployer` requires
[Apache Commons Compress](https://commons.apache.org/proper/commons-compress/) to extract the CEF archive.
This dependency is declared as `requires static` in `ceffx-natives`'s `module-info.java`, so it is not
added to the runtime module graph automatically. It must be declared explicitly in the application's
`module-info.java`:

```java
module com.example.myapp {
    requires com.techsenger.ceffx.natives;
    requires org.apache.commons.compress;
}
```

Applications running on the classpath (unnamed module) are unaffected - Commons Compress is a regular
Maven dependency of `ceffx-natives` and is already present on the classpath either way.

**NativeDeployer operations**:

1. `DOWNLOAD_CEF` downloads the minimal CEF distribution archive matching `NativeProps.CEF_VERSION` and
   `NativeProps.CEF_PLATFORM`, verifying it against the official SHA-1 checksum before use.
2. `EXTRACT_CEF` extracts that archive into `targetDir`. On Linux and Windows, the contents of the archive's
   `Release` and `Resources` folders are merged directly into `targetDir`:

   Linux
   ```
   targetDir
   ├── chrome_100_percent.pak
   ├── chrome_200_percent.pak
   ├── chrome-sandbox
   ├── icudtl.dat
   ├── libcef.so
   ├── libEGL.so
   ├── libGLESv2.so
   ├── libvk_swiftshader.so
   ├── libvulkan.so.1
   ├── locales/
   ├── resources.pak
   ├── v8_context_snapshot.bin
   └── vk_swiftshader_icd.json
   ```

   Windows
   ```
   targetDir
   ├── chrome_100_percent.pak
   ├── chrome_200_percent.pak
   ├── d3dcompiler_47.dll
   ├── icudtl.dat
   ├── libcef.dll
   ├── libEGL.dll
   ├── libGLESv2.dll
   ├── locales/
   ├── resources.pak
   ├── snapshot_blob.bin
   ├── v8_context_snapshot.bin
   ├── vk_swiftshader_icd.json
   └── vulkan-1.dll
   ```

   macOS

   The archive's self-contained `Chromium Embedded Framework.framework` directory - already including all
   resources, locales and `.pak` files - is placed under a `Frameworks` subdirectory instead:

   ```
   targetDir
   └── Frameworks/
       └── Chromium Embedded Framework.framework/
   ```

3. `EXTRACT_CEFFX` then extracts the CEFFX native payload via `NativeExtractor`. On Linux and Windows, this
   adds `libceffx.so`/`ceffx.dll` and the `ceffx_helper` executable, flat, alongside the files above. On
   macOS, `libceffx.dylib` is placed into a `Lib` subdirectory, and all five `ceffx Helper*.app` bundles
   (base, GPU, Plugin, Renderer, Alerts) are placed into `Frameworks`, alongside the framework:

   ```
   targetDir
   ├── Lib/
   │   └── libceffx.dylib
   └── Frameworks/
       ├── Chromium Embedded Framework.framework/
       ├── ceffx Helper.app/
       ├── ceffx Helper (GPU).app/
       ├── ceffx Helper (Plugin).app/
       ├── ceffx Helper (Renderer).app/
       └── ceffx Helper (Alerts).app/
   ```

   The `Lib` subdirectory exists because CEF resolves the helper app bundles and the framework relative to
   wherever `libceffx.dylib` is loaded from - specifically, as `../Frameworks`. Since `Frameworks` must stay
   inside `targetDir` rather than a sibling of it, `libceffx.dylib` is placed one level deeper, in `Lib`, so
   that `../Frameworks` from there correctly resolves back to `targetDir/Frameworks`. This layout is required
   by CEF on macOS and is not optional - unlike Linux and Windows, there is no way to point CEF at a fully
   flat directory instead.

**Library Path**

To work with the `NativeDeployer` configuration is it necessary to set `java.library.path`:

Linux and Windows - point `java.library.path` at `targetDir`:
```
-Djava.library.path=<targetDir>
```
CEF locates its helper executable and resources relative to the loaded library by default; no further configuration
is required.

macOS - since the native library now lives in `targetDir/Lib` rather than `targetDir` itself, point
`java.library.path` there instead, or include both entries (separated by `path.separator`) if some other part of your
code still expects `targetDir` on the path:
```
-Djava.library.path=<targetDir>/Lib
```
See the Path resolution reference below for the additional command-line switches macOS needs when running
outside a real `.app` bundle - for example via `mvn javafx:run`, where no such bundle exists at all.

**CEF initialization**

```
var macFrameworkDir = path.resolve("Frameworks").resolve("Chromium Embedded Framework.framework");
CefApp.startup(new String[]{ "--framework-dir-path=" + macFrameworkDir.toAbsolutePath()});
CefApp.addAppHandler(new CefAppHandlerAdapter(null) {
    @Override
    public void onBeforeCommandLineProcessing(String processType, CefCommandLine commandLine) {
        if (NativeProps.CEF_PLATFORM.startsWith("mac")) {
            commandLine.appendSwitchWithValue("framework-dir-path", macFrameworkDir.toAbsolutePath().toString());
            var mainBundlePath = path.resolve("Frameworks").resolve("ceffx Helper.app");
            commandLine.appendSwitchWithValue("main-bundle-path", mainBundlePath.toAbsolutePath().toString());
        }
    }
});
```

#### Path resolution reference <a name="usage-path-reference"></a>

The following settings and mechanisms determine where CEF looks for the helper executable, resources and (on macOS) the
framework and helper app bundles. Most fall back to a value computed relative to `<library path>` - the directory found
by scanning `java.library.path` for `libceffx.dylib`, `libceffx.so` or `ceffx.dll` - unless overridden explicitly.

| Mechanism | Platform | Behavior |
|---|---|---|
| `CefSettings.browser_subprocess_path` | Linux | If `null`, computed as `<library path>/ceffx_helper`. |
| `CefSettings.browser_subprocess_path` | Windows | If `null`, computed as `<library path>/ceffx_helper.exe`. |
| `CefSettings.browser_subprocess_path` | macOS | If `null`, computed as `<library path>/../Frameworks/ceffx Helper.app/Contents/MacOS/ceffx Helper`. |
| `CefSettings.resources_dir_path` | Linux | If `null`, computed as `<library path>`. |
| `CefSettings.locales_dir_path` | Linux | If `null`, computed as `<library path>/locales`. |
| `"--framework-dir-path"` CEFFX startup argument | macOS | Not a CEF setting or native command-line switch itself - a CEFFX-internal `args[]` convention parsed entirely in Java by `getCefFrameworkPath()`, before native startup. The resulting path is passed straight into `cef_load_library()` (a `dlopen` equivalent) via `N_Startup`, to load the framework binary itself, prior to any `CefSettings`/`CefInitialize` call. If not passed, computed as `<library path>/../Frameworks/Chromium Embedded Framework.framework`. |
| `framework-dir-path` Chromium command-line switch | macOS | Not set by CEFFX itself - this is a real Chromium switch, distinct from the CEFFX startup argument above. When running outside a real `.app` bundle, the application must add it explicitly via `CefCommandLine.appendSwitchWithValue("framework-dir-path", ...)` inside its own `CefAppHandler.onBeforeCommandLineProcessing()`, pointing at the same framework path passed to `startup()`. Without it, resource lookups that go through Chromium's own bundle resolution (such as `icudtl.dat`) fail even though the framework binary itself loaded correctly via `dlopen`. |
| `main-bundle-path` Chromium command-line switch | macOS | Not set by CEFFX itself, and no `CefSettings` field exists for it. Chromium derives the Mach rendezvous name subprocesses use to connect back to the browser process from this bundle's identifier, expecting each helper's own identifier to match as `<id>.helper[.suffix]`. Outside a real `.app` bundle there is no such identifier to derive from, so subprocesses fail with "No rendezvous client" unless the application adds this switch explicitly via `onBeforeCommandLineProcessing`, pointing at `ceffx Helper.app` itself - which happens to carry the same fixed identifier the shipped helper bundles were built with. |
| `java.library.path` scanning | All | The mechanism `browser_subprocess_path`/`resources_dir_path`/`locales_dir_path` fall back to when unset: CEF scans each entry in `java.library.path` and uses whichever one actually contains the CEFFX native library. |

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

# Build the project with natives for the current OS
mvn install

# Build the project with natives for all OSes
mvn install -P linux,mac,mac-aarch64,windows
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
