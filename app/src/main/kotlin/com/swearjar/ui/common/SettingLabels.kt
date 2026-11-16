package com.swearjar.ui.common

import androidx.annotation.StringRes
import com.swearjar.R
import com.swearjar.domain.model.GoalBehaviour
import com.swearjar.domain.model.LoggingPolicy
import com.swearjar.domain.model.PricingMode
import com.swearjar.domain.model.Role

/** A setting choice as shown to people: a short title and one line of explanation. */
data class OptionText(@StringRes val title: Int, @StringRes val description: Int)

val LoggingPolicy.text: OptionText
    get() = when (this) {
        LoggingPolicy.ANYONE -> OptionText(R.string.logging_anyone, R.string.logging_anyone_desc)
        LoggingPolicy.ANYONE_WITH_DISPUTE -> OptionText(R.string.logging_dispute, R.string.logging_dispute_desc)
        LoggingPolicy.SELF_ONLY -> OptionText(R.string.logging_self, R.string.logging_self_desc)
        LoggingPolicy.ADMINS_ONLY -> OptionText(R.string.logging_admins, R.string.logging_admins_desc)
    }

val PricingMode.text: OptionText
    get() = when (this) {
        PricingMode.FIXED -> OptionText(R.string.pricing_fixed, R.string.pricing_fixed_desc)
        PricingMode.FIXED_WITH_OVERRIDE -> OptionText(R.string.pricing_override, R.string.pricing_override_desc)
        PricingMode.ESCALATING -> OptionText(R.string.pricing_escalating, R.string.pricing_escalating_desc)
    }

val GoalBehaviour.text: OptionText
    get() = when (this) {
        GoalBehaviour.RESET -> OptionText(R.string.goal_reset, R.string.goal_reset_desc)
        GoalBehaviour.KEEP_FILLING -> OptionText(R.string.goal_keep, R.string.goal_keep_desc)
        GoalBehaviour.CLOSE -> OptionText(R.string.goal_close, R.string.goal_close_desc)
    }

val Role.label: Int
    @StringRes get() = when (this) {
        Role.OWNER -> R.string.role_owner
        Role.ADMIN -> R.string.role_admin
        Role.MEMBER -> R.string.role_member
    }
