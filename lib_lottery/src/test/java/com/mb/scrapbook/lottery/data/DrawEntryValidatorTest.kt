package com.mb.scrapbook.lottery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DrawEntryValidatorTest {

    @Test
    fun validEntryPasses() {
        assertNull(DrawEntryValidator.validate(listOf(1, 5, 11, 22, 28, 33), 9))
    }

    @Test
    fun duplicateRedRejected() {
        assertNotNull(DrawEntryValidator.validate(listOf(1, 1, 11, 22, 28, 33), 9))
    }

    @Test
    fun outOfRangeRejected() {
        assertNotNull(DrawEntryValidator.validate(listOf(0, 5, 11, 22, 28, 33), 9))
        assertNotNull(DrawEntryValidator.validate(listOf(1, 5, 11, 22, 28, 34), 9))
        assertNotNull(DrawEntryValidator.validate(listOf(1, 5, 11, 22, 28, 33), 17))
        assertNotNull(DrawEntryValidator.validate(listOf(1, 5, 11, 22, 28, 33), 0))
    }

    @Test
    fun incompleteRejected() {
        assertEquals("红球未录满 6 个", DrawEntryValidator.validate(listOf(1, 5, 11, 22, 28, -1), 9))
    }
}
