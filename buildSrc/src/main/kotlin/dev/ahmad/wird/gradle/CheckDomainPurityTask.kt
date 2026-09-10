package dev.ahmad.wird.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails the build when the domain layer reaches outward.
 *
 * Wired to every Kotlin compilation, so both `assembleDebug` and
 * `wasmJsBrowserDistribution` fail on a violation rather than only `check`.
 */
abstract class CheckDomainPurityTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val domainSources: ConfigurableFileCollection

    @get:Input
    abstract val allowedImportPrefixes: ListProperty<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val allowed = allowedImportPrefixes.get()
        val kotlinFiles = domainSources.files.filter { it.isFile && it.extension == "kt" }.sorted()

        val violations = kotlinFiles.flatMap { file ->
            DomainPurity.check(file.readText(), allowed).map { violation ->
                "${file.invariantSeparatorsPath}:${violation.line}  $violation"
            }
        }

        val output = report.get().asFile
        output.parentFile.mkdirs()

        if (violations.isEmpty()) {
            output.writeText("domain purity OK: ${kotlinFiles.size} file(s) checked\n")
            logger.lifecycle("domain purity OK")
            return
        }

        output.writeText(violations.joinToString("\n", postfix = "\n"))
        throw GradleException(
            buildString {
                appendLine(
                    "Layering violation: domain/ must not depend on data/, ui/, di/, " +
                        "Compose, Room, Koin, Supabase or Android.",
                )
                appendLine("Allowed import prefixes: ${allowed.joinToString(", ")}")
                appendLine()
                violations.forEach { appendLine("  $it") }
            },
        )
    }
}
