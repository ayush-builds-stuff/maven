/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.internal.impl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Packaging;
import org.apache.maven.api.Type;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.SourceQueries;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

/**
 * Transitional adapter from the v3 project builder output to an immutable v4 project.
 */
public final class LegacyProjectAdapter {

    private LegacyProjectAdapter() {}

    public static DefaultProject snapshot(InternalMavenSession session, MavenProject project) {
        String groupId = project.getGroupId();
        String artifactId = project.getArtifactId();
        String version = project.getVersion();
        Packaging packaging = packaging(session, project);
        boolean producesMainArtifact = packaging != null
                && packaging.producesMainArtifact()
                && !SourceQueries.usesModuleSourceHierarchy(project);
        ArrayList<org.eclipse.aether.artifact.Artifact> artifacts = new ArrayList<>(2);
        if (hasCoordinates(groupId, artifactId, version)) {
            org.eclipse.aether.artifact.Artifact pomArtifact = RepositoryUtils.toArtifact(new ProjectArtifact(project));
            artifacts.add(pomArtifact);
            if (producesMainArtifact) {
                Type type = packaging.type();
                org.eclipse.aether.artifact.Artifact projectArtifact = project.getArtifact() != null
                        ? RepositoryUtils.toArtifact(project.getArtifact())
                        : new org.eclipse.aether.artifact.DefaultArtifact(
                                groupId,
                                artifactId,
                                type.classifier() != null ? type.classifier() : "",
                                type.extension(),
                                version);
                if (!ArtifactIdUtils.equalsVersionlessId(pomArtifact, projectArtifact)) {
                    artifacts.add(projectArtifact);
                }
            }
        }
        DefaultProject.State state = new DefaultProject.State(
                groupId,
                artifactId,
                version,
                packaging,
                artifacts,
                producesMainArtifact,
                project.getModel().getDelegate(),
                project.getFile() != null ? project.getFile().toPath() : null,
                project.getBasedir() != null ? project.getBasedir().toPath() : null,
                rootDirectory(project),
                session.getProject(project.getParent()),
                Stream.iterate(project, Objects::nonNull, MavenProject::getParent)
                        .flatMap(current -> current.getModel().getDelegate().getProfiles().stream())
                        .toList(),
                project.getActiveProfiles().stream()
                        .map(org.apache.maven.model.Profile::getDelegate)
                        .toList(),
                Stream.iterate(project, Objects::nonNull, MavenProject::getParent)
                        .flatMap(current -> current.getActiveProfiles().stream())
                        .map(org.apache.maven.model.Profile::getDelegate)
                        .toList());
        return new DefaultProject(session, state);
    }

    private static Packaging packaging(InternalMavenSession session, MavenProject project) {
        ClassLoader threadContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(project.getClassRealm());
            return session.requirePackaging(project.getPackaging());
        } finally {
            Thread.currentThread().setContextClassLoader(threadContextClassLoader);
        }
    }

    private static Path rootDirectory(MavenProject project) {
        try {
            return project.getRootDirectory();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private static boolean hasCoordinates(String groupId, String artifactId, String version) {
        return groupId != null
                && !groupId.isEmpty()
                && artifactId != null
                && !artifactId.isEmpty()
                && version != null
                && !version.isEmpty();
    }
}
