"""Fetch the pinned official release and reference sources; never use patch archives."""

import argparse
import hashlib
import json
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[2]
WORK = ROOT / "build/starfield-iteration0"
LOCK = json.loads((Path(__file__).parent / "dependencies.json").read_text(encoding="utf-8"))
STARFIELD = LOCK["starfield"]
COMMIT = STARFIELD["commit"]
REFERENCE_FILES = {
    "gradle.properties", "LICENSE.md",
    "src/main/java/com/stardew/craft/block/ModBlocks.java",
    "src/main/java/com/stardew/craft/item/ModItems.java",
    "src/main/java/com/stardew/craft/block/terrain/TerrainSoils.java",
    "src/main/java/com/stardew/craft/block/utility/SprinklerBlock.java",
    "src/main/java/com/stardew/craft/block/utility/SprinklerTier.java",
    "src/main/java/com/stardew/craft/block/utility/GardenPotBlock.java",
    "src/main/java/com/stardew/craft/item/tool/WateringCanItem.java",
    "src/main/java/com/stardew/craft/manager/SprinklerManager.java",
    "src/main/java/com/stardew/craft/manager/CropGrowthManager.java",
    "src/main/java/com/stardew/craft/manager/FertilizerManager.java",
    "src/main/java/com/stardew/craft/blockentity/SeedMakerBlockEntity.java",
    "src/main/java/com/stardew/craft/blockentity/TimedProductionBlockEntity.java",
    "src/main/java/com/stardew/craft/blockentity/KegBlockEntity.java",
    "src/main/java/com/stardew/craft/blockentity/DehydratorBlockEntity.java",
    "src/main/java/com/stardew/craft/gametest/MachineProductionGameTests.java",
    "src/main/java/com/stardew/craft/item/artisan/SeedMakerOutputResolver.java",
    "src/main/java/com/stardew/craft/api/v1/machine/StardewArtisanResolvers.java",
    "src/main/java/com/stardew/craft/api/v1/internal/machine/StardewArtisanResolverRegistry.java",
    "src/main/java/com/stardew/craft/item/WildSeedsItem.java",
    "src/main/java/com/stardew/craft/item/MixedSeedsItem.java",
    "src/main/java/com/stardew/craft/item/MixedFlowerSeedsItem.java",
    "src/main/java/com/stardew/craft/core/ModDimensions.java",
    "src/main/java/com/stardew/craft/core/FarmAreaResolver.java",
    "src/main/java/com/stardew/craft/farm/FarmInstance.java",
    "src/main/java/com/stardew/craft/farm/FarmInstanceRegistry.java",
    "src/main/java/com/stardew/craft/farm/FarmType.java",
    "src/main/java/com/stardew/craft/farm/OfflineFarmCatchUp.java",
    "src/main/java/com/stardew/craft/farm/FarmDailyProcessHelper.java",
    "src/main/java/com/stardew/craft/farming/SeasonLocationRules.java",
    "src/main/java/com/stardew/craft/farming/CropPlanting.java",
    "src/main/java/com/stardew/craft/block/nature/ForageBlock.java",
    "src/main/java/com/stardew/craft/api/v1/world/StardewLocations.java",
    "src/main/java/com/stardew/craft/api/v1/world/StardewLocation.java",
    "src/main/java/com/stardew/craft/api/v1/world/StardewLocationDefinition.java",
    "src/main/java/com/stardew/craft/greenhouse/GreenhouseManager.java",
    "src/main/java/com/stardew/craft/interior/InteriorRegionRegistry.java",
    "src/main/java/com/stardew/craft/item/crop/CactusSeedItem.java",
    "src/main/java/com/stardew/craft/block/FertilizerType.java",
    "src/main/java/com/stardew/craft/item/tool/ScytheItem.java",
    "src/main/java/com/stardew/craft/event/ScytheHarvestEvents.java",
    "src/main/java/com/stardew/craft/event/FarmAreaProtectionEvents.java",
    "src/main/java/com/stardew/craft/player/PlayerStardewDataAPI.java",
    "src/main/java/com/stardew/craft/player/PlayerStardewData.java",
    "src/main/java/com/stardew/craft/player/PlayerDataManager.java",
    "src/main/java/com/stardew/craft/gametest/CarrotCropGameTests.java",
    "src/main/java/com/stardew/craft/gametest/GiantCropGrowthGameTests.java",
    "src/main/java/com/stardew/craft/gametest/CropChunkLoadGameTests.java",
    "src/main/java/com/stardew/craft/gametest/CropSelectionAssertions.java",
    "src/main/java/com/stardew/craft/gametest/ApiContractGameTests.java",
    "src/main/java/com/stardew/craft/gametest/DailyInfoApiGameTests.java",
}
REFERENCE_PREFIXES = (
    "src/main/java/com/stardew/craft/block/crop/",
    "src/main/java/com/stardew/craft/api/v1/agriculture/",
    "src/main/java/com/stardew/craft/api/v1/internal/crop/",
    "src/main/java/com/stardew/craft/api/v1/internal/giant/",
    "src/main/java/com/stardew/craft/item/crop/",
    "src/main/java/com/stardew/craft/api/v1/machine/",
    "src/main/java/com/stardew/craft/api/v1/internal/machine/",
)


def fetch(url):
    request = Request(url, headers={"User-Agent": "AgriCraft-iteration0"})
    with urlopen(request, timeout=120) as response:
        return response.read()


def verify_release(path):
    with path.open("rb") as stream:
        checksum = hashlib.file_digest(stream, "sha256").hexdigest()
    if path.stat().st_size != STARFIELD["size"] or checksum != STARFIELD["sha256"]:
        raise ValueError(f"Official release size/SHA-256 mismatch: {path}")


def prepare(offline=False):
    jar = WORK / "dependencies" / STARFIELD["file"]
    jar.parent.mkdir(parents=True, exist_ok=True)
    if not jar.exists():
        if offline:
            raise FileNotFoundError(jar)
        partial = jar.with_suffix(".jar.part")
        request = Request(STARFIELD["url"], headers={"User-Agent": "AgriCraft-iteration0"})
        with urlopen(request, timeout=120) as response, partial.open("wb") as stream:
            while chunk := response.read(1024 * 1024):
                stream.write(chunk)
        verify_release(partial)
        partial.replace(jar)
    verify_release(jar)
    tree_path = WORK / "release-tree.json"
    if not tree_path.exists():
        if offline:
            raise FileNotFoundError(tree_path)
        tree_path.write_bytes(fetch(
            f"https://api.github.com/repos/ChangQingElysium/Starfield-Pastoral/git/trees/{COMMIT}?recursive=1"))
    tree = json.loads(tree_path.read_text(encoding="utf-8-sig"))
    if tree["sha"] != COMMIT or tree.get("truncated"):
        raise ValueError("Source tree must be complete and match the pinned release commit")
    entries = [entry for entry in tree["tree"] if entry["type"] == "blob" and (
        entry["path"] in REFERENCE_FILES or entry["path"].startswith(REFERENCE_PREFIXES))]

    def prepare_source(entry):
        path = WORK / "upstream" / entry["path"]
        if not path.exists():
            if offline:
                raise FileNotFoundError(path)
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(fetch(
                f"https://raw.githubusercontent.com/ChangQingElysium/Starfield-Pastoral/{COMMIT}/{entry['path']}"))
        content = path.read_bytes()
        blob = hashlib.sha1(f"blob {len(content)}\0".encode() + content).hexdigest()
        if blob != entry["sha"]:
            raise ValueError(f"Reference source differs from the official Git blob: {path}")

    with ThreadPoolExecutor(max_workers=6) as pool:
        list(pool.map(prepare_source, entries))
    print(f"Verified official {STARFIELD['version']} JAR and {len(entries)} source blobs")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--offline", action="store_true", help="Only verify already fetched files")
    parser.add_argument("--incompatible", action="store_true", help="Fetch/verify the official older release for inactive-version acceptance")
    args = parser.parse_args()
    if args.incompatible:
        release = LOCK["unverified_starfield"]
        jar = WORK / "dependencies" / release["file"]
        jar.parent.mkdir(parents=True, exist_ok=True)
        if not jar.exists():
            if args.offline: raise FileNotFoundError(jar)
            partial = jar.with_suffix(".jar.part")
            with urlopen(Request(release["url"], headers={"User-Agent": "AgriCraft-local-compat-validation"}), timeout=120) as response, partial.open("wb") as stream:
                while chunk := response.read(1024 * 1024): stream.write(chunk)
            partial.replace(jar)
        with jar.open("rb") as stream: checksum = hashlib.file_digest(stream, "sha256").hexdigest()
        if jar.stat().st_size != release["bytes"] or checksum != release["sha256"]:
            raise ValueError(f"Official older release size/SHA-256 mismatch: {jar}")
        print(f"Verified official unverified-runtime release {release['version']}")
    else:
        prepare(args.offline)
