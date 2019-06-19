/**
 *
 *  Copyright 2018 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package nebula.plugin.dependencylock.utils

import nebula.plugin.dependencylock.ConfigurationsToLockFinder
import nebula.plugin.dependencylock.tasks.GenerateLockTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class CoreLockingHelper {
    private Project project

    private Boolean shouldLockAllConfigurations

    private static final Logger LOGGER = Logging.getLogger(CoreLockingHelper)


    CoreLockingHelper(Project project) {
        this.project = project
        shouldLockAllConfigurations = project.hasProperty("lockAllConfigurations") && (project.property("lockAllConfigurations") as String).toBoolean()
    }

    void lockSelectedConfigurations(Set<String> configurationNames) {
        if (shouldLockAllConfigurations) {
            project.dependencyLocking {
                it.lockAllConfigurations()
            }
        } else {
            def closureToLockConfigurations = {
                it.resolutionStrategy.activateDependencyLocking()
            }
            runClosureWhenPluginsAreSeen(configurationNames, closureToLockConfigurations)
        }
    }

    void migrateLockedConfigurations(Set<String> configurationNames, Closure closure) {
        runClosureWhenPluginsAreSeen(configurationNames, closure)
    }

    void migrateUnlockedDependenciesClosure(Set<String> configurationNames, Closure closure) {
        runClosureWhenPluginsAreSeen(configurationNames, closure)
    }

    private void runClosureWhenPluginsAreSeen(Set<String> configurationNames, Closure closure) {
        runClosureOnConfigurations(configurationNames, closure, new ArrayList<String>())

        project.plugins.withId("nebula.facet") { // FIXME: not working currently
            runClosureOnConfigurations(configurationNames, closure, new ArrayList<String>())
        }
        project.plugins.withId("nebula.integtest") {
            runClosureOnConfigurations(configurationNames, closure, new ArrayList<String>())
        }
        if (project.plugins.hasPlugin("scala")) {
            // the configurations `incrementalScalaAnalysisFor_x_ extend from `compile` and `implementation` rather than `compileClasspath`
            def scalaConfigurationsToLock = []
            project.configurations
                    .findAll { it.name == 'compile' }
                    .each { it.isCanBeResolved() }
                    .each {
                        scalaConfigurationsToLock.add(it.name)
                    }

            // we cannot resolve the 'implementation' configuration to determine if there are dependencies on here. Providing warning instead:
            LOGGER.warn("Locking warning: Cannot lock scala configurations based on the 'implementation' configuration. Please define dependencies on the 'compile' configuration, if needed")

            runClosureOnConfigurations(configurationNames, closure, scalaConfigurationsToLock)
        }
    }

    private void runClosureOnConfigurations(Set<String> configurationNames, Closure closure, List<String> additionalBaseConfigurationsToLock) {
        Set<Configuration> configurationsToLock
        if (shouldLockAllConfigurations) {
            configurationsToLock = GenerateLockTask.lockableConfigurations(project, project, configurationNames)
        } else {
            configurationsToLock = findConfigurationsToLock(configurationNames, additionalBaseConfigurationsToLock)
        }

        configurationsToLock.each {
            closure(it)
        }
    }

    private Set<Configuration> findConfigurationsToLock(Set<String> configurationNames, List<String> additionalBaseConfigurationsToLock) {
        def lockableConfigurationNames = new ConfigurationsToLockFinder(project).findConfigurationsToLock(configurationNames, additionalBaseConfigurationsToLock)

        def lockableConfigurations = new HashSet()
        project.configurations.each {
            if (lockableConfigurationNames.contains(it.name)) {
                lockableConfigurations.add(it)
            }
        }
        return lockableConfigurations
    }
}