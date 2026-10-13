"""Install a release JAR in an isolated, ordinary NeoForge server and restart it.

Requires an already installed NeoForge 21.1.226 server under the ignored scratch
directory. Does not put development classes or a probe mod on its classpath.
"""
import argparse
import gzip
import hashlib
import json
import os
from pathlib import Path
import queue
import shutil
import struct
import subprocess
import threading
import time

ROOT = Path(__file__).resolve().parents[2]
POSITION = (300000, 65, 300000)


def nbt(path):
    data = memoryview(gzip.decompress(path.read_bytes()))
    offset = 0

    def read(fmt):
        nonlocal offset
        value = struct.unpack_from(fmt, data, offset)
        offset += struct.calcsize(fmt)
        return value[0] if len(value) == 1 else value

    def string():
        nonlocal offset
        size = read('>H')
        value = bytes(data[offset:offset+size]).decode('utf-8')
        offset += size
        return value

    def tag(kind):
        nonlocal offset
        if kind in range(1, 7):
            return read({1: '>b', 2: '>h', 3: '>i', 4: '>q', 5: '>f', 6: '>d'}[kind])
        if kind == 7:
            size = read('>i'); value = bytes(data[offset:offset+size]); offset += size
            return value
        if kind == 8:
            return string()
        if kind == 9:
            child, size = read('>b'), read('>i')
            return [tag(child) for _ in range(size)]
        if kind == 10:
            value = {}
            while (child := read('>b')) != 0:
                name = string(); value[name] = tag(child)
            return value
        if kind in (11, 12):
            return [read('>i' if kind == 11 else '>q') for _ in range(read('>i'))]
        raise ValueError(f'Unsupported NBT tag {kind}')

    kind = read('>b'); string()
    return tag(kind)


def run_server(folder, java, first, reload_before_stop=False):
    prefix = 'reload-' if reload_before_stop else ''
    log_path = folder / (prefix + ('write.log' if first else 'read.log'))
    messages = queue.Queue()
    command = [str(java), '-Xmx4G', '@user_jvm_args.txt',
               '@libraries/net/neoforged/neoforge/21.1.226/win_args.txt', 'nogui']
    with log_path.open('w', encoding='utf-8') as log:
        process = subprocess.Popen(command, cwd=folder, stdin=subprocess.PIPE,
                                   stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                   text=True, encoding='utf-8', errors='replace')

        def collect():
            for line in process.stdout:
                log.write(line); log.flush(); messages.put(line)
        reader = threading.Thread(target=collect, daemon=True); reader.start()

        def send(command):
            process.stdin.write(command + '\n'); process.stdin.flush()

        try:
            deadline = time.monotonic() + 240
            ready = False
            while time.monotonic() < deadline:
                if process.poll() is not None:
                    raise RuntimeError(f'Server exited before startup: {log_path}')
                try:
                    line = messages.get(timeout=1)
                except queue.Empty:
                    continue
                if 'Done (' in line:
                    ready = True; break
            if not ready:
                raise TimeoutError(f'Server startup timed out: {log_path}')
            print(f'Ordinary installed server started: {log_path.name}', flush=True)
            send('execute in stardewcraft:stardew_valley run forceload add 300000 300000')
            if first:
                send('execute in stardewcraft:stardew_valley run setblock 300000 64 300000 stardewcraft:farmland[moisture=7]')
                send('execute in stardewcraft:stardew_valley run setblock 300000 65 300000 stardewcraft:potato_crop')
            # Real server ticks, chunk load/migration and native callbacks run
            # before the ordinary stop command saves authoritative world data.
            time.sleep(8)
            if reload_before_stop:
                # Discard startup messages so an old catalog publication cannot
                # masquerade as completion of the new asynchronous reload.
                while not messages.empty():
                    messages.get_nowait()
                send('reload')
                deadline = time.monotonic() + 180
                while time.monotonic() < deadline:
                    if process.poll() is not None:
                        raise RuntimeError(f'Server exited during reload: {log_path}')
                    try:
                        line = messages.get(timeout=1)
                    except queue.Empty:
                        continue
                    if 'Starfield compatibility catalog: 50 native crops, 0 invalid entries' in line:
                        break
                else:
                    raise TimeoutError(f'Reload did not publish the native catalog: {log_path}')
                time.sleep(2)
                # Exercise the historical release-of-ticket -> shutdown sequence
                # using real console commands in the ordinary installed artifact.
                send('execute in stardewcraft:stardew_valley run forceload remove 300000 300000')
            send('stop')
            if process.wait(timeout=120) != 0:
                raise RuntimeError(f'Installed server failed: {log_path}')
            reader.join(timeout=5)
            print(f'Ordinary shutdown completed: {log_path.name}', flush=True)
        except subprocess.TimeoutExpired:
            jcmd = Path(java).resolve().parent / 'jcmd.exe'
            if jcmd.is_file() and process.poll() is None:
                snapshot = subprocess.run([str(jcmd), str(process.pid), 'Thread.print', '-l'],
                                          capture_output=True, text=True, encoding='utf-8',
                                          errors='replace', timeout=20)
                log_path.with_name(log_path.stem + '-save-threads.txt').write_text(
                    snapshot.stdout + snapshot.stderr, encoding='utf-8')
            raise
        finally:
            if process.poll() is None:
                process.terminate(); process.wait(timeout=20)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('release', type=Path)
    parser.add_argument('resourcefulconfig', type=Path)
    parser.add_argument('--java', type=Path)
    parser.add_argument('--reload-before-stop', action='store_true',
                        help='Wait for real datapack reload, unforce the crop chunk, then stop both JVMs')
    args = parser.parse_args()
    java = args.java or (Path(os.environ['JAVA_HOME']) / 'bin/java.exe' if os.environ.get('JAVA_HOME') else shutil.which('java'))
    if java is None:
        parser.error('Set JAVA_HOME to Java 21 or pass --java')
    folder = ROOT / 'build/starfield-standalone'
    mods = folder / 'mods'; mods.mkdir(exist_ok=True)
    lock = json.loads((ROOT / 'tests/starfield/dependencies.json').read_text(encoding='utf-8'))['starfield']
    native = ROOT / 'build/starfield-iteration0/dependencies' / lock['file']
    assert hashlib.sha256(native.read_bytes()).hexdigest() == lock['sha256']
    for source in (args.release, args.resourcefulconfig, native):
        shutil.copy2(source, mods / source.name)
    config = folder / 'config'; config.mkdir(exist_ok=True)
    (config / 'agricraft.jsonc').write_text('{"compat":{"starfield":true}}', encoding='utf-8')
    (folder / 'eula.txt').write_text('eula=true\n', encoding='utf-8')
    (folder / 'server.properties').write_text(
        'server-ip=127.0.0.1\nserver-port=25580\nonline-mode=false\nview-distance=4\n'
        'simulation-distance=4\nspawn-protection=0\nmax-tick-time=180000\n', encoding='utf-8')
    saved = folder / 'world/data/agricraft_starfield_plants.dat'
    witnesses = []
    for first in (True, False):
        run_server(folder, java, first, args.reload_before_stop)
        state = nbt(saved)['data']
        assert state['schema'] == 1
        x, y, z = POSITION
        packed = ((x & 0x3ffffff) << 38) | ((z & 0x3ffffff) << 12) | (y & 0xfff)
        row = next(row for row in state['plants'] if row['root'] == packed
                   and row['dimension'] == 'stardewcraft:stardew_valley')
        assert row['type'] == 'stardewcraft:potato_crop'
        assert set(row['genome']['genes']) == {'species', 'growth', 'gain', 'strength', 'resistance', 'fertility', 'mutativity'}
        assert len(row['plantingId']) == 4
        witnesses.append(row)
    assert witnesses[0] == witnesses[1], 'Installed artifact restart changed planting identity/genome'
    digest = hashlib.sha256(args.release.read_bytes()).hexdigest()
    result = folder / ('reload-result.txt' if args.reload_before_stop else 'result.txt')
    result.write_text(
        f'PASS installed release {args.release.name}\nSHA256 {digest}\n'
        'Two ordinary dedicated-server JVMs; native crop, full baseline genotype and UUID retained after restart.\n' +
        ('Each JVM completed a real datapack reload and removed the chunk ticket before ordinary stop/save.\n'
         if args.reload_before_stop else ''), encoding='utf-8')
    print(result.read_text(encoding='utf-8'), flush=True)


if __name__ == '__main__':
    main()
