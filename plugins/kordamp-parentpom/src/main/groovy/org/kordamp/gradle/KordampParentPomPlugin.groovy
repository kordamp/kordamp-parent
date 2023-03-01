/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2023 Andres Almiray.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kordamp.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.kordamp.gradle.plugin.base.ProjectConfigurationExtension
import org.kordamp.gradle.plugin.project.java.JavaProjectPlugin

/**
 * @author Andres Almiray
 */
class KordampParentPomPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.plugins.apply(JavaProjectPlugin)

        if (!project.hasProperty('sonatypeUsername')) project.ext.sonatypeUsername = '**undefined**'
        if (!project.hasProperty('sonatypePassword')) project.ext.sonatypePassword = '**undefined**'
        if (!project.hasProperty('reproducibleBuild')) project.ext.reproducible = 'false'

        boolean reproducibleBuild = (project.rootProject.findProperty('reproducibleBuild') ?: false).toBoolean()
        def rscompat = project.rootProject.findProperty('sourceCompatibility')
        def rtcompat = project.rootProject.findProperty('targetCompatibility')
        String jv = String.valueOf(rtcompat ?: rscompat)
        if (jv.startsWith('1.')) jv = jv[2..-1]

        project.extensions.findByType(ProjectConfigurationExtension).with {
            release = true

            info {
                vendor = 'Kordamp'
                bytecodeVersion = jv

                links {
                    website      = "https://github.com/kordamp/${project.rootProject.name}"
                    issueTracker = "https://github.com/kordamp/${project.rootProject.name}/issues"
                    scm          = "https://github.com/kordamp/${project.rootProject.name}.git"
                }

                scm {
                    url                 = "https://github.com/kordamp/${project.rootProject.name}"
                    connection          = "scm:git:https://github.com/kordamp/${project.rootProject.name}.git"
                    developerConnection = "scm:git:git@github.com:kordamp/${project.rootProject.name}.git"
                }

                people {
                    person {
                        id    = 'aalmiray'
                        name  = 'Andres Almiray'
                        url   = 'https://andresalmiray.com/'
                        roles = ['developer']
                        properties = [
                            twitter: 'aalmiray',
                            github : 'aalmiray'
                        ]
                    }
                }

                credentials {
                    sonatype {
                        username = project.sonatypeUsername
                        password = project.sonatypePassword
                    }
                }

                repositories {
                    repository {
                        name = 'localRelease'
                        url  = "${project.rootProject.buildDir}/repos/local/release"
                    }
                    repository {
                        name = 'localSnapshot'
                        url  = "${project.rootProject.buildDir}/repos/local/snapshot"
                    }
                }
            }

            buildInfo {
                useCommitTimestamp = reproducibleBuild
                skipBuildBy        = reproducibleBuild
                skipBuildJdk       = reproducibleBuild
                skipBuildOs        = reproducibleBuild
            }

            licensing {
                licenses {
                    license {
                        id = 'Apache-2.0'
                    }
                }
            }

            docs {
                javadoc {
                    excludes = ['**/*.html', 'META-INF/**']
                }
                sourceXref {
                    inputEncoding = 'UTF-8'
                }
            }

            publishing {
                releasesRepository  = 'localRelease'
                snapshotsRepository = 'localSnapshot'
            }
        }

        project.allprojects {
            repositories {
                mavenCentral()
            }

            normalization {
                runtimeClasspath {
                    ignore('/META-INF/MANIFEST.MF')
                }
            }

            dependencyUpdates.resolutionStrategy {
                componentSelection { rules ->
                    rules.all { selection ->
                        boolean rejected = ['alpha', 'beta', 'rc', 'cr'].any { qualifier ->
                            selection.candidate.version ==~ /(?i).*[.-]${qualifier}[.\d-]*.*/
                        }
                        if (rejected) {
                            selection.reject('Release candidate')
                        }
                    }
                }
            }
        }

        project.allprojects { Project p ->
            def scompat = project.findProperty('sourceCompatibility')
            def tcompat = project.findProperty('targetCompatibility')

            p.tasks.withType(JavaCompile) { JavaCompile c ->
                if (scompat) c.sourceCompatibility = scompat
                if (tcompat) c.targetCompatibility = tcompat
            }
            p.tasks.withType(GroovyCompile) { GroovyCompile c ->
                if (scompat) c.sourceCompatibility = scompat
                if (tcompat) c.targetCompatibility = tcompat
            }

            if (reproducibleBuild) {
                p.tasks.withType(AbstractArchiveTask).configureEach {
                    preserveFileTimestamps = false
                    reproducibleFileOrder = true
                }
            }
        }
    }
}
