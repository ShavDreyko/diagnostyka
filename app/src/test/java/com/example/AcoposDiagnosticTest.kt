package com.example

import com.example.data.AcoposAssetParser
import com.example.data.AcoposDiagnosticEngine
import com.example.model.ErrorCategory
import com.example.model.ErrorSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AcoposDiagnosticTest {

    @Test
    fun parsesPolishAndEnglishEntriesIncludingDottedCodesAndInfoLines() {
        val plSample = """
            Teksty błędów ACOPOS
            • 10.000: Parametr (y) identyfikacyjny brakujące
            • 32.210: Niewłaściwy ident interfejs Dzwoniąc plState () (Dodatkowe informacje w Logger)
            • 35.050: SMC Fail Safe: SMP - granica dolna pozycja jest większa lub równa górnej granicy
            • 7222: Stopień mocy: Podłączenie prądu sumarycznego X5: Nadprądowe (zwarcie doziemne)
            • 9040: Model temperatury odpowietrzacza: Przegrzanie - ruch zatrzymany
        """.trimIndent()

        val enSample = """
              7222: Power stage: Summation current connection X5: Overcurrent (Ground fault)
                   Info(REAL): Limit
             29205: The axis is not homed
                   Info(UINT): PLCopen_FB
        """.trimIndent()

        val plMap = AcoposAssetParser.parsePolishText(plSample)
        val enMap = AcoposAssetParser.parseEnglishText(enSample)
        val merged = AcoposAssetParser.mergeEntries(plMap, enMap)
        val byCode = merged.associateBy { it.code }

        assertNotNull(byCode[10000])
        assertNotNull(byCode[32210])
        assertNotNull(byCode[35050])
        assertNotNull(byCode[7222])
        assertNotNull(byCode[29205])
        assertEquals("Info(REAL): Limit", byCode[7222]?.infoDetail)
        assertTrue(byCode[9040]!!.titlePl.contains("Bleeder"))
        assertTrue(byCode[29205]!!.titlePl.contains("zbazowana"))

        val allCodes = byCode.keys
        val diag7222 = AcoposDiagnosticEngine.buildDiagnosticError(byCode[7222]!!, allCodes)
        assertEquals(ErrorCategory.POWER_DC_BUS, diag7222.category)
        assertEquals(ErrorSeverity.CRITICAL, diag7222.severity)
        assertTrue(diag7222.requiresHighVoltageCaution)
        assertTrue(diag7222.repairSteps.size >= 4)

        val diag35050 = AcoposDiagnosticEngine.buildDiagnosticError(byCode[35050]!!, allCodes)
        assertEquals(ErrorCategory.SAFETY_SAFEMC, diag35050.category)
        assertTrue(diag35050.problemDescription.isNotBlank())
        assertTrue(diag35050.repairSteps.isNotEmpty())
    }
}
