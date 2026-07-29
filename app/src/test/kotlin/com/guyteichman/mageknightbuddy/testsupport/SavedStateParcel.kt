package com.guyteichman.mageknightbuddy.testsupport

import android.os.Bundle
import android.os.Parcel
import androidx.lifecycle.SavedStateHandle

/**
 * Marshals [handle]'s saved state through a real Android [Parcel] and reads it back - the exact
 * thing the framework does in `onSaveInstanceState` when the app is backgrounded.
 *
 * This is the boundary plain-JVM `SavedStateHandle()` tests never cross: an in-memory handle holds
 * any object, so a value Android can't actually parcel (e.g. a `data object` that's neither
 * Parcelable nor Serializable) sails through every ordinary unit test and only crashes on a real
 * device (issue #212). Calling this in a Robolectric test forces that crash into the test instead.
 *
 * Must be called from a Robolectric-run test (`@RunWith(RobolectricTestRunner::class)`) so the
 * android.os types have real implementations.
 */
fun parcelRoundTrip(handle: SavedStateHandle): Bundle {
    // savedStateProvider().saveState() is how the framework snapshots a handle into a Bundle.
    val saved: Bundle = handle.savedStateProvider().saveState()
    val parcel = Parcel.obtain()
    try {
        // writeBundle throws if any value inside isn't a type Parcel knows how to write.
        parcel.writeBundle(saved)
        parcel.setDataPosition(0)
        return parcel.readBundle(SavedStateHandle::class.java.classLoader)!!
    } finally {
        parcel.recycle()
    }
}
