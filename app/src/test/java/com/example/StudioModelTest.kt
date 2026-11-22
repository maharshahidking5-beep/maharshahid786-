package com.example

import com.example.model.ProjectData
import com.example.model.VideoClip
import com.example.ui.components.formatShortDuration
import com.example.ui.components.formatTimecode
import org.junit.Assert.assertEquals
import org.junit.Test

class StudioModelTest {

    @Test
    fun `clip effective duration scales with trim and speed`() {
        val clip = VideoClip(
            uriString = "file:///sample.mp4",
            displayName = "Clip 1",
            totalDurationMs = 10000L,
            trimStartMs = 2000L,
            trimEndMs = 8000L,
            speed = 2.0f
        )
        assertEquals(3000L, clip.effectiveDurationMs)
    }

    @Test
    fun `project total duration sums clip effective durations`() {
        val project = ProjectData(
            title = "s.s Reel",
            clips = listOf(
                VideoClip(uriString = "a", displayName = "A", totalDurationMs = 4000L, trimStartMs = 0L, trimEndMs = 4000L, speed = 1f),
                VideoClip(uriString = "b", displayName = "B", totalDurationMs = 6000L, trimStartMs = 1000L, trimEndMs = 5000L, speed = 0.5f)
            )
        )
        assertEquals(12000L, project.totalDurationMs)
    }

    @Test
    fun `timecode formatting outputs mm_ss_tenths`() {
        assertEquals("00:04.2", formatTimecode(4250L))
        assertEquals("01:05", formatShortDuration(65000L))
    }
}
