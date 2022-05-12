package com.isaacsufyan.myimagepicker.listener

internal interface ResultListener<T> {

    fun onResult(t: T?)
}
