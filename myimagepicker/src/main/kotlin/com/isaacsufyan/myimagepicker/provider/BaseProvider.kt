package com.isaacsufyan.myimagepicker.provider

import android.content.ContextWrapper
import android.os.Bundle
import android.os.Environment
import com.isaacsufyan.myimagepicker.ImagePickerActivity
import java.io.File


abstract class BaseProvider(protected val activity: ImagePickerActivity) :
    ContextWrapper(activity) {

    fun getFileDir(path: String?): File {
        return if (path != null) File(path)
        else getExternalFilesDir(Environment.DIRECTORY_DCIM) ?: activity.filesDir
    }

    protected fun setError(error: String) {
        onFailure()
        activity.setError(error)
    }

    protected fun setError(errorRes: Int) {
        setError(getString(errorRes))
    }

    protected fun setResultCancel() {
        onFailure()
        activity.setResultCancel()
    }

    protected open fun onFailure() {
    }

    open fun onSaveInstanceState(outState: Bundle) {
    }

    open fun onRestoreInstanceState(savedInstanceState: Bundle?) {
    }
}
