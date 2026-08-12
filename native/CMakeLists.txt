# Copyright (c) 2016 The Chromium Embedded Framework Authors. All rights
# reserved. Use of this source code is governed by a BSD-style license that
# can be found in the LICENSE file.

# OVERVIEW
#
# CMake is a cross-platform open-source build system that can generate project
# files in many different formats. It can be downloaded from
# http://www.cmake.org or installed via a platform package manager.
#
# CMake-generated project formats that have been tested with CEFFX include:
#
# Linux:      Ninja, GCC 7.5.0+, Unix Makefiles
# MacOS:      Ninja, Xcode 13.3 to 16.4
# Windows:    Ninja, Visual Studio 2022
#
# Ninja is a cross-platform open-source tool for running fast builds using
# pre-installed platform toolchains (GNU, clang, Xcode or MSVC). It can be
# downloaded from http://martine.github.io/ninja/ or installed via a platform
# package manager.
#
# BUILD REQUIREMENTS
#
# The below requirements must be met to build CEFFX.
#
# - CMake version 3.21 or newer.
#
# - Linux requirements:
#   Currently supported distributions include Debian 10 (Buster), Ubuntu 18
#   (Bionic Beaver), and related. Ubuntu 18.04 64-bit with GCC 7.5.0+ is
#   recommended. Newer versions will likely also work but may not have been
#   tested.
#   Required packages include:
#     build-essential
#     libgtk3.0-dev     (required by the cefclient target only)
#
# - MacOS requirements:
#   Xcode 13.5 to 16.4 building on MacOS 12.0 (Monterey) or newer. The Xcode
#   command-line tools must also be installed. Newer Xcode versions may not have
#   been been tested and are not recommended.
#
# - Windows requirements:
#   Visual Studio 2022 building on Windows 10 or newer. Windows 10/11 64-bit is
#   recommended. Newer versions will likely also work but may not have been
#   tested.
#
# BUILD EXAMPLES
#
# The below commands will generate project files and create a Debug build of all
# CEFFX native targets using CMake and the platform toolchain.
#
# Start by creating and entering the CMake build output directory. The
#`ceffx_build` directory name is required by other CEFFX tooling (specifically the
# tools/make_distrib.[bat|sh] and tools/run.[bat|sh] scripts) and should not be
# changed.
# > cd path/to/java-cef/src
# > mkdir ceffx_build && cd ceffx_build
#
# To perform a Linux build using a 32-bit CEF binary distribution on a 32-bit
# Linux platform or a 64-bit CEF binary distribution on a 64-bit Linux platform:
#   Using Unix Makefiles:
#     > cmake -G "Unix Makefiles" -DCMAKE_BUILD_TYPE=Debug ..
#     > make -j4
#
#   Using Ninja:
#     > cmake -G "Ninja" -DCMAKE_BUILD_TYPE=Debug ..
#     > ninja
#
# To perform a MacOS build using a 64-bit CEF binary distribution:
#   Using the Xcode IDE:
#     > cmake -G "Xcode" -DPROJECT_ARCH="x86_64" ..
#     Open ceffx.xcodeproj in Xcode and select Product > Build.
#
#   Using Ninja:
#     > cmake -G "Ninja" -DPROJECT_ARCH="x86_64" -DCMAKE_BUILD_TYPE=Debug ..
#     > ninja
#
# To perform a MacOS build using an ARM64 CEF binary distribution:
#   Using the Xcode IDE:
#     > cmake -G "Xcode" -DPROJECT_ARCH="arm64" ..
#     Open ceffx.xcodeproj in Xcode and select Product > Build.
#
#   Using Ninja:
#     > cmake -G "Ninja" -DPROJECT_ARCH="arm64" -DCMAKE_BUILD_TYPE=Debug ..
#     > ninja
#
# To perform a Windows build using a 32-bit CEF binary distribution:
#   Using the Visual Studio 2022 IDE:
#     > cmake -G "Visual Studio 17" -A Win32 ..
#     Open ceffx.sln in Visual Studio and select Build > Build Solution.
#
#   Using Ninja with Visual Studio 2022 command-line tools:
#     (this path may be different depending on your Visual Studio installation)
#     > "C:\Program Files\Microsoft Visual Studio\2022\Professional\VC\Auxiliary\Build\vcvars32.bat"
#     > cmake -G "Ninja" -DCMAKE_BUILD_TYPE=Debug ..
#     > ninja
#
# To perform a Windows build using a 64-bit CEF binary distribution:
#   Using the Visual Studio 2022 IDE:
#     > cmake -G "Visual Studio 17" -A x64 ..
#     Open ceffx.sln in Visual Studio and select Build > Build Solution.
#
#   Using Ninja with Visual Studio 2022 command-line tools:
#     (this path may be different depending on your Visual Studio installation)
#     > "C:\Program Files\Microsoft Visual Studio\2022\Professional\VC\Auxiliary\Build\vcvars64.bat"
#     > cmake -G "Ninja" -DCMAKE_BUILD_TYPE=Debug ..
#     > ninja

#
# Shared configuration.
#

# For VS2022 and Xcode 12+ support.
cmake_minimum_required(VERSION 3.21)

# Only generate Debug and Release configuration types.
set(CMAKE_CONFIGURATION_TYPES Debug Release)

# Project name.
project(ceffx)

# Use folders in the resulting project files.
set_property(GLOBAL PROPERTY OS_FOLDERS ON)


#
# CEF configuration.
#

# Specify the CEF distribution version.
if(NOT DEFINED CEF_VERSION)
  set(CEF_PROPERTIES_FILE "${CMAKE_SOURCE_DIR}/../cef.properties")
  if(NOT EXISTS "${CEF_PROPERTIES_FILE}")
    message(FATAL_ERROR "cef.properties not found at ${CEF_PROPERTIES_FILE}")
  endif()
  file(STRINGS "${CEF_PROPERTIES_FILE}" CEF_VERSION_LINE REGEX "^cef\\.version=")
  if(NOT CEF_VERSION_LINE)
    message(FATAL_ERROR "cef.version property not found in ${CEF_PROPERTIES_FILE}")
  endif()
  string(REGEX REPLACE "^cef\\.version=" "" CEF_VERSION "${CEF_VERSION_LINE}")
  message(STATUS "Read CEF_VERSION from cef.properties: ${CEF_VERSION}")
endif()

# Determine the platform.
if("${CMAKE_SYSTEM_NAME}" STREQUAL "Darwin")
  if("${PROJECT_ARCH}" STREQUAL "arm64")
    set(CEF_PLATFORM "macosarm64")
  elseif("${PROJECT_ARCH}" STREQUAL "x86_64")
    set(CEF_PLATFORM "macosx64")
  elseif("${CMAKE_HOST_SYSTEM_PROCESSOR}" STREQUAL "arm64")
    set(PROJECT_ARCH "arm64")
    set(CEF_PLATFORM "macosarm64")
  else()
    set(PROJECT_ARCH "x86_64")
    set(CEF_PLATFORM "macosx64")
  endif()
elseif("${CMAKE_SYSTEM_NAME}" STREQUAL "Linux")
  if(CMAKE_SIZEOF_VOID_P MATCHES 8)
    set(CEF_PLATFORM "linux64")
  else()
    set(CEF_PLATFORM "linux32")
  endif()
elseif("${CMAKE_SYSTEM_NAME}" STREQUAL "Windows")
  if(CMAKE_SIZEOF_VOID_P MATCHES 8)
    set(CEF_PLATFORM "windows64")
  else()
    set(CEF_PLATFORM "windows32")
  endif()
endif()

# Add this project's cmake/ directory to the module path.
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_CURRENT_SOURCE_DIR}/cmake")

# Download and extract the CEF binary distribution (executes DownloadCEF.cmake).
include(DownloadCEF)
DownloadCEF("${CEF_PLATFORM}" "${CEF_VERSION}" "${CMAKE_SOURCE_DIR}/cef")

# Add the CEF binary distribution's cmake/ directory to the module path.
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CEF_ROOT}/cmake")

# Load the CEF configuration (executes FindCEF.cmake).
find_package(CEF REQUIRED)


#
# Python configuration.
#

# Support specification of the Python executable path via the command-line.
if(DEFINED ENV{PYTHON_EXECUTABLE})
  file(TO_CMAKE_PATH "$ENV{PYTHON_EXECUTABLE}" PYTHON_EXECUTABLE)
endif()

if(NOT PYTHON_EXECUTABLE)
  unset(PYTHON_EXECUTABLE)

  # Find the python interpreter.
  find_package(PythonInterp)

  if(NOT ${PYTHONINTERP_FOUND})
    message(FATAL_ERROR "A Python installation is required. Set the "
                        "PYTHON_EXECUTABLE environment variable to explicitly "
                        "specify the Python executable path.")
  endif()
endif()

message(STATUS "Using Python: ${PYTHON_EXECUTABLE}")


#
# Java configuration.
#

# Minimum required Java version.
set(JDK_MIN_VERSION 1.7)

set(JAVA_FATAL_ERROR "A Java installation is required. Set the JAVA_HOME "
                     "environment variable to explicitly specify the Java "
                     "installation directory.")

# Find the Java Native Interface (JNI) installation.
find_package(JNI ${JDK_MIN_VERSION})
if(NOT ${JNI_FOUND})
  message(FATAL_ERROR ${JAVA_FATAL_ERROR})
endif()

if(OS_MACOSX)
  # OS X stores the Java binaries separately from the JNI includes/libraries.
  # Find the Java development installation.
  find_package(Java ${JDK_MIN_VERSION} COMPONENTS Development)

  if(NOT ${Java_FOUND})
    message(FATAL_ERROR ${JAVA_FATAL_ERROR})
  endif()

  # Determine the root path for the Java installation.
  # Remove "bin/javac" from the path.
  get_filename_component(JAVA_DIR ${Java_JAVAC_EXECUTABLE} DIRECTORY)
  get_filename_component(JAVA_DIR ${JAVA_DIR} DIRECTORY)
else()
  # Determine the root path for the Java installation.
  # Remove "include" from the path.
  get_filename_component(JAVA_DIR ${JAVA_INCLUDE_PATH} DIRECTORY)
endif()


#
# Post-configuration actions.
#

# Generate the CEFFX version header.
message(STATUS "Generating native/ceffx_version.h file...")
execute_process(
  COMMAND "${PYTHON_EXECUTABLE}"
          "tools/make_version_header.py"
          "--header"
          "src/ceffx_version.h"
          "--cef-path"
          "${CEF_ROOT}"
  WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
  RESULT_VARIABLE EXECUTE_RV
  )
if(NOT EXECUTE_RV STREQUAL "0")
  message(FATAL_ERROR "Execution failed with unexpected result: ${EXECUTE_RV}")
endif()

# Copy the CEF README.txt file to the cmake build directory for use by the
# make_readme.py script.
file(COPY "${CEF_ROOT}/README.txt" DESTINATION "${CMAKE_BINARY_DIR}")

# Download clang-format directly from Google Storage.
if(OS_WINDOWS)
  set(CF_HASHFILE "tools/buildtools/win/clang-format.exe.sha1")
  set(CF_OUTPATH "${CMAKE_CURRENT_SOURCE_DIR}/tools/buildtools/win/clang-format.exe")
elseif(OS_MACOSX)
  if("${CMAKE_HOST_SYSTEM_PROCESSOR}" STREQUAL "arm64")
    set(CF_HASHFILE "tools/buildtools/mac/clang-format.arm64.sha1")
  else()
    set(CF_HASHFILE "tools/buildtools/mac/clang-format.x64.sha1")
  endif()
  set(CF_OUTPATH "${CMAKE_CURRENT_SOURCE_DIR}/tools/buildtools/mac/clang-format")
elseif(OS_LINUX)
  set(CF_HASHFILE "tools/buildtools/linux64/clang-format.sha1")
  set(CF_OUTPATH "${CMAKE_CURRENT_SOURCE_DIR}/tools/buildtools/linux64/clang-format")
endif()

message(STATUS "Downloading clang-format from Google Storage...")
file(READ "${CMAKE_CURRENT_SOURCE_DIR}/${CF_HASHFILE}" CF_HASH)
string(STRIP "${CF_HASH}" CF_HASH)
file(DOWNLOAD
  "https://storage.googleapis.com/chromium-clang-format/${CF_HASH}"
  "${CF_OUTPATH}"
  STATUS DOWNLOAD_STATUS
  SHOW_PROGRESS
)
list(GET DOWNLOAD_STATUS 0 DOWNLOAD_STATUS_CODE)
if(NOT DOWNLOAD_STATUS_CODE EQUAL 0)
  message(FATAL_ERROR "Failed to download clang-format: ${DOWNLOAD_STATUS}")
endif()
if(NOT OS_WINDOWS)
  file(CHMOD "${CF_OUTPATH}" PERMISSIONS
    OWNER_READ OWNER_WRITE OWNER_EXECUTE
    GROUP_READ GROUP_EXECUTE
    WORLD_READ WORLD_EXECUTE)
endif()

#
# Include target subdirectories.
#
# Determine the classifier used to name the native output directory, matching
# the Maven classifiers used by the ceffx-natives module (linux, mac,
# mac-aarch64, win).
if(OS_LINUX)
  set(NATIVES_CLASSIFIER "linux")
elseif(OS_MAC)
  if("${PROJECT_ARCH}" STREQUAL "arm64")
    set(NATIVES_CLASSIFIER "mac-aarch64")
  else()
    set(NATIVES_CLASSIFIER "mac")
  endif()
elseif(OS_WINDOWS)
  set(NATIVES_CLASSIFIER "win")
endif()

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/Release/${NATIVES_CLASSIFIER})
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/Release/${NATIVES_CLASSIFIER})
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/Release/${NATIVES_CLASSIFIER})

add_subdirectory(${CEF_LIBCEF_DLL_WRAPPER_PATH} libcef_dll_wrapper)
add_subdirectory(src)


#
# Display configuration settings.
#

PRINT_CEF_CONFIG()

message(STATUS "*** CEFFX CONFIGURATION SETTINGS ***")
message(STATUS "Python executable:            ${PYTHON_EXECUTABLE}")
message(STATUS "Java directory:               ${JAVA_DIR}")
message(STATUS "JNI libraries:                ${JNI_LIBRARIES}")
message(STATUS "JNI include directories:      ${JNI_INCLUDE_DIRS}")
