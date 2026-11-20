package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AcoposDiagnosticEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name and load full bundled ACOPOS diagnostic catalog`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ACOPOS Diagnosta", appName)

        val catalog = AcoposDiagnosticEngine.buildCatalog(context.assets)
        assertTrue("Expected full ACOPOS catalog > 750 entries, got ${catalog.size}", catalog.size > 750)

        val byCode = catalog.associateBy { it.code }
        assertNotNull(byCode[1])
        assertNotNull(byCode[4007])
        assertNotNull(byCode[6028])
        assertNotNull(byCode[7222])
        assertNotNull(byCode[9000])
        assertNotNull(byCode[10000])
        assertNotNull(byCode[29205])
        assertNotNull(byCode[32210])
        assertNotNull(byCode[35050])
        assertNotNull(byCode[65535])

        // Every entry must have Polish title, problem description, and repair steps
        catalog.forEach { err ->
            assertTrue("Missing Polish title for ${err.code}", err.titlePl.isNotBlank())
            assertTrue("Missing description for ${err.code}", err.problemDescription.isNotBlank())
            assertTrue("Missing repair steps for ${err.code}", err.repairSteps.isNotEmpty())
        }
    }
}
