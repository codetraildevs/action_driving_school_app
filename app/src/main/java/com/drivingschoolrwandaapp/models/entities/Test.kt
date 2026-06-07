package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName

data class Test(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("title") var title: String? = null,
    @SerializedName("description") var description: String? = null,
    @SerializedName("testNumber") var testNumber: Int = 0,
    @SerializedName("imageUrl") var imageUrl: String? = null,
    @SerializedName("totalMarks") var totalMarks: Int = 0,
    @SerializedName("passMarks") var passMarks: Int = 0,
    @SerializedName("duration") var duration: Int = 0,
    @SerializedName("isFree") var isFree: Boolean = false,
    @SerializedName("createdAt") var createdAt: String? = null,
    @SerializedName("updatedAt") var updatedAt: String? = null,
    @SerializedName("subscriptionId") var subscriptionId: Int = 0,
    @SerializedName("subscription") var subscription: SubscriptionInfo? = null,
    @SerializedName("_count") var count: TestCount? = null,
    @SerializedName("questions") var questions: List<TestQuestion>? = null,
    @SerializedName("testTranslations") var testTranslations: List<TestTranslation>? = null
) {

    data class SubscriptionInfo(
        @SerializedName("planName") var planName: String? = null
    )

    data class TestCount(
        @SerializedName("testQuestions") var testQuestions: Int = 0
    )
}
