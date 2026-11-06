"""Build an isolated template and prove remapped Mixins in development and Forge.

Requires JDKs 7/8/17 (setup scripts or explicit Gradle properties). Never uses an
existing game world. The output directory must not already exist.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import queue
import re
import struct
import shutil
import subprocess
import threading
import time
import urllib.request
import urllib.error
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
MARKER = '[RETROSPECTUM-TEST] mapped method executed'


def run_server(command, directory, log_path, timeout=180):
    directory.mkdir(parents=True, exist_ok=True)
    (directory / 'server.properties').write_text(
        'server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\n'
        'level-name=test-world\nview-distance=3\nlevel-type=FLAT\n')
    process = subprocess.Popen(command, cwd=directory, stdin=subprocess.PIPE,
                               stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                               text=True, encoding='utf-8', errors='replace')
    lines = queue.Queue()

    def read():
        for line in process.stdout:
            lines.put(line)
        lines.put(None)

    threading.Thread(target=read, daemon=True).start()
    text, stopped = '', False
    deadline = time.monotonic() + timeout
    try:
        with log_path.open('w', encoding='utf-8') as log:
            while time.monotonic() < deadline:
                try:
                    line = lines.get(timeout=0.5)
                except queue.Empty:
                    continue
                if line is None:
                    break
                log.write(line)
                log.flush()
                text += line
                if 'Encountered an unexpected exception' in line:
                    raise RuntimeError(f'Server crashed: {log_path}')
                if not stopped and MARKER in text and 'Done (' in text:
                    process.stdin.write('save-all\nstop\n')
                    process.stdin.flush()
                    stopped = True
            else:
                raise RuntimeError(f'Server timeout: {log_path}')
        code = process.wait(timeout=20)
        if code or not stopped or 'Saving chunks' not in text:
            raise RuntimeError(f'Mixin/ready/save/stop failed (exit={code}): {log_path}')
    finally:
        if process.poll() is None:
            process.kill()
            process.wait()


def manifest_attributes(jar):
    with zipfile.ZipFile(jar) as archive:
        text = archive.read('META-INF/MANIFEST.MF').decode().replace('\r\n', '\n')
    return dict(line.split(': ', 1) for line in text.replace('\n ', '').split('\n\n')[0].splitlines()
                if ': ' in line)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--loom', type=Path, required=True)
    parser.add_argument('--mixin', type=Path, required=True, help='Built shared runtime JAR')
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--remapper-probe', type=Path)
    parser.add_argument('--game-java', type=Path, action='append', default=[])
    parser.add_argument('--bytecode-target', type=int, choices=(6, 7), default=6)
    parser.add_argument('--gradle-property', action='append', default=[])
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    inputs = output / 'inputs'
    inputs.mkdir()
    runtime = inputs / args.mixin.name
    shutil.copy2(args.mixin, runtime)
    loom = args.loom.resolve()
    if args.bytecode_target == 7:
        # The supported plugin deliberately rejects major 51 mod classes. Raise
        # that ceiling only in a disposable test checkout, never the real plugin.
        loom = output / 'experimental-loom'
        shutil.copytree(args.loom, loom, ignore=shutil.ignore_patterns('.git', '.gradle', 'build', '.idea'))
        validation = loom / 'src/main/java/net/fabricmc/loom/ModValidation.java'
        source = validation.read_text(encoding='utf-8')
        assert 'major > 50' in source
        validation.write_text(source.replace('major > 50', 'major > 51'), encoding='utf-8')
    fixture = output / 'template'
    shutil.copytree(ROOT, fixture, ignore=shutil.ignore_patterns(
        '.git', '.gradle', '.jdks', 'build', 'run', '.idea', '.vscode', '*.launch', '__pycache__', '.work'))
    shutil.copytree(ROOT / 'scripts/fixtures/toolchain/java', fixture / 'src/main/java', dirs_exist_ok=True)
    mod = fixture / 'src/main/java/example/ExampleMod.java'
    source = mod.read_text(encoding='utf-8')
    position = source.rfind('}')
    mod.write_text(source[:position] + '''    @Mod.EventHandler
    public void started(cpw.mods.fml.common.event.FMLServerStartedEvent event) {
        MappingProbe.verify(net.minecraftforge.common.DimensionManager.getWorld(0));
    }
''' + source[position:], encoding='utf-8')
    config = fixture / 'src/main/resources/mod.mixins.json'
    mixin = json.loads(config.read_text(encoding='utf-8'))
    mixin['compatibilityLevel'] = 'JAVA_' + str(args.bytecode_target)
    config.write_text(json.dumps(mixin, indent=2) + '\n', encoding='utf-8')
    (fixture / 'verification-extra.srg').write_text('# initial mapping probe\n', encoding='utf-8')
    # Known dependencies exercise standard publication scope and exclusion handling.
    with (fixture / 'build.gradle').open('a', encoding='utf-8') as build:
        build.write("""
apply plugin: 'java-library'
publishing.repositories.maven { name = 'Verification'; url = layout.buildDirectory.dir('verification-maven') }
if (providers.gradleProperty('verification.invalidInput').isPresent()) {
    tasks.named('remapJarForRelease') { input.set(file('invalid-input.jar')) }
}
tasks.named('remapJarForRelease') { mixinMappings.from(file('verification-extra.srg')) }
tasks.named('jar', Jar) {
    manifest.attributes('FMLCorePlugin': 'toolchain.coremod.ProbeCoremod', 'FMLCorePluginContainsFMLMod': 'true')
}
retrospectum.runConfigs.all { property('fml.coreMods.load', 'toolchain.coremod.ProbeCoremod') }
dependencies {
    api('commons-io:commons-io:2.4') { exclude group: 'test.excluded', module: 'unused' }
    implementation 'org.apache.commons:commons-lang3:3.1'
    modImplementation 'com.google.code.gson:gson:2.2.2'
    modCompileOnly 'junit:junit:4.13.2'
}
tasks.register('verifyForgeBinpatches') {
    doLast {
        def directory = layout.buildDirectory.dir('verification/forge').get().asFile
        copy {
            from zipTree(configurations.forge.singleFile)
            include 'binpatches.pack.lzma'
            into directory
        }
        def patches = new net.fabricmc.loom.mcp.Binpatch.Pack().read(new File(directory, 'binpatches.pack.lzma').toPath())
        assert patches.client.modificationCount > 0
        assert patches.server.modificationCount > 0
    }
}
""")
    with (fixture / 'build.gradle').open('a', encoding='utf-8') as build:
        build.write("\ntasks.withType(JavaCompile).configureEach { sourceCompatibility = '1.%d'; targetCompatibility = '1.%d' }\n" % (args.bytecode_target, args.bytecode_target))
    wrapper = [str(fixture / 'gradlew.bat')] if os.name == 'nt' else ['bash', str(fixture / 'gradlew')]
    command = wrapper + ['--daemon', '--console=plain',
        '-PretrospectumLoomCheckout=' + str(loom),
        '-Pmixin_bootstrap=' + str(runtime),
        *['-P' + prop for prop in args.gradle_property],
        'build', 'verifyForgeBinpatches', 'genIdeaRuns', 'genEclipseRuns', 'vscode', 'exportDevelopmentLaunches',
        'generatePomFileForModPublication', 'generateMetadataFileForModPublication']
    with (output / 'build.log').open('w') as log:
        subprocess.run(command, cwd=fixture, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=900)
    ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
    pom = ET.parse(fixture / 'build/publications/mod/pom-default.xml')
    deps = pom.findall('m:dependencies/m:dependency', ns)
    scopes = {d.findtext('m:artifactId', namespaces=ns): d.findtext('m:scope', namespaces=ns) for d in deps}
    assert scopes == {'commons-io': 'compile', 'commons-lang3': 'runtime', 'gson': 'runtime'}, scopes
    assert pom.find('.//m:exclusion/m:groupId', ns).text == 'test.excluded'
    metadata = json.loads((fixture / 'build/publications/mod/module.json').read_text())
    for variant in metadata['variants']:
        assert variant['attributes']['org.gradle.jvm.version'] == 7, variant
        for file in variant.get('files', []):
            assert '-dev.jar' not in file['name'], file
    launches = json.loads((fixture / 'build/retromixin/launches.json').read_text())
    dev = launches['server']
    if args.remapper_probe:
        classpath = [Path(p) for p in dev['classpath'].split(os.pathsep)]
        def dependency(name):
            return next(p for p in classpath if p.name == name)
        with (output / 'remapper-failure.log').open('w') as log:
            subprocess.run([os.sys.executable, str(args.remapper_probe.resolve()),
                '--runtime', str(runtime),
                '--launchwrapper', str(dependency('launchwrapper-1.8.jar')),
                '--asm', str(dependency('asm-all-4.1.jar')),
                '--jopt', str(dependency('jopt-simple-4.5.jar')),
                '--java', dev['executable']], stdout=log, stderr=subprocess.STDOUT, check=True, timeout=120)
    assert dev['args'].count('--tweakClass=dev.desiderium.retro.mixin.bootstrap.RetroMixinTweaker') == 1
    for name in ('client', 'server'):
        idea = fixture / f'.idea/runConfigurations/{name}.xml'
        assert ET.parse(idea).find('.//configuration').get('type') == 'GradleRunConfiguration'
    for name in ('client', 'server'):
        xml = (fixture / f'{name}.launch').read_text()
        assert 'RetroMixinTweaker' in xml and 'retromixin.mixin.configs' in xml
    vscode = json.loads((fixture / '.vscode/launch.json').read_text())
    for config in vscode['configurations']:
        assert 'RetroMixinTweaker' in config['args']
    # Regeneration must preserve an existing user-edited IDEA configuration.
    idea = fixture / '.idea/runConfigurations/server.xml'
    original = idea.read_bytes() + b'\n<!-- user edit -->\n'
    idea.write_bytes(original)
    with (output / 'regenerate.log').open('w') as log:
        subprocess.run(command[:command.index('build')] + ['remapJarForRelease', 'genIdeaRuns'], cwd=fixture,
                       stdout=log, stderr=subprocess.STDOUT, check=True, timeout=900)
    assert idea.read_bytes() == original
    unchanged = (output / 'regenerate.log').read_text(encoding='utf-8', errors='replace')
    assert '> Task :exportMixinMappings UP-TO-DATE' in unchanged, unchanged[-4000:]
    assert '> Task :exportReleaseMappings UP-TO-DATE' in unchanged, unchanged[-4000:]
    assert '> Task :remapJarForRelease UP-TO-DATE' in unchanged, unchanged[-4000:]
    extra_mapping = fixture / 'verification-extra.srg'
    changed_record = 'FD: example/probe/BranchProbe/value example/probe/BranchProbe/renamedValue\n'
    extra_mapping.write_text(changed_record, encoding='utf-8')
    remap_command = command[:command.index('build')] + ['remapJarForRelease']
    with (output / 'mapping-change.log').open('w', encoding='utf-8') as log:
        subprocess.run(remap_command, cwd=fixture, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=900)
    changed = (output / 'mapping-change.log').read_text(encoding='utf-8', errors='replace')
    assert '> Task :remapJarForRelease UP-TO-DATE' not in changed
    archive, = [p for p in (fixture / 'build/libs').glob('*.jar') if not p.name.endswith('-dev.jar')]
    with zipfile.ZipFile(archive) as jar:
        assert b'renamedValue' in jar.read('example/probe/BranchProbe.class')
    previous = hashlib.sha256(archive.read_bytes()).hexdigest()
    extra_mapping.write_text('FD: example/probe/BranchProbe/value unrelated/Owner/value\n', encoding='utf-8')
    with (output / 'mapping-failure.log').open('w', encoding='utf-8') as log:
        failed = subprocess.run(remap_command, cwd=fixture, stdout=log, stderr=subprocess.STDOUT, timeout=900)
    assert failed.returncode != 0
    assert 'Conflicting Mixin owner' in (output / 'mapping-failure.log').read_text(encoding='utf-8', errors='replace')
    assert hashlib.sha256(archive.read_bytes()).hexdigest() == previous
    extra_mapping.write_text(changed_record, encoding='utf-8')
    with (output / 'mapping-recovery.log').open('w', encoding='utf-8') as log:
        subprocess.run(remap_command, cwd=fixture, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=900)
    assert hashlib.sha256(archive.read_bytes()).hexdigest() == previous

    # Check the actual selected input, not merely the normal development archive.
    bad_input = fixture / 'invalid-input.jar'
    with zipfile.ZipFile(bad_input, 'w') as jar:
        jar.writestr('Unsupported.class', bytes.fromhex('cafebabe00000034'))
    with (output / 'custom-input-failure.log').open('w', encoding='utf-8') as log:
        failed = subprocess.run(remap_command + ['-Pverification.invalidInput=true'], cwd=fixture,
            stdout=log, stderr=subprocess.STDOUT, timeout=900)
    assert failed.returncode != 0
    assert 'major=52' in (output / 'custom-input-failure.log').read_text(encoding='utf-8', errors='replace')
    assert hashlib.sha256(archive.read_bytes()).hexdigest() == previous
    with (output / 'publish-fixture.log').open('w', encoding='utf-8') as log:
        subprocess.run(command[:command.index('build')] + ['publishModPublicationToVerificationRepository'],
            cwd=fixture, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=900)
    consumer = output / 'consumer'
    (consumer / 'src/main/java').mkdir(parents=True)
    (consumer / 'settings.gradle').write_text("rootProject.name = 'consumer'\n", encoding='utf-8')
    if (fixture / 'gradle.local.properties').is_file():
        # Pass machine-local toolchain paths to this standalone project as Gradle properties.
        shutil.copy2(fixture / 'gradle.local.properties', consumer / 'gradle.properties')
    api_home = Path(dev['executable']).parent.parent
    api = api_home / 'jre/lib/rt.jar'
    if not api.is_file(): api = api_home / 'lib/rt.jar'
    assert api.is_file(), api
    (consumer / 'build.gradle').write_text("""plugins { id 'java'; id 'application' }
repositories { maven { url = uri(%s) }; mavenCentral() }
dependencies { implementation 'example:example-mod:1.0.0' }
java { toolchain.languageVersion = JavaLanguageVersion.of(8) }
tasks.withType(JavaCompile).configureEach {
    sourceCompatibility = '1.7'; targetCompatibility = '1.7'
    options.bootstrapClasspath = files(%s)
}
application.mainClass = 'Consumer'
tasks.named('run', JavaExec) { javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(7) } }
if (providers.gradleProperty('verification.consumerJvm6').isPresent()) {
    configurations.compileClasspath.attributes.attribute(org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 6)
}
""" % (json.dumps((fixture / 'build/verification-maven').as_uri()), json.dumps(api.as_posix())), encoding='utf-8')
    (consumer / 'src/main/java/Consumer.java').write_text('''public final class Consumer {
    public static void main(String[] args) {
        if (!"examplemod".equals(example.BuildConstants.MOD_ID)) throw new AssertionError("Wrong published mod");
        System.out.println("PUBLISHED MOD CONSUMED " + example.BuildConstants.class.getName());
    }
}
''', encoding='utf-8')
    consumer_command = command[:command.index('build')] + ['-p', str(consumer)]
    with (output / 'consumer.log').open('w', encoding='utf-8') as log:
        subprocess.run(consumer_command + ['run'], cwd=consumer, stdout=log,
            stderr=subprocess.STDOUT, check=True, timeout=900)
    assert 'PUBLISHED MOD CONSUMED' in (output / 'consumer.log').read_text(encoding='utf-8', errors='replace')
    with (output / 'consumer-java6-rejected.log').open('w', encoding='utf-8') as log:
        rejected = subprocess.run(consumer_command + ['compileJava', '-Pverification.consumerJvm6=true'],
            cwd=consumer, stdout=log, stderr=subprocess.STDOUT, timeout=900)
    assert rejected.returncode != 0
    rejection = (output / 'consumer-java6-rejected.log').read_text(encoding='utf-8', errors='replace')
    assert 'example:example-mod:1.0.0' in rejection and 'only compatible with JVM runtime version 7 or newer' in rejection, rejection[-4000:]

    # Original Forge server, with only the release mod and shared runtime added.
    production = output / 'production'
    (production / 'mods').mkdir(parents=True)
    release, = [p for p in (fixture / 'build/libs').glob('*.jar') if not p.name.endswith('-dev.jar')]
    attributes = manifest_attributes(release)
    assert attributes.get('MixinConfigs') == 'examplemod.mixins.json', attributes
    assert not any(key.startswith('RetroMixin') for key in attributes), attributes
    shutil.copy2(release, production / 'mods' / release.name)
    shutil.copy2(runtime, production / 'mods' / runtime.name)
    cache = Path(os.environ.get('GRADLE_USER_HOME', Path.home() / '.gradle')) / 'caches'
    forge, = list((cache / 'modules-2/files-2.1/net.minecraftforge/forge/1.6.4-9.11.1.1345').rglob('*universal.jar'))
    assert hashlib.sha256(forge.read_bytes()).hexdigest() == '7acfb321c4b1fd35253b73dd2715f47cf3c38ee3ec4586d762c217114697df01'
    shutil.copy2(forge, production / 'forge.jar')
    # Loom already downloaded the official Mojang version descriptor and server.
    version_file, = list((cache / 'retrospectum-loom').glob('minecraft-1.6.4-info-*.json'))
    server_info = json.loads(version_file.read_text())['downloads']['server']
    server_file = production / 'minecraft_server.1.6.4.jar'
    with urllib.request.urlopen(server_info['url'], timeout=60) as response:
        server_file.write_bytes(response.read())
    assert hashlib.sha1(server_file.read_bytes()).hexdigest() == server_info['sha1']
    for entry in manifest_attributes(forge)['Class-Path'].split():
        if not entry.startswith('libraries/'):
            continue
        relative = Path(entry.removeprefix('libraries/'))
        candidates = list((cache / 'modules-2/files-2.1').rglob(relative.name))
        target = production / entry
        target.parent.mkdir(parents=True, exist_ok=True)
        if candidates:
            shutil.copy2(candidates[0], target)
        else:
            for host in ('https://libraries.minecraft.net/', 'https://repo.maven.apache.org/maven2/', 'https://maven.minecraftforge.net/'):
                try:
                    with urllib.request.urlopen(host + relative.as_posix(), timeout=30) as response:
                        target.write_bytes(response.read())
                    break
                except urllib.error.HTTPError:
                    continue
            else:
                raise RuntimeError('Cannot download original Forge library: ' + str(relative))
    with zipfile.ZipFile(release) as archive:
        majors = {struct.unpack('>H', archive.read(name)[6:8])[0]
                  for name in archive.namelist() if name.endswith('.class')}
    assert majors == {args.bytecode_target + 44}, majors
    java_runs = []
    java_paths = args.game_java or [Path(dev['executable'])]
    seen = set()
    for executable in java_paths:
        executable = executable.resolve()
        if os.name == 'nt' and executable.suffix.lower() != '.exe':
            executable = executable.with_suffix('.exe')
        version = subprocess.run([str(executable), '-version'], capture_output=True, check=True, timeout=15)
        version_text = (version.stdout + version.stderr).decode('utf-8', errors='replace')
        match = re.search(r'version "1\.([78])\.', version_text)
        if not match:
            raise RuntimeError('Expected a Java 7/8 game runtime: ' + version_text)
        major = int(match.group(1))
        if major in seen:
            raise RuntimeError('Duplicate Java version in runtime matrix: ' + str(major))
        seen.add(major)
        development_run = output / ('development-java' + str(major))
        production_run = output / ('production-java' + str(major))
        shutil.copytree(Path(dev['directory']), development_run)
        shutil.copytree(production, production_run)
        run_server([str(executable), '-Xverify:all', '-Dfile.encoding=UTF-8', *dev['jvmArgs'], '-cp', dev['classpath'],
                    dev['mainClass'], *dev['args']], development_run, output / ('development-java%d.log' % major))
        run_server([str(executable), '-Xverify:all', '-Dfile.encoding=UTF-8', '-Xmx1G', '-jar', 'forge.jar', 'nogui'],
                   production_run, output / ('production-java%d.log' % major))
        from verify_stackmaps import verify
        asm = next(Path(p) for p in dev['classpath'].split(os.pathsep) if Path(p).name == 'asm-all-4.1.jar')
        with (output / ('stackmaps-java%d.log' % major)).open('w', encoding='utf-8') as log:
            verify(release, asm, executable, args.bytecode_target, log)
        java_runs.append({'major': major, 'version': version_text.strip(), 'development': 'passed',
                          'production': 'passed', 'controlledFrameStrip': 'expected result'})
    # A fresh producer JAR must be built before its SRG members are remapped for compilation.
    multi = output / 'multi-project'
    shutil.copytree(fixture, multi, ignore=shutil.ignore_patterns('build', '.gradle', 'run', '.idea', '.vscode'))
    with (multi / 'settings.gradle').open('a', encoding='utf-8') as script:
        script.write("\ninclude 'dependency-probe'\n")
    with (multi / 'build.gradle').open('a', encoding='utf-8') as script:
        script.write("\ndependencies { modImplementation project(':dependency-probe') }\n")
    producer = multi / 'dependency-probe'
    (producer / 'src/main/java/probe').mkdir(parents=True)
    (producer / 'build.gradle').write_text("""plugins { id 'java-library' }
java { toolchain.languageVersion = JavaLanguageVersion.of(8) }
tasks.withType(JavaCompile).configureEach { sourceCompatibility = '1.7'; targetCompatibility = '1.7' }
""", encoding='utf-8')
    producer_source = producer / 'src/main/java/probe/Dependency.java'
    producer_source.write_text('package probe; public final class Dependency { public static int field_72986_A = 1; }\n', encoding='utf-8')
    (multi / 'src/main/java/example/ProjectDependencyProbe.java').write_text(
        'package example; public final class ProjectDependencyProbe { public int read() { return probe.Dependency.worldInfo; } }\n', encoding='utf-8')
    multi_command = command[:command.index('build')] + ['-p', str(multi), 'build']
    def multi_build(name):
        with (output / name).open('w', encoding='utf-8') as log:
            subprocess.run(multi_command, cwd=multi, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=900)
        return (output / name).read_text(encoding='utf-8', errors='replace')
    first = multi_build('multi-project-first.log')
    assert first.index('> Task :dependency-probe:jar') < first.index('> Task :remapModImplementationDependencies') < first.index('> Task :compileJava'), first[-5000:]
    mapped, = (multi / 'build/retromixin/dependencies/modImplementation').glob('*dependency-probe*.jar')
    with zipfile.ZipFile(mapped) as jar:
        assert b'worldInfo' in jar.read('probe/Dependency.class')
    old_mapped = hashlib.sha256(mapped.read_bytes()).hexdigest()
    unchanged = multi_build('multi-project-unchanged.log')
    assert '> Task :remapModImplementationDependencies UP-TO-DATE' in unchanged
    producer_source.write_text(producer_source.read_text().replace(' = 1;', ' = 2;'), encoding='utf-8')
    changed = multi_build('multi-project-changed.log')
    assert '> Task :remapModImplementationDependencies UP-TO-DATE' not in changed
    assert hashlib.sha256(mapped.read_bytes()).hexdigest() != old_mapped

    # Also consume another Loom project's SRG release variant, including a real game method reference.
    (producer / 'build.gradle').write_text("""plugins { id 'dev.desiderium.retro.loom' }
dependencies {
    minecraft 'com.mojang:minecraft:1.6.4'
    forge 'net.minecraftforge:forge:1.6.4-9.11.1.1345:universal'
    mappings 'net.minecraftforge:forge:1.6.4-9.11.1.1345:src@zip'
}
""", encoding='utf-8')
    producer_source.write_text('package probe; public final class Dependency { public static long time(net.minecraft.world.World world) { return world.getWorldTime(); } }\n', encoding='utf-8')
    (multi / 'src/main/java/example/ProjectDependencyProbe.java').write_text(
        'package example; public final class ProjectDependencyProbe { public long read(net.minecraft.world.World world) { return probe.Dependency.time(world); } }\n', encoding='utf-8')
    loom_build = multi_build('multi-project-loom.log')
    assert loom_build.index('> Task :dependency-probe:remapJarForRelease') < loom_build.index('> Task :remapModImplementationDependencies') < loom_build.index('> Task :compileJava'), loom_build[-5000:]
    producer_release = producer / 'build/libs/dependency-probe-1.0.0.jar'
    with zipfile.ZipFile(producer_release) as jar:
        assert b'func_72820_D' in jar.read('probe/Dependency.class')
    with zipfile.ZipFile(mapped) as jar:
        assert b'getWorldTime' in jar.read('probe/Dependency.class')

    (output / 'result.json').write_text(json.dumps({'development': 'passed', 'production': 'passed', 'projectDependency': 'passed', 'loomProjectDependency': 'passed',
        'publication': 'passed', 'ideGeneration': 'passed', 'incrementalRemapping': 'passed', 'publishedConsumer': 'passed', 'minimumJvmMetadata': 7, 'modClassMajor': args.bytecode_target + 44,
        'validationCeilingPatchedInDisposableCheckout': args.bytecode_target == 7,
        'gameRuns': java_runs}, indent=2) + '\n', encoding='utf-8')
    print(f'Toolchain verification passed: {output}')


if __name__ == '__main__':
    main()
