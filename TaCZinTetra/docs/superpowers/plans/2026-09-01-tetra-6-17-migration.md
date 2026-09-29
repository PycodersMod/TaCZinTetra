# Tetra 6.17.0 Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task with review checkpoints.

**Goal:** Upgrade the development and runtime dependency from Tetra 6.9.0 to 6.17.0 while preserving TaCZ 1.1.8-hotfix and verifying the impact on completed TiT work.

**Architecture:** Keep TiT independent from Tetra internals wherever possible. Resolve Tetra through `fg.deobf`, prepare the mapped development artifact for `run/mods`, and retain the existing refmap remapping path. Compare the old and new JAR/API/schema surfaces before adapting code or data.

**Tech Stack:** Minecraft 1.20.1, Forge 47.4.16, ForgeGradle 6.0.54, Gradle 8.5, Java 17, official mappings, Tetra 6.17.0, TaCZ 1.1.8-hotfix, Mutil 6.2.0.

**Spec:** `TaCZ_in_Tetra_完整项目规范_Codex版.md` and the confirmed migration design in the preceding conversation.

## Global Constraints

- Only Tetra changes from `6.9.0` to `6.17.0`; TaCZ remains `1.20.1-1.1.8-hotfix`.
- Forge remains `47.4.16`; Minecraft remains `1.20.1`.
- Public documentation must not receive local paths, usernames, machine details, or internal logs.
- Development runs must use mapped Tetra/Mutil/TaCZ artifacts and absolute refmap mapping configuration.
- Existing completed logic must be compared and re-tested; static checks do not replace client/server verification.
- Preserve recoverable copies of existing `run/mods` artifacts before replacement.

### Task 1: Resolve and inventory Tetra 6.17.0

**Files:**
- Read: `build.gradle.kts`, `gradle.properties`, `task.md`
- Runtime artifacts: `run/mods/tetra-1.20.1-6.9.0.jar`, mapped Gradle cache

- [ ] Update the dependency version in a single controlled change.
- [ ] Run Gradle dependency resolution and record the exact 6.17.0 artifact paths and hashes in the internal task log.
- [ ] Inspect the new JAR metadata, refmaps, public classes, and bundled Tetra JSON resources.
- [ ] Preserve the old runtime artifact as a recoverable backup.

### Task 2: Compare completed TiT surfaces

**Files:**
- Read: `src/main/java/com/pycoder/taczintetra/**`
- Read: `src/main/resources/data/tetra/**`
- Read: `src/test/java/**`
- Internal report: `docs/Tetra_6.9_to_6.17_兼容性核对_20260901.md`

- [ ] Compare referenced Tetra classes/methods/fields with 6.17.0 using bytecode/API inspection.
- [ ] Compare module, material, repair, and schematic JSON schemas against 6.17.0 samples.
- [ ] Check known completed areas: six module definitions, material item mapping, state policy tests, reload calculators, resolver tests, and Holo GUI material preview data.
- [ ] Record each item as unaffected, source-compatible, schema-compatible, or requiring adaptation with evidence.

### Task 3: Adapt build and development runtime

**Files:**
- Modify: `build.gradle.kts`
- Modify: `run/mods/tetra-1.20.1-6.17.0.jar` via `prepareDevMods`
- Internal log: `task.md`

- [ ] Update the Tetra version in `implementation(fg.deobf(...))`.
- [ ] Update `prepareDevMods` to copy the 6.17.0 mapped artifact while leaving Mutil and TaCZ versions unchanged.
- [ ] Keep client/server/data refmap remapping properties intact and verify the resolved output path.
- [ ] Run `test` and `build`; stop and diagnose any compile or dependency conflict before further changes.

### Task 4: Run compatibility verification

**Files:**
- Read: `run/logs/**`, `run/crash-reports/**`
- Update: `task.md`

- [ ] Run `runClient` once with no external `JAVA_TOOL_OPTIONS` and confirm Mixin/refmap initialization.
- [ ] Enter or create a world and check Tetra registration, Holo/material preview, and TiT item registration.
- [ ] Run `runServer --no-daemon`; accept only the normal EULA stop if no server-side exception occurs.
- [ ] Run `runData` and distinguish Tetra's known client-side datagen limitation from migration regressions.
- [ ] Scan logs for `InvalidInjectionException`, `NoSuchMethodError`, `NoClassDefFoundError`, registry failures, and new model/data warnings.

### Task 5: Final impact report

**Files:**
- Internal report: `docs/Tetra_6.9_to_6.17_兼容性核对_20260901.md`
- Internal log: `task.md`

- [ ] Summarize changed files, artifact versions, verification commands, and exact outcomes.
- [ ] List completed content that remains valid and any completed item invalidated by 6.17.0.
- [ ] Do not mark the migration stable unless build, client, and server evidence support it.
