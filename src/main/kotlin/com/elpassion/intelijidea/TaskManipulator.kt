package com.elpassion.intelijidea

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.RunManagerEx
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project

fun injectMainframerBeforeTasks(runManagerEx: RunManagerEx, mfTaskProvider: BeforeRunTaskProvider<*>) {
    runManagerEx.getConfigurations()
            .filterIsInstance<RunConfigurationBase>()
            .filter { it.isCompileBeforeLaunchAddedByDefault }
            .forEach {
                val task = mfTaskProvider.createTask(it)
                if (task != null) {
                    task.isEnabled = true
                    runManagerEx.setBeforeRunTasks(it, listOf<BeforeRunTask<*>>(task), false)
                }
            }
}

fun restoreDefaultBeforeRunTasks(runManager: RunManagerEx, project: Project) {
    runManager.getConfigurations()
            .associate { it to getHardcodedBeforeRunTasks(it, project) }
            .forEach {
                runManager.setBeforeRunTasks(it.key, it.value, false)
            }
}

private fun RunManagerEx.getConfigurations(): Collection<RunConfiguration> = (getExistingConfigurations() + getTemplateConfigurations()).values.map { it.configuration }

private fun getHardcodedBeforeRunTasks(settings: RunConfiguration, project: Project): List<BeforeRunTask<*>> {
    val beforeRunProviders = Extensions.getExtensions(BeforeRunTaskProvider.EXTENSION_POINT_NAME, project)
    return beforeRunProviders.associate { provider -> provider.id to provider.createTask(settings) }
            .filterValues { task -> task != null && task.isEnabled }
            .map {
                settings.factory.configureBeforeRunTaskDefaults(it.key, it.value)
                it.value
            }
            .filterNotNull()
            .filter { it.isEnabled }
}

private fun RunManagerEx.getExistingConfigurations(): Map<String, RunnerAndConfigurationSettings> = getFieldByReflection("myConfigurations")

private fun RunManagerEx.getTemplateConfigurations(): Map<String, RunnerAndConfigurationSettings> = getFieldByReflection("myTemplateConfigurationsMap")

//TODO: remove usage of reflection
private fun <T> Any.getFieldByReflection(fieldName: String): T {
    val declaredField = this.javaClass.getDeclaredField(fieldName)
    declaredField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return declaredField.get(this) as T
}
