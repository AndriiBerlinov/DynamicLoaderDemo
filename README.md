## How it works

- App downloads external module.jar at runtime
- Module is a dex-jar built via d8
- App loads it using DexClassLoader
- After execution module is deleted and execution state is persisted

## Module build

./gradlew :dynamicmodule:moduleJar
