import unittest
from pathlib import Path

from rk182x_cards import build_config, parse_devices


class CardDiscoveryTest(unittest.TestCase):
    def test_no_card(self):
        self.assertEqual(parse_devices("List of ntb devices attached\n"), [])
        config, report = build_config([], Path("/models.json"), lambda _: (True, ""))
        self.assertEqual(config["rk182x_cards"], {})
        self.assertEqual(report, [])

    def test_sorted_slots_and_failed_probe(self):
        devices = parse_devices("0003:31:00.0 abc PCIE\n0002:21:00.0 def PCIE\n")
        config, report = build_config(devices, Path("/models.json"),
                                      lambda device: (device == "0003:31:00.0", "probe"))
        self.assertEqual([item["device_id"] for item in report], devices)
        self.assertEqual(list(config["rk182x_cards"]), ["2"])
        self.assertEqual(config["cameras"], {})

    def test_reject_duplicate_and_more_than_two(self):
        with self.assertRaisesRegex(ValueError, "duplicate"):
            parse_devices("0002:21:00.0 a PCIE\n0002:21:00.0 b PCIE\n")
        with self.assertRaisesRegex(ValueError, "at most two"):
            parse_devices("0001:11:00.0 a PCIE\n0002:21:00.0 b PCIE\n0003:31:00.0 c PCIE\n")


if __name__ == "__main__":
    unittest.main()
