# TaCZ Compatibility and Config Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make TaCZinTetra use the two-file configuration contract, fix modular-gun aiming reticle behavior, and provide configurable native TaCZ restrictions with optional JEI integration.

**Architecture:** Keep the editable nine-category resource catalog in one Gson JSON file and keep primitive rules plus the initial 3x3 recipe in a Forge TOML config. Add focused policy services for native TaCZ filtering and recipe blocking so gameplay code does not read raw config paths. Make JEI integration optional and fail-closed when JEI is absent.

**Tech Stack:** Minecraft 1.20.1, Forge 47.4.16, Java 17, Gson, ForgeConfigSpec, TaCZ 1.1.8-hotfix, Tetra 6.17.0, JUnit 5.

**Spec:** `TaCZ_in_Tetra_完整项目规范_Codex版.md` plus the approved 2026-09-05 user requirements in the conversation.

## Global Constraints

- Runtime baseline is Forge 47.4.16 and Minecraft 1.20.1.
- TaCZ baseline is `1.1.8-hotfix`; Tetra baseline is `6.17.0`.
- The public config paths are exactly `config/taczintetra.json` and `config/taczintetra.toml`.
- The JSON domain catalog has nine categories: barrels, bodies, magazines, optics, stocks, grips, enchantments, repair_agents, special_inlays.
- Existing user JSON values must be migrated without silently discarding data.
- Native TaCZ restrictions are configurable and default to enabled.
- KubeJS or another mod must be able to override the initial recipe after registration.
- Dev-only automation must remain gated and must use the `tit` launch-property prefix.

### Task 1: Add focused tests for the new configuration contract

**Files:**
- Create: `src/test/java/com/pycoder/taczintetra/config/TaczConfigContractTest.java`
- Modify: `src/test/java/com/pycoder/taczintetra/config/ModuleConfigTest.java`

**Interfaces:**
- Consumes: current JSON catalog parser and the new rule-config API.
- Produces: executable expectations for migration, nine category names, recipe tags, and default rules.

- [ ] **Step 1: Write failing tests** for the exact new paths and defaults.
- [ ] **Step 2: Run `gradlew.bat test --tests '*TaczConfigContractTest' --no-daemon` and verify failure.**
- [ ] **Step 3: Keep the tests limited to observable paths, parsed defaults, and migration output.**

### Task 2: Implement JSON migration and the nine-category schema

**Files:**
- Modify: `src/main/java/com/pycoder/taczintetra/config/ModuleConfigManager.java`
- Modify: `src/main/java/com/pycoder/taczintetra/config/ModuleConfig.java`
- Create/modify: `src/main/resources/defaultconfigs/taczintetra.json`
- Preserve: `src/main/resources/defaultconfigs/taczintetra/modules.json` only as a migration source if required by tests.

**Interfaces:**
- Consumes: `Path configDirectory`.
- Produces: `ModuleConfigManager.loadOrCreate(Path)` loading `config/taczintetra.json`, one-time migration from the old nested file, and a stable nine-category `ModuleConfig` view.

- [ ] **Step 1: Change the primary path to `taczintetra.json`.**
- [ ] **Step 2: Migrate the old file atomically, writing a `.bak` copy before any replacement.**
- [ ] **Step 3: Map legacy `attachments` into `optics`, `stocks`, and `grips` by slot; retain enchantments, repair agents, and special inlays with empty defaults where legacy data has no equivalent.**
- [ ] **Step 4: Merge missing defaults without overwriting user values.**
- [ ] **Step 5: Run the focused config tests.**

### Task 3: Add `taczintetra.toml` rules and initial recipe configuration

**Files:**
- Create: `src/main/java/com/pycoder/taczintetra/config/TaczInTetraForgeConfig.java`
- Create: `src/main/resources/defaultconfigs/taczintetra.toml`
- Modify: `src/main/java/com/pycoder/taczintetra/TaCZinTetra.java`
- Modify: `src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java`

**Interfaces:**
- Produces: typed accessors for `disableNativeTaczItems`, `disableNativeTaczRecipes`, `disableNativeTaczWorkbenches`, `onlySemiFinishedParts`, `onlyRepairAgents`, `jeiMode`, nine initial-gun part/material IDs, and nine recipe ingredient strings.
- Recipe ingredient grammar: blank or `minecraft:air` means empty; `#namespace:tag` means a tag; `namespace:item` means an item.

- [ ] **Step 1: Register the Forge common config at `config/taczintetra.toml`.**
- [ ] **Step 2: Encode the requested wood 9mm pistol/standard-magazine starter and plank-stick-slab recipe as defaults.**
- [ ] **Step 3: Validate IDs and ingredient grammar, logging a safe fallback for malformed values.**
- [ ] **Step 4: Update dev automation’s watched path to the new JSON location.**
- [ ] **Step 5: Run config tests and a client compile.**

### Task 4: Fix modular-gun reticle routing

**Files:**
- Create or modify: `src/main/java/com/pycoder/taczintetra/client/ModularGunReticleHandler.java`
- Modify: `src/main/resources/assets/tacz/custom/taczintetra/data/taczintetra/data/guns/modular_gun_data.json`
- Test: `src/test/java/com/pycoder/taczintetra/client/ModularGunReticlePolicyTest.java`

**Interfaces:**
- Produces: a client-only policy that identifies modular guns and selects the configured TaCZ dot crosshair path without rendering a custom ruler.

- [ ] **Step 1: Reproduce the current renderer event path with a failing policy test.**
- [ ] **Step 2: Subscribe to the TaCZ crosshair event using the exact 1.1.8-hotfix event result contract.**
- [ ] **Step 3: Cancel/replace only the modular-gun ruler path and leave native TaCZ crosshair behavior unchanged.**
- [ ] **Step 4: Verify no client classes are loaded on the dedicated server.**
- [ ] **Step 5: Run the focused test and launch a single isolated client for visual verification.**

### Task 5: Disable native TaCZ items, workbenches, recipes, and optional JEI entries

**Files:**
- Create: `src/main/java/com/pycoder/taczintetra/compat/NativeTaczRestrictionService.java`
- Create: `src/main/java/com/pycoder/taczintetra/compat/NativeTaczRecipeFilter.java`
- Create: `src/main/java/com/pycoder/taczintetra/compat/jei/TaczJeiPlugin.java` when the optional API is available.
- Modify: `build.gradle.kts` only if an available compatible JEI compile dependency is needed.
- Create: `src/test/java/com/pycoder/taczintetra/compat/NativeTaczRestrictionTest.java`

**Interfaces:**
- Produces: `isNativeTaczItem(ResourceLocation)`, `isNativeTaczWorkbench(ResourceLocation)`, and `shouldHideFromJei(...)` policy methods.

- [ ] **Step 1: Enumerate actual 1.1.8-hotfix registry IDs from the local jar rather than guessing.**
- [ ] **Step 2: Block native TaCZ crafting recipes at recipe-update time while preserving unrelated recipes and later KubeJS overrides.**
- [ ] **Step 3: Prevent native workbench use when the config rule is enabled.**
- [ ] **Step 4: Add optional JEI hiding; if JEI is absent, do not load JEI classes.**
- [ ] **Step 5: Test native IDs, unrelated IDs, config-disabled behavior, and no-JEI class loading.**

### Task 6: Update launch scripts, task records, and resources

**Files:**
- Modify: `launch-clients.ps1`
- Modify: `launch-profile.json`
- Modify: `launch-profile-workbench.json`
- Modify: `task.md`
- Modify: `README.md` only for public config names if required.

- [ ] **Step 1: Replace stale launch-property prefixes with `tit` while preserving PID-scoped shutdown.**
- [ ] **Step 2: Remove generated old config directories from the dev profile only after migration is tested.**
- [ ] **Step 3: Record completed items and known limitations in `task.md`.**
- [ ] **Step 4: Keep public documentation free of local machine paths.**

### Task 7: Build, isolated-window test, and full audit

- [ ] **Step 1: Run `gradlew.bat test --no-daemon`.**
- [ ] **Step 2: Run `gradlew.bat build --no-daemon`.**
- [ ] **Step 3: Launch exactly one test client with the `tit` profile and record its PID.**
- [ ] **Step 4: Verify config creation, reticle appearance, native recipe/JEI restrictions, starter recipe, and KubeJS-compatible recipe replacement.**
- [ ] **Step 5: Stop only the recorded target PID and verify no target child process remains.**
- [x] **Step 6: Run `rg` audit excluding `build/**`, `run/**`, and backups for stale paths, TODOs, debug-only leakage, unsafe constants, and duplicated policy logic.**
- [ ] **Step 7: Update `task.md` and mark the goal complete only when all evidence is present.**
