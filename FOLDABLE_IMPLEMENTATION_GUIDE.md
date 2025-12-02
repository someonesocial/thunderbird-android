# Foldable Device Support - Complete Implementation Guide

## Summary

This implementation adds foldable device support to Thunderbird for Android. Users can now select "When device is unfolded" in the split-screen settings, and the app will automatically switch between single-pane and split-view layouts based on the device's fold state.

## What Was Implemented

### 1. Core Components

#### FoldableStateObserver
- **Location**: `legacy/ui/legacy/src/main/java/com/fsck/k9/ui/foldable/FoldableStateObserver.kt`
- **Purpose**: Monitors device fold state using Jetpack WindowManager
- **Features**:
  - Lifecycle-aware observation
  - 300ms debouncing to prevent layout thrashing
  - Exposes `StateFlow<FoldableState>` for reactive updates
  - Fallback handling for non-foldable devices

#### FoldableState Enum
```kotlin
enum class FoldableState {
    FOLDED,      // Device is folded
    UNFOLDED,    // Device is unfolded or half-opened
    UNKNOWN,     // Not a foldable or state cannot be determined
}
```

### 2. Modified Files

#### Preference System
- **File**: `core/preference/api/src/commonMain/kotlin/net/thunderbird/core/preference/GeneralSettings.kt`
- **Change**: Added `WHEN_UNFOLDED` to `SplitViewMode` enum

#### MainActivity
- **File**: `legacy/ui/legacy/src/main/java/com/fsck/k9/activity/MainActivity.kt`
- **Changes**:
  - Injected `FoldableStateObserver` via Koin
  - Added lifecycle registration and state observation
  - Extended `useSplitView()` to check foldable state
  - Added handlers for fold/unfold events

#### Dependency Injection
- **File**: `legacy/ui/legacy/src/main/java/com/fsck/k9/ui/KoinModule.kt`
- **Change**: Added `FoldableStateObserver` factory

#### UI Resources
- **Files**: `strings.xml`, `arrays_general_settings_strings.xml`, `arrays_general_settings_values.xml`
- **Changes**: Added "When device is unfolded" option

#### Build Configuration
- **Files**: `gradle/libs.versions.toml`, `legacy/ui/legacy/build.gradle.kts`
- **Changes**: Added `androidx.window:window:1.3.0` dependency

### 3. Documentation

- **Developer Guide**: `docs/developer/foldable-device-support.md`
- **Package README**: `legacy/ui/legacy/src/main/java/com/fsck/k9/ui/foldable/README.md`
- **Summary Link**: Added to `docs/SUMMARY.md`

### 4. Tests

- **Unit Tests**: `legacy/ui/legacy/src/test/java/com/fsck/k9/ui/foldable/FoldableStateObserverTest.kt`
- **Coverage**: State mapping, debouncing, lifecycle behavior

## How to Build and Test

### 1. Sync Dependencies

```bash
./gradlew --refresh-dependencies
```

### 2. Build the Project

```bash
./gradlew :legacy:ui:legacy:assembleDebug
```

### 3. Run Unit Tests

```bash
./gradlew :legacy:ui:legacy:testDebugUnitTest --tests "FoldableStateObserverTest"
```

### 4. Manual Testing on Emulator

#### Setup Foldable Emulator
1. Open Android Studio → Tools → Device Manager
2. Create Virtual Device → Select "7.6\" Fold-in with outer display"
3. Choose API Level 33 or higher
4. Finish and launch emulator

#### Test Scenarios
1. Install and launch Thunderbird
2. Go to **Settings** → **Display** → **Show split-screen**
3. Select **"When device is unfolded"**
4. Use emulator fold/unfold controls to test:
   - Unfolded → Split-view appears (list + detail)
   - Folded → Single-pane view
   - State preservation when switching
   - Orientation changes
   - Multi-window mode

## Architecture

### State Flow Diagram

```
Device State (Physical)
    ↓
WindowInfoTracker (Android System)
    ↓
FoldableStateObserver
    ├─ Processes WindowLayoutInfo
    ├─ Applies 300ms debounce
    └─ Emits FoldableState
        ↓
MainActivity (via StateFlow)
    ├─ Checks if WHEN_UNFOLDED mode active
    ├─ Compares with current layout
    └─ Calls recreate() if change needed
        ↓
New Layout Loaded
    ├─ Split-view (unfolded)
    └─ Single-pane (folded)
```

### Module Dependencies

```
app-thunderbird/
    └─ legacy:ui:legacy (MainActivity)
        ├─ uses androidx.window (NEW)
        ├─ ui/foldable/FoldableStateObserver (NEW)
        └─ core:preference:api
            └─ SplitViewMode.WHEN_UNFOLDED (NEW)
```

## Key Design Decisions

### 1. Why Activity Recreate?
- **Reason**: Simplest implementation that leverages Android's existing state restoration
- **Trade-off**: Brief flash during transition
- **Future**: Can be improved with dynamic layout swapping

### 2. Why 300ms Debounce?
- **Reason**: Fold/unfold animations typically take 200-400ms
- **Benefit**: Prevents multiple recreate() calls during animation
- **Result**: Smoother user experience

### 3. Why StateFlow?
- **Reason**: Reactive, lifecycle-aware, single source of truth
- **Benefit**: Automatically handles lifecycle, no memory leaks
- **Integration**: Works seamlessly with Kotlin Coroutines

## Backward Compatibility

✅ **100% Backward Compatible**
- No database migrations required
- No breaking API changes
- Existing settings preserved
- Non-foldable devices: New option has no negative effect
- Optional opt-in feature

## Known Limitations

1. **Brief flash during layout switch** - Uses `recreate()` for simplicity
2. **Tablets without FoldingFeature** - Detected as UNKNOWN, user should choose ALWAYS
3. **No hinge-aware positioning** - Future improvement to position content around hinge

## Next Steps

### Before Creating PR

- [ ] Run full test suite: `./gradlew test`
- [ ] Run code quality checks: `./gradlew detekt spotlessCheck lint`
- [ ] Test on foldable emulator
- [ ] Take screenshots for PR
- [ ] Update CHANGELOG (if applicable)

### PR Creation

1. Create feature branch: `git checkout -b feature/foldable-support`
2. Commit changes with conventional commit message
3. Push and create PR on GitHub
4. Use implementation summary as PR description
5. Link to documentation in PR

### Recommended PR Title

```
feat: Add foldable device support with auto split-screen mode
```

### Recommended PR Description Template

```markdown
## Summary
Adds automatic split-screen layout switching for foldable devices.

## Changes
- Added WHEN_UNFOLDED option to split-screen settings
- Implemented FoldableStateObserver using Jetpack WindowManager
- Integrated foldable detection into MainActivity
- Added documentation and unit tests

## Testing
- [x] Unit tests pass
- [ ] Manual testing on foldable emulator
- [ ] Code quality checks pass

## Screenshots
[Add screenshots of folded/unfolded states]

## Documentation
See `docs/developer/foldable-device-support.md`

## Backward Compatibility
Fully backward compatible. Opt-in feature, no breaking changes.
```

## Troubleshooting

### Build Errors

**Problem**: "Unresolved reference 'layout'"
**Solution**: Run `./gradlew --refresh-dependencies` to sync androidx.window

**Problem**: "Cannot resolve symbol 'FoldingFeature'"
**Solution**: Ensure `implementation(libs.androidx.window)` is in build.gradle.kts

### Runtime Issues

**Problem**: Layout doesn't switch on fold/unfold
**Solution**: 
1. Check setting is on "When device is unfolded"
2. Verify emulator has foldable features
3. Check logcat for FoldableStateObserver messages

**Problem**: App crashes on fold
**Solution**:
1. Check stack trace in logcat
2. Verify Koin module is configured
3. Ensure lifecycle observer is registered

## References

### External Documentation
- [Jetpack WindowManager](https://developer.android.com/jetpack/androidx/releases/window)
- [Foldable Device Guidelines](https://developer.android.com/guide/topics/large-screens/learn-about-foldables)
- [WindowInfoTracker API](https://developer.android.com/reference/androidx/window/layout/WindowInfoTracker)

### Project Documentation
- [Developer Guide](docs/developer/foldable-device-support.md)
- [Package README](legacy/ui/legacy/src/main/java/com/fsck/k9/ui/foldable/README.md)
- [Contributing Guide](docs/CONTRIBUTING.md)

## Credits

Implementation based on:
- Jetpack WindowManager library
- Android foldable device best practices
- Thunderbird Android architecture guidelines

---

**Status**: ✅ Implementation Complete  
**Ready for**: Manual Testing → PR Creation  
**Version**: 1.0  
**Date**: 2025-12-02

