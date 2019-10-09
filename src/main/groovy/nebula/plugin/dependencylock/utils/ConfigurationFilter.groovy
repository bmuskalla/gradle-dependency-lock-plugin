/**
 *
 *  Copyright 2019 Netflix, Inc.
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

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer


class ConfigurationFilter {
    static Collection<Configuration> findAllConfigurationsThatMatchSuffixes(ConfigurationContainer configurations, Collection<String> suffixesToMatch) {
        configurations
                .stream()
                .filter { conf ->
                    configurationMatchesSuffixes(conf, suffixesToMatch)
                }
                .collect()
    }

    static boolean configurationMatchesSuffixes(Configuration configuration, Collection<String> suffixesToMatch) {
        return configuration.name.toLowerCase().endsWithAny(*suffixesToMatch.collect { it.toLowerCase() })
    }
}
