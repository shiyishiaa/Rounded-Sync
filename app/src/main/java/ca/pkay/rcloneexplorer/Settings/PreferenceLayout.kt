package ca.pkay.rcloneexplorer.Settings

import androidx.preference.Preference
import androidx.preference.PreferenceGroup

/** Removes the empty leading icon column from preferences that have no icon. */
internal fun Preference.removeUnusedIconSpacingRecursively() {
    if (icon == null) {
        isIconSpaceReserved = false
    }

    if (this is PreferenceGroup) {
        for (index in 0 until preferenceCount) {
            getPreference(index).removeUnusedIconSpacingRecursively()
        }
    }
}
