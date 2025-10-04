cmake_minimum_required(VERSION 3.15)
project(analytics-service)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find packages
find_package(Threads REQUIRED)

# External dependencies via FetchContent
include(FetchContent)

# cpp-httplib for REST API
FetchContent_Declare(
    httplib
    GIT_REPOSITORY https://github.com/yhirose/cpp-httplib.git
    GIT_TAG v0.14.0
)

# nlohmann/json for JSON parsing
FetchContent_Declare(
    json
    GIT_REPOSITORY https://github.com/nlohmann/json.git
    GIT_TAG v3.11.2
)

# prometheus-cpp for metrics
FetchContent_Declare(
    prometheus-cpp
    GIT_REPOSITORY https://github.com/jupp0r/prometheus-cpp.git
    GIT_TAG v1.1.0
)

FetchContent_MakeAvailable(httplib json prometheus-cpp)

# Source files
set(SOURCES
    src/main.cpp
)

# Executable
add_executable(analytics-service ${SOURCES})

# Include directories
target_include_directories(analytics-service PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Link libraries
target_link_libraries(analytics-service PRIVATE
    Threads::Threads
    nlohmann_json::nlohmann_json
    prometheus-cpp::core
    prometheus-cpp::pull
)

# Compiler options
target_compile_options(analytics-service PRIVATE
    -Wall
    -Wextra
    -Wpedantic
    -O3
)

# Install
install(TARGETS analytics-service
    RUNTIME DESTINATION bin
)