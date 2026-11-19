import tempfile
from pathlib import Path
import unittest

from patch_rknn3_library import ORIGINAL, patch_file


class Rknn3LibraryPatchTest(unittest.TestCase):
    def test_only_the_expected_declaration_is_replaced(self):
        with tempfile.TemporaryDirectory() as folder:
            source = Path(folder) / "rknn3_types.py"
            library = Path(folder) / "librknn3_api.so"
            library.touch()
            source.write_text(ORIGINAL + "\nOTHER = 1\n", encoding="utf-8")
            patch_file(source, library)
            self.assertEqual(source.read_text(encoding="utf-8"),
                             f"LIBRKNN3RT_PATH = {str(library)!r}\nOTHER = 1\n")
            with self.assertRaisesRegex(ValueError, "unexpected"):
                patch_file(source, library)


if __name__ == "__main__":
    unittest.main()
