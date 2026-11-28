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
package org.apache.maven.impl;

import java.util.List;
import java.util.Properties;

import org.apache.maven.api.Project;
import org.apache.maven.api.SourceRoot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class MutableProjectTest {

    @Test
    void initializesProjectOnce() {
        MutableProject<String> mutableProject = new MutableProject<>();
        Project project = mock(Project.class);

        assertNull(mutableProject.projectOrNull());
        assertThrows(NullPointerException.class, mutableProject::project);

        mutableProject.initialize(project);
        mutableProject.initialize(project);

        assertSame(project, mutableProject.project());
        assertThrows(IllegalStateException.class, () -> mutableProject.initialize(mock(Project.class)));
    }

    @Test
    void rejectsNullProject() {
        MutableProject<String> project = new MutableProject<>();
        assertThrows(NullPointerException.class, () -> project.initialize(null));
    }

    @Test
    void replacesOnlyTheExpectedProject() {
        MutableProject<String> mutableProject = new MutableProject<>();
        Project original = mock(Project.class);
        Project replacement = mock(Project.class);
        mutableProject.initialize(original);

        mutableProject.replace(original, replacement);

        assertSame(replacement, mutableProject.project());
        assertThrows(IllegalStateException.class, () -> mutableProject.replace(original, mock(Project.class)));
    }

    @Test
    void ownsBuildTimeState() {
        MutableProject<String> project = new MutableProject<>();
        SourceRoot sourceRoot = mock(SourceRoot.class);
        SourceRoot replacement = mock(SourceRoot.class);
        SourceRoot secondSourceRoot = mock(SourceRoot.class);
        Properties properties = new Properties();
        properties.setProperty("key", "value");

        project.addSourceRoot(sourceRoot);
        project.addSourceRoot(sourceRoot);
        project.addSourceRoot(secondSourceRoot);
        project.replaceSourceRoot(source -> source == sourceRoot, replacement);
        project.addAttachedArtifact("first");
        project.addAttachedArtifact("second");
        project.setProperties(properties);

        assertEquals(
                List.of(replacement, secondSourceRoot),
                project.sourceRoots().stream().toList());
        assertEquals(List.of("first", "second"), project.attachedArtifacts());
        assertSame(properties, project.properties());
    }

    @Test
    void returnsBuildTimeStateSnapshots() {
        MutableProject<String> project = new MutableProject<>();
        SourceRoot original = mock(SourceRoot.class);
        SourceRoot replacement = mock(SourceRoot.class);
        project.setSourceRoots(List.of(original));
        project.setAttachedArtifacts(List.of("original"));

        var sourceRoots = project.sourceRoots();
        var attachedArtifacts = project.attachedArtifacts();
        project.setSourceRoots(List.of(replacement));
        project.setAttachedArtifacts(List.of("replacement"));

        assertEquals(List.of(original), sourceRoots.stream().toList());
        assertEquals(List.of("original"), attachedArtifacts);
        assertEquals(List.of(replacement), project.sourceRoots().stream().toList());
        assertEquals(List.of("replacement"), project.attachedArtifacts());
    }
}
