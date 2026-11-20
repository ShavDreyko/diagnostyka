"""Testy jednostkowe silnika diagnostycznego Windows Portable."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from acopos_engine import (
    build_diagnostic_report,
    load_catalog,
    search_catalog,
)


class AcoposDesktopEngineTest(unittest.TestCase):
    def test_loads_full_catalog_and_searches(self) -> None:
        catalog = load_catalog()
        self.assertGreater(len(catalog), 750, f"Oczekiwano >750 kodów, otrzymano {len(catalog)}")

        by_code = {e.code: e for e in catalog}
        for required_code in (1, 4007, 6028, 7222, 9000, 10000, 29205, 32210, 35050, 65535):
            self.assertIn(required_code, by_code, f"Brak kodu {required_code}")
            err = by_code[required_code]
            self.assertTrue(err.title_pl.strip())
            self.assertTrue(err.problem_description.strip())
            self.assertGreater(len(err.repair_steps), 0)

        # Wyszukiwanie po kodzie i bez polskich znaków
        res_code = search_catalog(catalog, "7222")
        self.assertEqual(res_code[0].code, 7222)

        res_text = search_catalog(catalog, "hamulec")
        self.assertTrue(any(e.code == 6028 for e in res_text))

        report = build_diagnostic_report(by_code[7222], {0})
        self.assertIn("7222", report)
        self.assertIn("[x] 1.", report)


if __name__ == "__main__":
    unittest.main()
