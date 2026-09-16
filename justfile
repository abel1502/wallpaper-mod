set shell := ["pwsh.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-Command"]


clean:
    ./gradlew clean setupDecompWorkspace

build:
    ./gradlew build

run:
    ./gradlew runClient

deploy TARGET=("D:/Games/PrismLauncher/data/instances/Dev/minecraft/mods"): build
    $target = "{{TARGET}}"; \
    $lib = (gci ./build/libs | sort LastWriteTime | select -last 1); \
    rm "$target/abel-wallpaper-*.jar"; \
    $lib.CopyTo("$target/$($lib.Name)") | Out-Null
