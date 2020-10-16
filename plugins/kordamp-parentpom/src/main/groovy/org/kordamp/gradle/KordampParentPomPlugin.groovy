/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2020 Andres Almiray.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.kordamp.gradle.plugin.base.ProjectConfigurationExtension
import org.kordamp.gradle.plugin.bintray.BintrayPlugin
import org.kordamp.gradle.plugin.project.java.JavaProjectPlugin

/**
 * @author Andres Almiray
 */
class KordampParentPomPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.plugins.apply(JavaProjectPlugin)
        project.plugins.apply(BintrayPlugin)

        if (!project.hasProperty('bintrayUsername'))  project.ext.bintrayUsername  = '**undefined**'
        if (!project.hasProperty('bintrayApiKey'))    project.ext.bintrayApiKey    = '**undefined**'
        if (!project.hasProperty('sonatypeUsername')) project.ext.sonatypeUsername = '**undefined**'
        if (!project.hasProperty('sonatypePassword')) project.ext.sonatypePassword = '**undefined**'

        project.extensions.findByType(ProjectConfigurationExtension).with {
            release = (project.rootProject.findProperty('release') ?: false).toBoolean()

            info {
                vendor = 'Kordamp'

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
                        url   = 'http://andresalmiray.com/'
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

            bintray {
                enabled = true
                credentials {
                    username = project.bintrayUsername
                    password = project.bintrayApiKey
                }
                userOrg = 'kordamp'
                repo    = 'maven'
                name    = project.rootProject.name
                publish = (project.rootProject.findProperty('release') ?: false).toBoolean()
            }

            publishing {
                releasesRepository  = 'localRelease'
                snapshotsRepository = 'localSnapshot'
            }
        }

        project.allprojects {
            repositories {
                jcenter()
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
        }
    }
}
