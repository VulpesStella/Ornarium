# Development

Setup records JDK selections in `.jdks/homes.properties` (used by build wrappers)
and `gradle.local.properties` (used by Gradle/IDE import). Explicit `-P` properties
win. Run setup again after moving or removing a JDK. `-Download` / `--download`
forces project-local packages. `JAVA7_HOME`, `JAVA8_HOME`, `JAVA17_HOME` and
`JDK7`, `JDK8`, `JDK17` can supply existing installations.

Only `example.BuildConstants` is generated from mod metadata; user Java sources
are compiled in place. If renaming the example package, also update the constant
output path and package in `generateModConstants`. Authors in `mcmod.info` come
from the comma-separated `mod_authors` property.

The example injects into world constructors with `<init>*` and `remap=false`:
constructor names do not change, while side-specific descriptors can differ.
For normal Minecraft method/field targets keep remapping enabled. The annotation
processor writes an SRG refmap; development launches remap it back to MCP, and
`remapJarForRelease` emits the SRG release mod. Do not hand-edit generated refmaps.

Use `modImplementation`, `modCompileOnly` or `modRuntimeOnly` for other mods in
SRG distribution names; Loom remaps them for development. Plain libraries can
use standard `implementation` / `compileOnly` / `runtimeOnly` configurations.
Project dependencies such as `modImplementation project(':other-mod')` build
their producer archives before remapping. The producer must expose an SRG
distribution archive, as another Loom project does for its published Java variants.
Dependency remapping runs as Gradle tasks under `build/retromixin/dependencies`;
after a clean checkout, build once before inspecting those generated JARs in an IDE.
Publishing is explicitly enabled in the template; `mod` uses `components.java`.
Minecraft and Forge development libraries are excluded from published variants.

For source development set `retrospectumLoomCheckout` to a Loom checkout.
Set `mixin_bootstrap` to a built shared runtime JAR, or
`retrospectumMixinCheckout` to a checkout whose built runtime matches `mixin_version`.
`retrospectumMavenUrl` overrides the hosted Maven repository for both components.

Run an isolated full verification after building Mixin:

```sh
python scripts/verify_toolchain.py --loom /path/to/RetrospectumLoom --mixin /path/to/retrospectum-mixin-VERSION.jar --output build/verification
```

Use JDK 17 as `JAVA_HOME`; add `--gradle-property java7Home=/path/to/jdk7` and
`--gradle-property org.gradle.java.installations.paths=/path/to/jdk7,/path/to/jdk8`
if setup/discovery is not available. The output directory must be new.
The test uses a copy of the template, probes a mapped `getWorldTime` injection,
checks publication scopes and generated IDE arguments, then starts development
and original Forge servers and verifies save/stop. Logs and `result.json` stay
under the output directory. Game archives are never committed.
It also publishes to an isolated Maven repository, compiles and runs an independent
Java 7 consumer, rejects a Java 6 consumer, and checks fresh multi-project builds,
unchanged dependency remapping and producer changes.

## Runtime verification

Add `--game-java /path/to/jdk7/bin/java --game-java /path/to/jdk8/bin/java` to
exercise both required runtime families, each with isolated development and
production worlds. The probes also check side stripping, Forge events, coremod
transformation, mapped fields, incremental remapping and failure recovery.
`--bytecode-target 7` is experimental: it raises the mod validation ceiling only
in a disposable Loom checkout under the output directory. The supported plugin
continues to target/require Java 6 mod bytecode with the Java 7 API boundary.
The separate `verify_stackmaps.py` probe intentionally strips frames and checks
expected verifier behavior; this is not a claim about stock Forge behavior.
