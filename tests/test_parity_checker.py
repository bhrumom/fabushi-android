import importlib.util
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "scripts/check_grok_android_parity.py"
SPEC = importlib.util.spec_from_file_location("check_grok_android_parity", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)

class ParityCheckerTest(unittest.TestCase):
    def test_phase0_inventory_is_complete(self):
        result = MODULE.run_checks(strict=False)
        self.assertEqual([], result.errors)
        self.assertEqual(2046, result.summary["inventory_files"])
        self.assertEqual(2046, result.summary["ledger_rows"])

    def test_strict_gate_is_intentionally_not_green_during_migration(self):
        result = MODULE.run_checks(strict=True)
        self.assertGreater(len(result.errors), 0)
        self.assertGreater(result.summary["legacy_monoliths_present"], 0)

if __name__ == "__main__":
    unittest.main()
