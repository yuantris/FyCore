package com.core.libraries.common.helper.valid

import android.widget.TextView
import java.util.regex.Pattern

/**
 *         Validations.add(
 *             Validations.NotEmpty("", "Name cannot be empty"),
 *         ).start {
 *             it?.let {
 *                 toast(it)
 *             } ?: run {
 *                 toast("123")
 *             }
 *         }
 */
class Validations(private vararg val rules: Rule) {

    fun start(validationCallback: (String?) -> Unit) {
        rules.firstOrNull { !it.validate() }?.let {
            validationCallback(it.errorMessage)
            return
        }
        validationCallback(null)
    }

    fun start(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        rules.firstOrNull { !it.validate() }?.let {
            onFailure(it.errorMessage)
            return
        }
        onSuccess()
    }

    interface Rule {
        fun validate(): Boolean
        val errorMessage: String
    }

    // Rules
    class NotEmpty(private val param: CharSequence?, override val errorMessage: String) : Rule {
        constructor(param: TextView, errorMessage: String) : this(param.text, errorMessage)

        override fun validate(): Boolean = !param.isNullOrBlank()
    }

    class StringMaxLength(private val param: CharSequence?, private val maxLength: Int, override val errorMessage: String) :
        Rule {
        constructor(param: TextView, maxLength: Int, errorMessage: String) : this(param.text, maxLength, errorMessage)

        override fun validate(): Boolean = !param.isNullOrBlank() && param.length <= maxLength
    }

    class StringMinLength(private val param: CharSequence?, private val minLength: Int, override val errorMessage: String) :
        Rule {
        constructor(param: TextView, minLength: Int, errorMessage: String) : this(param.text, minLength, errorMessage)

        override fun validate(): Boolean = !param.isNullOrBlank() && param.length >= minLength
    }

    class Same(private val param1: CharSequence?, private val param2: CharSequence?, override val errorMessage: String) :
        Rule {
        constructor(param1: TextView, param2: TextView, errorMessage: String) : this(param1.text, param2.text, errorMessage)

        override fun validate(): Boolean = param1 == param2
    }

    class Mobile(private val param: CharSequence?, override val errorMessage: String) : Rule {
        override fun validate(): Boolean {
            val regexMobile = "^((17[0-9])|(14[0-9])|(13[0-9])|(15[^4,\\D])|(18[0,5-9]))\\d{8}$"
            return !param.isNullOrBlank() && Pattern.matches(regexMobile, param)
        }
    }

    class Size(private val size: Int, private val min: Int, private val max: Int = Int.MAX_VALUE, override val errorMessage: String) :
        Rule {
        override fun validate(): Boolean = size in min..max
    }

    class PositiveNumber(private val num: Double, override val errorMessage: String) : Rule {
        override fun validate(): Boolean = num > 0
    }

    companion object {
        fun add(vararg rules: Rule): Validations = Validations(*rules)
    }
}
