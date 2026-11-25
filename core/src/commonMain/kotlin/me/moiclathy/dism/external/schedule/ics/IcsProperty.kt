package me.moiclathy.dism.external.schedule.ics

/**
 * 一个已经完成基础拆分的 iCalendar Property。
 *
 * 例如：
 *
 * DTSTART;VALUE=DATE:20260916
 *
 * name      = DTSTART
 * parameters = VALUE=DATE
 * value     = 20260916
 */
data class IcsProperty(
    val name: String,
    val parameters: Map<String, String> = emptyMap(),
    val value: String,
) {

    init {
        require(name.isNotBlank()) {
            "ICS property name must not be blank."
        }

        require(value.isNotBlank()) {
            "ICS property value must not be blank."
        }

        require(parameters.keys.all { it.isNotBlank() }) {
            "ICS parameter names must not be blank."
        }
    }

    operator fun get(
        parameter: String,
    ): String? =
        parameters[parameter.uppercase()]

    companion object {

        /**
         * 将一行已经完成 unfold 的 ICS Property 解析为结构。
         *
         * 注意：
         * 这里只处理 Property 结构，
         * 不负责整个 ICS 文档。
         */
        fun parse(
            line: String,
        ): IcsProperty {

            require(line.isNotBlank()) {
                "ICS property line must not be blank."
            }

            val separatorIndex =
                line.indexOf(':')

            require(separatorIndex > 0) {
                "ICS property must contain ':'."
            }

            val left =
                line.substring(
                    0,
                    separatorIndex,
                )

            val value =
                line.substring(
                    separatorIndex + 1,
                )

            require(value.isNotBlank()) {
                "ICS property value must not be blank."
            }

            val parts =
                left.split(';')

            val name =
                parts
                    .first()
                    .uppercase()

            require(name.isNotBlank()) {
                "ICS property name must not be blank."
            }

            val parameters =
                buildMap {

                    parts
                        .drop(1)
                        .forEach { parameter ->

                            val parameterSeparator =
                                parameter.indexOf('=')

                            if (parameterSeparator <= 0) {
                                return@forEach
                            }

                            val key =
                                parameter
                                    .substring(
                                        0,
                                        parameterSeparator,
                                    )
                                    .uppercase()

                            val parameterValue =
                                parameter
                                    .substring(
                                        parameterSeparator + 1,
                                    )

                            put(
                                key,
                                parameterValue,
                            )
                        }
                }

            return IcsProperty(
                name =
                    name,

                parameters =
                    parameters,

                value =
                    value,
            )
        }
    }
}
