# Third-Party Licenses

WallKraft uses the following third-party code:

## Liquid Glass Engine

- **Source:** [Mortd3kay/liquid-glass-android](https://github.com/Mortd3kay/liquid-glass-android)
- **Copyright:** 2024 MRTDK
- **License:** Apache License 2.0
- **File:** `app/src/main/java/com/wallkraft/app/presentation/components/glass/LiquidGlass.kt`
- **Shader:** `app/src/main/assets/shaders/glass_displacement.agsl`

Adapted for WallKraft: per-frame println telemetry removed; used for a single
bottom tab capsule. AGSL path runs on API 33+; older releases fall back to
gradient simulation automatically.

```
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
