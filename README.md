# Retrospectum Mod Template

Starter project for Minecraft 1.6.4 / Forge 9.11.1.1345 with Retrospectum Loom
and the shared Retrospectum Mixin bootstrap.

## Start

1. Use this template, clone it, and run setup.
2. Edit mod metadata in `gradle.properties` and the project name in `settings.gradle`.
3. Build and generate IDE configurations:

   ```powershell
   # Windows x64, PowerShell 5.1+
   ./setup.ps1
   ./build.ps1 build genSources genIdeaRuns
   ```

   ```sh
   # Linux x86_64 with glibc, Bash, curl, tar and GNU utilities
   ./setup.sh
   ./build.sh build genSources genIdeaRuns
   ```

4. Open the project in IntelliJ IDEA, select JDK 17 for its Gradle JVM, and let
   Gradle import finish. Start the generated Retrospectum client/server Gradle
   configuration, or run `./build.ps1 runClient` (`./build.sh runClient` on Linux).
5. Distribute `build/libs/<mod_id>-retrospectum-1.6.4-<version>.jar` and require
   Retrospectum Mixin alongside it. The `-dev.jar` is for development only.

If Windows blocks the script, invoke it with
`powershell -NoProfile -ExecutionPolicy Bypass -File ./setup.ps1` (and similarly
for `build.ps1`). This changes policy only for that invocation.

JDK 17 runs Gradle, JDK 8 compiles/AP, and Java 7 or 8 runs the game. Setup finds
installed JDKs or installs pinned, verified packages under project `.jdks/`.
Machine-local paths are recorded in ignored files; setup creates no global SDK links.
macOS, ARM and musl Linux are outside the supported setup/test matrix.

## Automated builds

The **Build mod** GitHub Actions workflow runs on pushes and pull requests, and
can also be started manually. It builds this project using the Loom and Mixin
versions in `gradle.properties`, with Java 7 APIs, the Java 8 compiler and Gradle
on Java 17.

Download `mod-<commit SHA>` from the workflow run's **Artifacts** section to get
the distributable SRG JAR. Development (`-dev.jar`) archives are excluded.
Artifacts are retained for 14 days. Failed builds upload available reports
for 7 days. Install Retrospectum Mixin separately alongside the mod.

## Next steps

- [Development, Mixin mapping and local toolchain verification](docs/DEVELOPMENT.md)
- [Compatibility](docs/COMPATIBILITY.md), [troubleshooting](docs/TROUBLESHOOTING.md)
- [Loom reference](https://github.com/DesideriumMC/RetrospectumLoom/blob/main/docs/REFERENCE.md)

MIT; see [LICENSE](LICENSE). Keep upstream notices and choose the appropriate
license and copyright statement for your own mod before publishing it.
