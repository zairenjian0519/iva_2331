from __future__ import annotations

import unittest

from app.inference import ModelEvent
from compat.business_rules import build_roi_tasks, evaluate


class BusinessCompatibilityTest(unittest.TestCase):
    @staticmethod
    def _config(threshold: float = 0.5, invalid_rois: list | None = None) -> dict:
        return {"algorithm_threshold": threshold, "invalid_rois": invalid_rois or [],
                "alarm_grade": "1", "alarm_Interval_period": 10, "target_threshold": 2}

    @staticmethod
    def _person(width: int = 200, height: int = 300) -> dict:
        return {"class_name": "person", "score": 0.9,
                "bbox": {"x": 100, "y": 80, "width": width, "height": height}}

    @staticmethod
    def _reflective(name: str, score: float, person: dict, width: int = 100, height: int = 100) -> dict:
        return {"class_name": name, "score": score,
                "bbox": {"x": 120, "y": 120, "width": width, "height": height},
                "parent_bbox": person["bbox"],
                "roi_bbox": {"x": 90, "y": 80, "width": person["bbox"]["width"] + 20,
                             "height": person["bbox"]["height"]}}

    @staticmethod
    def _detection(class_id: int, name: str, score: float,
                   x: int, y: int, width: int, height: int) -> dict:
        return {"class_id": class_id, "class_name": name, "score": score,
                "bbox": {"x": x, "y": y, "width": width, "height": height}}

    def test_primary_and_smoking_contract(self) -> None:
        configs = {
            0: {"algorithm_threshold": 0.2, "invalid_rois": [], "alarm_grade": "1", "alarm_Interval_period": 0, "target_threshold": 0},
            4: {"algorithm_threshold": 0.2, "invalid_rois": [], "alarm_grade": "1", "alarm_Interval_period": 0, "target_threshold": 0},
        }
        person = {"class_name": "person", "score": 0.9, "bbox": {"x": 100, "y": 80, "width": 150, "height": 300}}
        head = {"class_name": "head", "score": 0.8, "bbox": {"x": 130, "y": 90, "width": 50, "height": 50}}
        smoke_roi = {"x": 104, "y": 90, "width": 102, "height": 86}
        smoke = {"class_name": "smoking", "score": 0.7, "bbox": {"x": 120, "y": 110, "width": 30, "height": 30}, "parent_bbox": head["bbox"], "roi_bbox": smoke_roi}
        hand = {"class_name": "hand", "score": 0.8, "bbox": {"x": 125, "y": 105, "width": 30, "height": 30}, "parent_bbox": head["bbox"], "roi_bbox": smoke_roi}
        maps = {"person_helmet": {"head": {"kind": "alarm", "algorithm": 4}}, "smoking_phone": {}}
        persons, alarms = evaluate(
            {"person_helmet": ModelEvent([person, head]), "smoking_phone": ModelEvent([hand, smoke])},
            configs, maps, 720, 1280,
        )
        self.assertEqual(persons[0][:2], [0, 0.9])
        self.assertEqual({group[0][0] for group in alarms}, {0, 4})

    def test_smoking_roi_uses_non_person_primary_box(self) -> None:
        person = self._person()
        head = {"class_name": "head", "score": 0.9,
                "bbox": {"x": 100, "y": 80, "width": 50, "height": 40}}
        tasks = build_roi_tasks([person, head], {0: self._config()}, 720, 1280)
        smoking = [task for task in tasks if task.role == "smoking_phone"]
        self.assertEqual(len(smoking), 1)
        self.assertEqual(smoking[0].parent_bbox, head["bbox"])
        self.assertEqual(smoking[0].roi_bbox, {"x": 76, "y": 80, "width": 100, "height": 68})

    def test_reflective_roi_geometry_is_even_and_padded(self) -> None:
        person = self._person(width=201, height=301)
        task = next(task for task in build_roi_tasks([person], {6: self._config()}, 720, 1280)
                    if task.role == "reflective_clothes")
        self.assertEqual(task.roi_bbox, {"x": 90, "y": 80, "width": 222, "height": 302})

    def test_reflective_classes_use_original_person_box(self) -> None:
        person = self._person()
        maps = {"person_helmet": {}, "reflective_clothes": {}}
        for name, algorithm in (("reflective", 7), ("no_reflective", 6),
                                ("refleactive", 7), ("no_refleactive", 6)):
            configs = {algorithm: self._config()}
            _, groups = evaluate(
                {"person_helmet": ModelEvent([person]),
                 "reflective_clothes": ModelEvent([self._reflective(name, 0.834, person)])},
                configs, maps, 1000, 1000,
            )
            alarm = groups[0][0]
            self.assertEqual(alarm[:2], [algorithm, 0.83])
            self.assertEqual(alarm[2:6], [100, 80, 200, 300])
            self.assertEqual(alarm[6:], ["1", 10.0, 2])

    def test_reflective_uncertain_highest_suppresses_alarm(self) -> None:
        person = self._person()
        _, groups = evaluate(
            {"person_helmet": ModelEvent([person]),
             "reflective_clothes": ModelEvent([
                 self._reflective("reflective", 0.8, person),
                 self._reflective("uncertain", 0.95, person),
             ])},
            {7: self._config()}, {"person_helmet": {}, "reflective_clothes": {}}, 1000, 1000,
        )
        self.assertEqual(groups, [])

    def test_reflective_prefers_valid_area_then_falls_back(self) -> None:
        person = self._person()
        results = {"person_helmet": ModelEvent([person]), "reflective_clothes": ModelEvent([
            self._reflective("no_reflective", 0.95, person, 20, 20),
            self._reflective("reflective", 0.7, person, 100, 100),
        ])}
        _, groups = evaluate(results, {6: self._config(), 7: self._config()},
                             {"person_helmet": {}, "reflective_clothes": {}}, 1000, 1000)
        self.assertEqual(groups[0][0][0], 7)

        fallback = {"person_helmet": ModelEvent([person]), "reflective_clothes": ModelEvent([
            self._reflective("no_reflective", 0.88, person, 20, 20),
            self._reflective("reflective", 0.7, person, 15, 15),
        ])}
        _, groups = evaluate(fallback, {6: self._config(), 7: self._config()},
                             {"person_helmet": {}, "reflective_clothes": {}}, 1000, 1000)
        self.assertEqual(groups[0][0][0], 6)

    def test_reflective_filters_small_person_invalid_roi_and_disabled_class(self) -> None:
        small = self._person(width=100, height=100)
        maps = {"person_helmet": {}, "reflective_clothes": {}}
        _, groups = evaluate(
            {"person_helmet": ModelEvent([small]),
             "reflective_clothes": ModelEvent([self._reflective("no_reflective", 0.9, small)])},
            {6: self._config()}, maps, 1000, 1000,
        )
        self.assertEqual(groups, [])

        person = self._person()
        _, groups = evaluate(
            {"person_helmet": ModelEvent([person]),
             "reflective_clothes": ModelEvent([self._reflective("reflective", 0.9, person)])},
            {6: self._config()}, maps, 1000, 1000,
        )
        self.assertEqual(groups, [])

        _, groups = evaluate(
            {"person_helmet": ModelEvent([person]),
             "reflective_clothes": ModelEvent([self._reflective("no_reflective", 0.9, person)])},
            {6: self._config(invalid_rois=[[(90, 70), (310, 70), (310, 390), (90, 390)]])},
            maps, 1000, 1000,
        )
        self.assertEqual(groups, [])

    def test_oil_roles_use_full_frame_boxes_and_business_thresholds(self) -> None:
        results = {
            "oil_leak": ModelEvent([
                self._detection(0, "oil", 0.81, 10, 20, 30, 40),
                self._detection(0, "unexpected_plugin_label", 0.49, 50, 60, 70, 80),
            ]),
            "pipe_oil_leak": ModelEvent([
                self._detection(0, "pipe_oil", 0.91, 100, 110, 120, 130),
            ]),
        }
        _, groups = evaluate(results, {29: self._config(0.5), 31: self._config(0.5)}, {}, 480, 640)
        alarms = {group[0][0]: group[0] for group in groups}
        self.assertEqual(alarms[29][1:6], [0.81, 10, 20, 30, 40])
        self.assertEqual(alarms[31][1:6], [0.91, 100, 110, 120, 130])

    def test_oil_roles_respect_disabled_algorithm_and_invalid_roi(self) -> None:
        oil = self._detection(0, "oil", 0.9, 10, 20, 30, 40)
        _, groups = evaluate({"oil_leak": ModelEvent([oil])}, {}, {}, 480, 640)
        self.assertEqual(groups, [])
        _, groups = evaluate(
            {"oil_leak": ModelEvent([oil])},
            {29: self._config(0.5, invalid_rois=[[(0, 0), (50, 0), (50, 70), (0, 70)]])},
            {}, 480, 640,
        )
        self.assertEqual(groups, [])

    def test_uniform_reports_only_people_without_workwear_centers(self) -> None:
        clothed = self._detection(0, "head", 0.92, 10, 10, 100, 200)
        unclothed = self._detection(0, "head", 0.88, 200, 20, 100, 200)
        workwear = self._detection(1, "person", 0.85, 35, 70, 40, 80)
        _, groups = evaluate(
            {"uniform": ModelEvent([clothed, unclothed, workwear])},
            {30: self._config(0.5)}, {}, 480, 640,
        )
        self.assertEqual(len(groups), 1)
        self.assertEqual(groups[0][0][:6], [30, 0.88, 200, 20, 100, 200])

    def test_uniform_ignores_unknown_classes_and_disabled_algorithm(self) -> None:
        person = self._detection(0, "person", 0.9, 10, 10, 100, 200)
        unknown = self._detection(2, "other", 0.99, 20, 20, 50, 50)
        _, groups = evaluate({"uniform": ModelEvent([person, unknown])}, {}, {}, 480, 640)
        self.assertEqual(groups, [])

    def test_mask_roi_is_created_only_from_head_boxes(self) -> None:
        person = self._person()
        head = self._detection(0, "head", 0.9, 120, 90, 80, 80)
        tasks = build_roi_tasks([person, head], {8: self._config()}, 720, 1280)
        mask_tasks = [task for task in tasks if task.role == "mask"]
        self.assertEqual(len(mask_tasks), 1)
        self.assertEqual(mask_tasks[0].parent_bbox, head["bbox"])
        self.assertEqual(mask_tasks[0].algorithms, (8,))

    def test_safety_belt_uses_explicit_three_state_result(self) -> None:
        person = self._person()
        configs = {3: self._config(0.5), 16: self._config(0.5)}
        maps = {"person_helmet": {}, "safety_belt": {}}

        def evaluate_safety(*detections: dict) -> list[list[list]]:
            _, groups = evaluate(
                {
                    "person_helmet": ModelEvent([person]),
                    "safety_belt": ModelEvent(list(detections)),
                },
                configs,
                maps,
                720,
                1280,
            )
            return groups

        wearing = self._detection(0, "safety", 0.91, 110, 90, 180, 280)
        not_wearing = self._detection(2, "no_safety", 0.92, 110, 90, 180, 280)
        uncertain = self._detection(1, "uncertain_safety", 0.95, 110, 90, 180, 280)

        self.assertEqual(evaluate_safety(wearing)[0][0][0], 3)
        self.assertEqual(evaluate_safety(not_wearing)[0][0][0], 16)
        self.assertEqual(evaluate_safety(not_wearing, uncertain), [])
        self.assertEqual(evaluate_safety(), [])

    def test_fire_mask_vehicle_and_incomplete_bolt_mappings(self) -> None:
        maps = {
            "fire_smoke": {
                "fire": {"kind": "alarm", "algorithm": 2},
                "smoke": {"kind": "alarm", "algorithm": 2},
                "smoking": {"kind": "ignore"},
                "phone": {"kind": "ignore"},
            },
            "mask": {
                "mask": {"kind": "alarm", "algorithm": 8},
                "nomask": {"kind": "alarm", "algorithm": 9},
                "other": {"kind": "ignore"},
            },
            "vehicle": {
                "person": {"kind": "ignore"},
                "bicycle": {"kind": "alarm", "algorithm": 18},
                "car": {"kind": "alarm", "algorithm": 18},
            },
            "bolt": {
                "screw": {"kind": "ignore"},
                "loose": {"kind": "alarm", "algorithm": 24},
            },
        }
        results = {
            "fire_smoke": ModelEvent([
                self._detection(0, "fire", 0.9, 10, 10, 20, 20),
                self._detection(1, "smoke", 0.8, 40, 10, 20, 20),
                self._detection(2, "smoking", 0.99, 70, 10, 20, 20),
            ]),
            "mask": ModelEvent([
                self._detection(1, "nomask", 0.91, 100, 80, 60, 60),
            ]),
            "vehicle": ModelEvent([
                self._detection(0, "person", 0.99, 0, 0, 50, 100),
                self._detection(1, "bicycle", 0.7, 200, 100, 100, 80),
                self._detection(3, "car", 0.85, 350, 120, 140, 90),
            ]),
            "bolt": ModelEvent([
                self._detection(0, "screw", 0.99, 500, 200, 40, 40),
            ]),
        }
        configs = {
            value: self._config(0.5)
            for value in (2, 8, 9, 18, 24)
        }
        _, groups = evaluate(results, configs, maps, 720, 1280)
        by_algorithm = {group[0][0]: group for group in groups}
        self.assertEqual(len(by_algorithm[2]), 2)
        self.assertEqual(len(by_algorithm[9]), 1)
        self.assertEqual(len(by_algorithm[18]), 2)
        self.assertNotIn(8, by_algorithm)
        self.assertNotIn(24, by_algorithm)


if __name__ == "__main__":
    unittest.main()
