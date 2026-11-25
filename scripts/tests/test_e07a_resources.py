"""Static DanPlus branding and Brazilian Portuguese regressions."""

from collections import Counter
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "app/src/main"
RES = MAIN / "res"
ANDROID = "{http://schemas.android.com/apk/res/android}"
FORMAT = re.compile(
    r"%(?:(?P<index>[1-9]\d*)\$)?[-#+0,(<]*\d*(?P<precision>\.\d+)?(?P<type>[tT][A-Za-z]|[A-Za-z%])"
)
CURRENT_PRODUCT = re.compile(r"\b(?:Nuvio Arc|Arc|DanPlus 2\.0)\b|\bDAN\+(?!\w)")
ATTRIBUTION = {
    "about_made_with_love",
    "licenses_attributions_nuvio_body",
    "licenses_attributions_nuvio_title",
}


def catalog(qualifier):
    strings = {}
    for file in sorted((RES / qualifier).glob("*.xml")):
        for element in ET.parse(file).getroot():
            if element.tag not in ("string", "plurals", "string-array"):
                continue
            key = (element.tag, element.attrib["name"])
            if key in strings:
                raise AssertionError(f"Duplicate resource in {qualifier}: {file.name}, {key}")
            strings[key] = element
    return strings


def text_of(element):
    return "".join(element.itertext())


def formats(element):
    implicit_index = 1
    args = Counter()
    for token in FORMAT.finditer(text_of(element)):
        index, precision, conversion = token.group("index", "precision", "type")
        if conversion in ("%", "n"):
            args[conversion] += 1
            continue
        if index is None:
            index = implicit_index
            implicit_index += 1
        args[int(index), conversion, precision] += 1
    return args


def webp_geometry(path):
    with path.open("rb") as file:
        header = file.read(25)
    if header[:4] != b"RIFF" or header[8:16] != b"WEBPVP8L" or header[20:21] != b"\x2f":
        raise AssertionError(f"Invalid lossless WebP banner: {path}")
    flags = int.from_bytes(header[21:25], "little")
    return (flags & 0x3FFF) + 1, ((flags >> 14) & 0x3FFF) + 1, bool(flags & (1 << 28))


class E07AResourceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.default = catalog("values")
        cls.brazil = catalog("values-pt-rBR")

    def test_brazil_covers_translatable_resources_and_preserves_format(self):
        for key, original in self.default.items():
            if original.attrib.get("translatable") == "false":
                continue
            with self.subTest(resource=key):
                translated = self.brazil[key]
                self.assertEqual(original.attrib.get("formatted"), translated.attrib.get("formatted"))
                if original.tag == "plurals":
                    quantities = {item.attrib["quantity"]: item for item in original}
                    self.assertIn("other", {item.attrib["quantity"] for item in translated})
                    for item in translated:
                        base = quantities.get(item.attrib["quantity"], quantities["other"])
                        self.assertEqual(formats(base), formats(item), item.attrib["quantity"])
                elif original.tag == "string-array":
                    self.assertEqual([formats(item) for item in original], [formats(item) for item in translated])
                elif original.attrib.get("formatted") != "false":
                    self.assertEqual(formats(original), formats(translated))

    def test_product_name_is_shared_and_scout_name_is_approved(self):
        brand = self.default["string", "app_name"]
        self.assertEqual("DanPlus", text_of(brand))
        self.assertEqual("false", brand.attrib.get("translatable"))
        for file in (ROOT / "app/src").glob("*/res/values*/strings.xml"):
            if file != RES / "values/strings.xml":
                self.assertIsNone(ET.parse(file).find("string[@name='app_name']"), file)
        self.assertEqual("Descubra-me", text_of(self.brazil["string", "scout_title"]))
        self.assertEqual("DanPlus", text_of(self.brazil["string", "cd_nuvio_logo"]))
        for qualifier in (self.default, self.brazil):
            for (_, name), resource in qualifier.items():
                if name not in ATTRIBUTION:
                    self.assertIsNone(CURRENT_PRODUCT.search(text_of(resource)), name)

    def test_origin_license_and_service_names_are_visible(self):
        for qualifier in (self.default, self.brazil):
            origin = text_of(qualifier["string", "licenses_attributions_nuvio_body"])
            for credit in ("Nuvio Arc", "Nuvio TV", "Tapframe", "ysosrs123", "GNU General Public License v3.0"):
                self.assertIn(credit, origin)
            self.assertEqual("Nuvio Sync", text_of(qualifier["string", "trakt_watch_progress_source_nuvio"]))
            self.assertIn("api.nuvio.tv", text_of(qualifier["string", "custom_server_error_official_active"]))
            self.assertIn("nuvio-collections.json", text_of(qualifier["string", "collections_file_description"]))

    def test_launcher_banners_and_alias_targets_keep_their_resource_contract(self):
        manifest = ET.parse(MAIN / "AndroidManifest.xml")
        launcher = manifest.find("application")
        self.assertEqual("${danplusLauncherLabel}", launcher.attrib[ANDROID + "label"])
        components = [launcher, *launcher.findall("activity"), *launcher.findall("activity-alias")]
        banners = [element for element in components if ANDROID + "banner" in element.attrib]
        for element in banners:
            self.assertEqual("${danplusLauncherLabel}", element.attrib[ANDROID + "label"])
        names = {
            element.attrib[ANDROID + "banner"].removeprefix("@mipmap/")
            for element in banners
        }
        for item in ET.parse(RES / "values/icon_pack.xml").getroot():
            value = text_of(item)
            if value.startswith("@mipmap/banner"):
                self.assertIn(value.removeprefix("@mipmap/"), names)
        self.assertEqual(80, len(names))
        for name in names:
            clear = "_clear_" in name
            for qualifier, width, height in (("xhdpi", 640, 360), ("xxxhdpi", 1280, 720)):
                with self.subTest(banner=name, density=qualifier):
                    self.assertEqual(
                        (width, height, clear),
                        webp_geometry(RES / f"mipmap-{qualifier}" / f"{name}.webp"),
                    )


if __name__ == "__main__":
    unittest.main()
