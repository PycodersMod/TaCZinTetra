import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile
import java.nio.charset.StandardCharsets
import java.util.Base64

plugins {
    java
    id("net.minecraftforge.gradle") version "6.0.54"
}

fun prop(name: String): String = providers.gradleProperty(name).get()

val minecraftVersion = prop("minecraft_version")
val minecraftVersionRange = prop("minecraft_version_range")
val forgeVersion = prop("forge_version")
val forgeVersionRange = prop("forge_version_range")
val javaVersionRange = prop("java_version_range")
val modId = prop("mod_id")
val modName = prop("mod_name")
val modVersion = prop("mod_version")
val modGroupId = prop("mod_group_id")
val modAuthors = prop("mod_authors")
val modDescription = prop("mod_description")
val modLicense = prop("mod_license")

val refMapRemappingFile = file("build/createSrgToMcp/output.srg")
val titRunDir = file(
    providers.gradleProperty("pycodersRuntimeDir")
        .orElse("../../runtime/legacy-import/TaCZinTetra/run")
        .get()
)
val titLegacyModsDir = file("../../runtime/legacy-import/TaCZinTetra/run/mods")
fun decodeArgs(name: String): List<String> = providers.gradleProperty(name).orNull?.takeIf { it.isNotEmpty() }?.split('.')?.map { if (it == "_") "" else String(Base64.getDecoder().decode(it), StandardCharsets.UTF_8) } ?: emptyList()
val pycodersGameArgs = decodeArgs("pycodersGameArgsB64")
val pycodersJavaArgs = decodeArgs("pycodersJavaArgsB64")
val pycodersUsername = providers.gradleProperty("titUsername").orNull
    ?.takeIf { it.isNotBlank() }
    ?: providers.gradleProperty("pycodersUsername").orElse("Dev").get()

group = modGroupId
version = modVersion

base {
    archivesName.set(modId)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net")
    maven("https://maven.blamejared.com")
    flatDir {
        dirs(titRunDir.resolve("mods"), titLegacyModsDir)
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")
    implementation(fg.deobf("mutil:mutil:1.20.1-6.3.0"))
    implementation(fg.deobf("tetra:tetra:1.20.1-6.17.0"))
    implementation(fg.deobf("tacz:tacz:1.20.1-1.1.8-hotfix"))
    // Optional target-pack integrations are development-only. ForgeGradle must
    // remap them through the same official -> MCP pipeline as the core mods;
    // manual constant replacement is not sufficient for runtime bridge methods.
    runtimeOnly(fg.deobf("guideme:guideme:20.1.7"))
    runtimeOnly(fg.deobf("sophisticatedcore:sophisticatedcore:1.20.1-1.3.21.1676"))
    runtimeOnly(fg.deobf("sophisticatedbackpacks:sophisticatedbackpacks:1.20.1-3.24.35.1675"))
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    compileOnly("mezz.jei:jei-1.20.1-common-api:15.20.0.106")
    compileOnly("mezz.jei:jei-1.20.1-forge-api:15.20.0.106")
    // Development-only mapped runtime; JEI remains an optional integration in the published mod.
    runtimeOnly(fg.deobf("mezz.jei:jei-1.20.1-forge:15.20.0.106"))
}

minecraft {
    mappings("official", minecraftVersion)

    runs {
        create("client") {
            workingDirectory(titRunDir)
            pycodersJavaArgs.forEach { jvmArg(it) }
            pycodersGameArgs.forEach { args(it) }
            arg("-mixin.config=taczintetra.mixins.json")
            providers.gradleProperty("titClientArgs").orNull
                ?.takeIf { it.isNotBlank() }
                ?.let { args(it.trim().split(Regex("\\s+"))) }
            providers.gradleProperty("titQuickPlayWorld").orNull
                ?.takeIf { it.isNotBlank() }
                ?.let { args("--quickPlaySingleplayer", it.trim()) }
            args("--username", pycodersUsername)
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", refMapRemappingFile.absolutePath)
            if (providers.gradleProperty("titAutomation").orNull == "true") {
                property("taczintetra.dev_automation", "true")
                providers.gradleProperty("titAutomationToken").orNull
                    ?.takeIf { it.isNotBlank() }
                    ?.let { property("taczintetra.dev_automation_token", it) }
                if (providers.gradleProperty("titLanHost").orNull == "true") {
                    property("taczintetra.dev_lan_host", "true")
                }
                if (providers.gradleProperty("titHoldHolo").orNull == "true") {
                    property("taczintetra.dev_hold_holo", "true")
                }
            }
            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("server") {
            workingDirectory(titRunDir)
            pycodersJavaArgs.forEach { jvmArg(it) }
            pycodersGameArgs.forEach { args(it) }
            arg("nogui")
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", refMapRemappingFile.absolutePath)
            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("data") {
            workingDirectory(titRunDir)
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", refMapRemappingFile.absolutePath)
            args(
                "--mod", modId,
                "--all",
                "--output", file("src/generated/resources/"),
                "--existing", file("src/main/resources/")
            )
        }
    }
}

val prepareDevMods = tasks.register<Copy>("prepareDevMods") {
    from(configurations.runtimeClasspath)
    into(titRunDir.resolve("mods"))
    include("*mutil*1.20.1-6.3.0_mapped_official_1.20.1.jar")
    include("*tetra*1.20.1-6.17.0_mapped_official_1.20.1.jar")
    include("*tacz*1.20.1-1.1.8-hotfix_mapped_official_1.20.1.jar")
    include("*jei*15.20.0.106_mapped_official_1.20.1.jar")
    include("*guideme*20.1.7_mapped_official_1.20.1.jar")
    include("*sophisticatedcore*1.20.1-1.3.21.1676_mapped_official_1.20.1.jar")
    include("*sophisticatedbackpacks*1.20.1-3.24.35.1675_mapped_official_1.20.1.jar")
    rename { name -> name.replace("_mapped_official_1.20.1", "") }
}

tasks.configureEach {
    if (name in setOf("runClient", "runServer", "runData")) {
        dependsOn(prepareDevMods)
    }
}

sourceSets.main.get().resources.srcDir("src/generated/resources")

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
    options.compilerArgs.add("-AreobfSrgFile=${refMapRemappingFile.absolutePath}")
    options.compilerArgs.add("-AoutRefMapFile=${file("build/resources/main/taczintetra.refmap.json").absolutePath}")
}

tasks.test {
    useJUnitPlatform()
}

val resourceProperties = mapOf(
    "mod_id" to modId,
    "mod_name" to modName,
    "mod_version" to modVersion,
    "mod_authors" to modAuthors,
    "mod_description" to modDescription,
    "mod_license" to modLicense,
    "minecraft_version" to minecraftVersion,
    "minecraft_version_range" to minecraftVersionRange,
    "forge_version" to forgeVersion,
    "forge_version_range" to forgeVersionRange,
    "java_version_range" to javaVersionRange
)

tasks.named<Copy>("processResources") {
    inputs.properties(resourceProperties)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
        expand(resourceProperties)
    }
}

tasks.jar {
    manifest {
        attributes["MixinConfigs"] = "taczintetra.mixins.json"
    }
}

