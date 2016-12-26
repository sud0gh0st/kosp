package de.randombyte.kosp.config

import com.google.common.reflect.TypeToken
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer
import org.spongepowered.api.text.LiteralText
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.TextRepresentable
import org.spongepowered.api.text.TextTemplate
import org.spongepowered.api.text.TextTemplate.*
import org.spongepowered.api.text.format.TextFormat
import org.spongepowered.api.text.serializer.TextSerializers

/**
 * Simplifies the config appearance of [TextTemplate]s. Arguments are enclosed in curly brackets.
 * Restrictions:    - Every argument is required, there are no optional ones
 *                  - Formatting codes have to be used to apply formatting to the text
 * Example: '&cThe number is {number}.'
 */
object SimpleTextTemplateSerializer : TypeSerializer<TextTemplate> {
    override fun serialize(type: TypeToken<*>, textTemplate: TextTemplate, node: ConfigurationNode) {
        textTemplate.firstOptionalArgument()?.apply {
            throw ObjectMappingException("TextTemplate '${node.key}': Argument '$key' is optional!")
        }
        textTemplate.firstNonSingleWordArgument()?.apply {
            throw ObjectMappingException("TextTemplate '${node.key}': Argument '$key' contains a space!")
        }

        val pseudoArguments = textTemplate.arguments.map { it.key to it.key.toFullArgumentName() }.toMap()
        val text = textTemplate.apply(pseudoArguments).build()
        node.value = text.serializeToString()
    }

    override fun deserialize(type: TypeToken<*>, node: ConfigurationNode): TextTemplate =
            TextTemplate.of(*getElements(node.string).toTypedArray())

    private fun getElements(string: String): List<Any> {
        val elements = mutableListOf<TextRepresentable>()
        val arguments = getArguments(string)

        var remainingString = string // gets cut down while processing
        arguments.forEach { argument ->
            val fullArgumentName = argument.name.toFullArgumentName()
            val argumentIndex = remainingString.indexOf(fullArgumentName)

            if (argumentIndex >= 1) { // check if there is some text before the argument
                val front = remainingString.take(argumentIndex)
                val frontText = front.deserializeToText()
                elements.add(frontText)
            }

            // format argument like the last element
            val lastTextFormat = elements.lastOrNull()?.toText()?.getLastFormat()
            val formattedArgument = if (lastTextFormat != null) {
                argument.toBuilder().format(lastTextFormat).build()
            } else argument

            elements.add(formattedArgument)

            remainingString = cutFormattedText(remainingString, argumentIndex + fullArgumentName.length)
        }

        // check if there is some text after the last argument
        if (remainingString.isNotEmpty()) elements.add(remainingString.deserializeToText().flatten())

        return elements
    }

    // matches arguments like '{number}'
    private val ARGUMENTS_REGEX = "\\$DEFAULT_OPEN_ARG(\\w+)\\$DEFAULT_CLOSE_ARG".toRegex()

    /**
     * @return a List of [Arg]uments extracted from [string], which might look like '&cThe numer is {number}.'
     */
    private fun getArguments(string: String): List<Arg> = ARGUMENTS_REGEX.findAll(string).map { matchResult ->
        // getting second group, enclosed by round bracket in regex pattern, so without '{}'
        val group = matchResult.groups[1]!!
        arg(group.value).build()
    }.toList()

    /**
     * Cuts the [formattedText] at [index] but keeps the formatting by keeping the formatters(e.g. '&c')
     * in the text.
     */
    private fun cutFormattedText(formattedText: String, index: Int): String {
        val formattersBeforeCut = getFormatters(formattedText, 0..index)
        val remainingText = formattedText.substring(index)
        // reinsert the formatters
        return formattersBeforeCut.joinToString(separator = "") + remainingText
    }

    // matches color and formatting codes like '&c'; full list: 0123456789abcdefklmnor
    private val FORMATTING_CODES_REGEX = "&[\\da-fk-or]".toRegex()

    /**
     * @return a list of the formatters and where there are located at, which are in [range]
     */
    private fun getFormatters(formattedText: String, range: IntRange): List<String> = FORMATTING_CODES_REGEX
            .findAll(formattedText)
            .filter { range.contains(it.range) }
            .map { it.groups.first()!!.value }
            .toList()

    private fun TextTemplate.firstOptionalArgument() = arguments.entries.firstOrNull { it.value.isOptional }
    private fun TextTemplate.firstNonSingleWordArgument() = arguments.entries.firstOrNull { it.key.contains(" ") }

    private fun String.toFullArgumentName() = DEFAULT_OPEN_ARG + this + DEFAULT_CLOSE_ARG

    private fun String.deserializeToText() = TextSerializers.FORMATTING_CODE.deserialize(this)
    private fun Text.serializeToString() = TextSerializers.FORMATTING_CODE.serialize(this)

    /**
     * @return true if the whole range [other] is in this range, false if not
     */
    private fun IntRange.contains(other: IntRange) = other.all { contains(it) }

    private fun Text.getLastFormat(): TextFormat = children.lastOrNull()?.getLastFormat() ?: format
    private fun Arg.toBuilder(): Arg.Builder = arg(name)

    /**
     * If there is no content and only one child, higher it in hierarchy. Should only be used in this context.
     */
    private fun Text.flatten(): Text = if (this is LiteralText && content.isEmpty() && children.size == 1) children.first() else this
}