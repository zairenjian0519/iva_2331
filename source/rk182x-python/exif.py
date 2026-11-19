"""Small compatibility layer for the optional ``exif`` package.

The shared business module imports ``exif.Image`` for optional JPEG metadata
writing. RK182X keeps this compatibility code local to its engine so the
RK3588 environment is not changed.
"""

from io import BytesIO

from PIL import Image as PillowImage


class Image:
    def __init__(self, data):
        self._image = PillowImage.open(BytesIO(data))
        self._exif = self._image.getexif()

    @property
    def image_description(self):
        return self._exif.get(0x010E)

    @image_description.setter
    def image_description(self, value):
        self._exif[0x010E] = str(value)

    @property
    def software(self):
        return self._exif.get(0x0131)

    @software.setter
    def software(self, value):
        self._exif[0x0131] = str(value)

    def get_file(self):
        output = BytesIO()
        image_format = self._image.format or "JPEG"
        self._image.save(output, format=image_format, exif=self._exif.tobytes())
        return output.getvalue()
