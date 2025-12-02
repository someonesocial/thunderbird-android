# Foldable Support

This package provides foldable device detection for Thunderbird Android using Jetpack WindowManager.

## FoldableStateObserver

Lifecycle-aware observer that tracks fold/unfold events on devices like Samsung Galaxy Fold and Google Pixel Fold.

### Usage

```kotlin
// Inject via Koin
private val foldableStateObserver: FoldableStateObserver by inject { parametersOf(this) }

// Register lifecycle observer
lifecycle.addObserver(foldableStateObserver)

// Collect state changes
lifecycleScope.launch {
    foldableStateObserver.foldableState.collect { state ->
        when (state) {
            FoldableState.UNFOLDED -> // Handle unfolded
            FoldableState.FOLDED -> // Handle folded
            FoldableState.UNKNOWN -> // Handle non-foldable
        }
    }
}
```

### FoldableState Values

- `UNFOLDED`: Device is fully opened or half-open (laptop mode)
- `FOLDED`: Device is folded (small screen)
- `UNKNOWN`: Not a foldable device or state cannot be determined

### Features

- **300ms debouncing**: Prevents layout thrashing during fold animations
- **Lifecycle-aware**: Automatically starts/stops observation
- **StateFlow-based**: Reactive state updates

## See Also

- [Foldable Device Support Documentation](../../../../../../docs/developer/foldable-device-support.md)
- [Jetpack WindowManager](https://developer.android.com/jetpack/androidx/releases/window)


