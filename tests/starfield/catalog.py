"""Extract a research inventory from pinned native sources and validate IDs against its JAR.

This strict parser is for the locked version only, not a runtime crop catalog.
Unsupported source shapes fail instead of guessing metadata.
"""

import argparse
import json
import re
from pathlib import Path
from zipfile import ZipFile

from prepare import COMMIT, ROOT, STARFIELD, WORK, prepare

JAVA = WORK / "upstream/src/main/java/com/stardew/craft"
SEASONS = ["spring", "summer", "fall", "winter"]
GIANTS = {"cauliflower", "melon", "pumpkin", "powder_melon", "qi_fruit"}
NOTES = {
    "potato": "Additional native drops; 20% chance with repeat rolls",
    "rice": "Near water (radius 3): automatic watering and +25% growth speed; special geometry",
    "taro_root": "Near water: automatic watering and faster growth; special geometry",
    "fiber": "Grows without watering; all seasons; normal quality; special drop composition",
    "cactus_fruit": "All seasons; planting only indoors/in a greenhouse or on the island",
    "qi_fruit": "Normal quality; four one-day phases",
    "sunflower": "Additional seeds on harvest",
    "coffee_bean": "Produce also serves as seed; multiple beans per harvest",
    "grape": "Trellis; the seed maker returns summer wild seeds",
}


def without_comments(source):
    return re.sub(r"/\*.*?\*/|//[^\n]*", "", source, flags=re.S)


def method(source, name):
    match = re.search(r"(?:protected|public)\s+(?:final\s+)?[\w<>\[\].]+\s+"
                      + re.escape(name) + r"\([^)]*\)\s*\{", source)
    if match is None:
        return None
    depth = 1
    end = match.end()
    while depth:
        depth += (source[end] == "{") - (source[end] == "}")
        end += 1
    return source[match.end():end - 1]


def extract():
    classes = {}
    for path in (JAVA / "block/crop").glob("*.java"):
        source = without_comments(path.read_text(encoding="utf-8"))
        match = re.search(r"class (\w+) extends (\w+)", source)
        if match:
            classes[match[1]] = (match[2], source)

    def inherited(name, field):
        while name in classes:
            parent, source = classes[name]
            body = method(source, field)
            if body is not None:
                return source, body
            name = parent
        raise ValueError(f"Missing method {field}")

    def constant(name, field, default=None):
        try:
            _, body = inherited(name, field)
        except ValueError:
            if default is not None:
                return default
            raise
        match = re.search(r"return\s+(\d+|true|false|HarvestMethod\.\w+)\s*;", body)
        if match is None:
            raise ValueError(f"Unsupported {name}.{field}: {body}")
        value = match[1]
        return {"true": True, "false": False}.get(value, int(value) if value.isdigit() else value.split(".")[-1])

    items_source = (JAVA / "item/ModItems.java").read_text(encoding="utf-8")
    items = dict(re.findall(r'(\w+)\s*=\s*ITEMS\.register\("([^"]+)"', items_source))
    items.update(re.findall(r'(\w+)\s*=\s*VANILLA_CATEGORY_ITEMS\.get\("([^"]+)"', items_source))

    def item_id(name, field):
        _, body = inherited(name, field)
        match = re.search(r'return ModItems\.(\w+)\s*;', body)
        if match:
            return items[match[1]]
        match = re.search(r'ModItems\.VANILLA_CATEGORY_ITEMS\.get\("([^"]+)"\)', body)
        if match:
            return match[1]
        raise ValueError(f"Unsupported {name}.{field}: {body}")

    blocks_source = (JAVA / "block/ModBlocks.java").read_text(encoding="utf-8")
    registrations = re.findall(r'BLOCKS\.register\("([^"]+_crop)",\s*\(\) -> new '
                               r'com\.stardew\.craft\.block\.crop\.(\w+)\(([^)]*)\)', blocks_source)
    result = []
    with ZipFile(WORK / "dependencies" / STARFIELD["file"]) as archive:
        files = set(archive.namelist())
        for block, name, constructor in registrations:
            if name == "DeadCropBlock":
                continue
            source, body = inherited(name, "getPhaseDays")
            if "return PHASE_DAYS;" not in body:
                raise ValueError(f"Unsupported phase schedule: {name}")
            phases = re.search(r"PHASE_DAYS\s*=\s*(?:new int\[\]\s*)?\{([^}]+)\}", source)
            days = [int(value.strip()) for value in phases[1].split(",")]
            wild = name == "WildSeedCropBlock"
            if wild:
                season, seed_constant = re.fullmatch(
                    r"([0-3]), (?:com\.stardew\.craft\.item\.)?ModItems\.(\w+)", constructor).groups()
                seed = items[seed_constant]
                produce = None
                seasons = [SEASONS[int(season)]]
            else:
                seed = item_id(name, "getSeedsItem")
                produce = item_id(name, "getCropItem")
                _, season_body = inherited(name, "isInSeason")
                expression = re.findall(r"return ([^;]+);", season_body)[-1].strip()
                allowed = re.findall(r"(?:seasonForGrowth\(\)|season)\s*==\s*([0-3])", expression)
                if allowed:
                    seasons = [SEASONS[int(value)] for value in allowed]
                elif expression == "true":
                    seasons = SEASONS.copy()
                else:
                    raise ValueError(f"Unsupported seasons: {name}: {expression}")
            chain = []
            ancestor = name
            while ancestor in classes and ancestor != "StardewCropBlock":
                parent, source = classes[ancestor]
                chain.append(source)
                ancestor = parent
            tall = any("DoubleBlockHalf" in source for source in chain)
            short = block.removesuffix("_crop")
            notes = "Replaced by a seasonal ForageBlock at maturity" if wild else NOTES.get(short, "")
            entry = {
                "id": "stardewcraft:" + block,
                "class": name,
                "seed": "stardewcraft:" + seed,
                "produce": "stardewcraft:" + produce if produce else None,
                "phaseDays": days,
                "seasons": seasons,
                "regrowDays": constant(name, "getRegrowDays", 0) if constant(name, "canRegrow") else None,
                "geometry": "two_parts" if tall else "single_block",
                "harvest": "transforms_to_forage" if wild else constant(name, "getHarvestMethod").lower(),
                "giant": short in GIANTS,
                "soil": "native_crop_support",
                "notes": notes,
            }
            for item in (entry["seed"], entry["produce"]):
                if item and "assets/stardewcraft/models/item/" + item.split(":")[1] + ".json" not in files:
                    raise ValueError(f"Item model missing in official release: {item}")
            if "assets/stardewcraft/blockstates/" + block + ".json" not in files:
                raise ValueError(f"Block ID missing in official release: {block}")
            if f"com/stardew/craft/block/crop/{name}.class" not in files:
                raise ValueError(f"Crop class missing in official release: {name}")
            result.append(entry)
    if len(result) != 50 or len({entry["id"] for entry in result}) != 50:
        raise ValueError(f"Expected 46 ordinary + 4 wild crops, found {len(result)}")
    return sorted(result, key=lambda entry: entry["id"])


def write_catalog(crops):
    destination = ROOT / "docs/starfield"
    destination.mkdir(parents=True, exist_ok=True)
    output = {"schemaVersion": 1, "sourceCommit": COMMIT, "release": STARFIELD["version"], "crops": crops}
    (destination / "crop-catalog.json").write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    lines = ["# Starfield crop catalog: iteration 0", "",
             f"Release `{STARFIELD['version']}`, commit `{COMMIT}`. 46 crops and 4 wild-seed species.", "",
             "This is a design inventory, not a list of already implemented integrations.", "",
             "All IDs below use the `stardewcraft` namespace. Base soils: native",
             "`farmland`, `sandy_farmland`, `infertile_farmland` or `garden_pot`, with additional",
             "species-specific restrictions. `phaseDays` excludes the internal harvest sentinel.", "",
             "| Species | Seed | Produce | phaseDays | Seasons | Regrow, days | Geometry | Harvest | Giant | Notes |",
             "|---|---|---|---|---|---|---|---|---|---|"]
    for crop in crops:
        path = lambda value: value.split(":")[1] if value else "seasonal forage"
        lines.append("| " + " | ".join([
            path(crop["id"]), path(crop["seed"]), path(crop["produce"]),
            ",".join(map(str, crop["phaseDays"])), ", ".join(crop["seasons"]),
            str(crop["regrowDays"]) if crop["regrowDays"] is not None else "—",
            "2 parts" if crop["geometry"] == "two_parts" else "1 block", crop["harvest"],
            "yes" if crop["giant"] else "—", crop["notes"] or "—"]) + " |")
    lines += ["", "Source: official registrations and inherited crop-class methods;",
              "IDs/classes/models are validated against the JAR pinned in `tests/starfield/dependencies.json`.",
              "Reproduce with `python tests/starfield/catalog.py`. The generator deliberately stops",
              "on unsupported source shapes instead of substituting guesses for data.", ""]
    (destination / "CROP-CATALOG.md").write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--offline", action="store_true")
    prepare(parser.parse_args().offline)
    crops = extract()
    write_catalog(crops)
    print(f"Validated and wrote {len(crops)} crop records")
