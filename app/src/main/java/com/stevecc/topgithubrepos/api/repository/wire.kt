package com.stevecc.topgithubrepos.api.repository

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

// https://docs.github.com/en/rest/reference/repos#list-repository-contributors
@JsonClass(generateAdapter = true)
@Parcelize
data class Contributor(
    val id: Int,
    val login: String
) : Parcelable
