package com.routing.brouter

import com.routing.brouter.Util.buildDoubleArray
import com.routing.brouter.Util.filenameForSegment
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test

class UtilUnitTest {

    @Test
    fun `segmentfilename valid`() {
        val segment = filenameForSegment(TEST_LAT_1, TEST_LON_1)
        assertThat("W5_N50", `is`(segment))
    }

    @Test
    fun `buildDoubleArray noVia`() {
        val doubles = buildDoubleArray(TEST_LON_1, TEST_LON_2, ArrayList())
        assertThat(2, `is`(doubles.size))
        assertThat(TEST_LON_1, `is`(doubles[0]))
        assertThat(TEST_LON_2, `is`(doubles[1]))
    }

    @Test
    fun `buildDoubleArray oneVia`() {

        val vias = ArrayList<Double>()
        vias.add(TEST_LON_3)

        val doubles = buildDoubleArray(TEST_LON_1, TEST_LON_2, vias)
        assertThat(3, `is`(doubles.size))
        assertThat(TEST_LON_1, `is`(doubles[0]))
        assertThat(TEST_LON_3, `is`(doubles[1]))
        assertThat(TEST_LON_2, `is`(doubles[2]))
    }

    @Test
    fun `buildDoubleArray twoVias`() {
        val vias = ArrayList<Double>()
        vias.add(TEST_LON_3)
        vias.add(TEST_LON_4)

        val doubles = buildDoubleArray(TEST_LON_1, TEST_LON_2, vias)
        assertThat(4, `is`(doubles.size))
        assertThat(TEST_LON_1, `is`(doubles[0]))
        assertThat(TEST_LON_3, `is`(doubles[1]))
        assertThat(TEST_LON_4, `is`(doubles[2]))
        assertThat(TEST_LON_2, `is`(doubles[3]))
    }

    companion object {
        const val TEST_LON_1 = -2.95
        const val TEST_LON_2 = -2.85
        const val TEST_LON_3 = -3.00
        const val TEST_LON_4 = -2.96

        const val TEST_LAT_1 = 54.2
    }
}