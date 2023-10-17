/*
 * Copyright Splunk Inc.
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

package com.splunk.rum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.android.instrumentation.activity.VisibleScreenTracker;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class StartTypeAwareSpanStorageTest {

    private final FileUtils fileUtils = mock();

    private final File rootDir = new File("files/");
    private final VisibleScreenTracker visibleScreenTracker = mock();

    private StartTypeAwareSpanStorage fileProvider;

    @BeforeEach
    void setup() {
        when(fileUtils.getSpansDirectory(rootDir)).thenReturn(new File(rootDir, "spans"));
        fileProvider = StartTypeAwareSpanStorage.create(visibleScreenTracker, fileUtils, rootDir);
    }

    @Test
    void create_onNewId_shouldCleanOldBackgroundFiles() {
        File file = mock();
        when(file.getPath()).thenReturn("files/spans/background/123");
        when(fileUtils.listDirectories(any())).thenReturn(Stream.of(file));
        ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);

        fileProvider = StartTypeAwareSpanStorage.create(visibleScreenTracker, fileUtils, rootDir);

        verify(fileUtils).safeDelete(fileArgumentCaptor.capture());
        assertEquals(file.getPath(), fileArgumentCaptor.getValue().getPath());
    }

    @Test
    void getPendingFiles_givenInBackground_shouldReturnForegoundOnlySpan() {
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn(null);
        when(visibleScreenTracker.getCurrentlyVisibleScreen()).thenReturn("unknown");
        List<File> spans = fileProvider.getPendingFiles().collect(Collectors.toList());
        assertEquals(0, spans.size());
    }

    @Test
    void getPendingFiles_givenPrevouslyInBackground_shouldMoveBackgroundSpanToForegroundSpanForSending() {
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("LauncherActivity");
        when(visibleScreenTracker.getCurrentlyVisibleScreen()).thenReturn("MainActivity");

        List<File> backgroundFiles = new ArrayList<>();
        File fileToMove = mock();
        ArgumentCaptor<File> fileDestinationCaptor = ArgumentCaptor.forClass(File.class);
        when(fileToMove.renameTo(fileDestinationCaptor.capture())).thenReturn(true);
        when(fileToMove.getName()).thenReturn("tosend.span");
        backgroundFiles.add(fileToMove);

        ArgumentCaptor<File> fileSourceCaptor = ArgumentCaptor.forClass(File.class);
        when(fileUtils.listSpanFiles(fileSourceCaptor.capture()))
                .thenReturn(backgroundFiles.stream(), backgroundFiles.stream());

        List<File> spans = fileProvider.getPendingFiles().collect(Collectors.toList());

        verify(fileToMove).renameTo(any());

        String destinationPath = fileDestinationCaptor.getValue().getPath();
        assertEquals("files/spans/tosend.span", destinationPath);

        String sourcePath = fileSourceCaptor.getAllValues().get(0).getPath();
        assertTrue(sourcePath.startsWith("files/spans/background"));
        assertEquals(backgroundFiles.size(), spans.size());
    }

    @Test
    void getSpanPath_givenInBackground_shouldReturnBackgroundSpanPath() {
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn(null);
        when(visibleScreenTracker.getCurrentlyVisibleScreen()).thenReturn("unknown");

        File path = fileProvider.provideSpanFile();

        assertTrue(path.getPath().startsWith("files/spans/background/"));
    }

    @Test
    void getSpanPath_givenInForeground_shouldReturnForegroundSpanPath() {
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("LauncherActivity");
        when(visibleScreenTracker.getCurrentlyVisibleScreen()).thenReturn("MainActivity");

        File path = fileProvider.provideSpanFile();

        assertFalse(path.getPath().startsWith("files/spans/background/"));
    }
}
