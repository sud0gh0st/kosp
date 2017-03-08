package de.randombyte.kosp.config.serializers.texttemplate

import com.google.common.reflect.TypeToken
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.TextTemplate
import org.spongepowered.api.text.format.TextFormat

/**
 * Simplifies the config appearance of [TextTemplate]s. Arguments are enclosed in curly brackets.
 * Restrictions:    - Every argument is required, there are no optional ones
 *                  - Arguments must not contain spaces
 *                  - Formatting codes have to be used to apply formatting to the text
 * Example: '&cThe number is {number}.'
 *
 * To apply a format to the argument do this: '{(&2&o)number}'. Codes must be directly enclosed in
 * round brackets. More than one code can be used.
 *
 * Note: The meaning of arguments and parameters is somehow switched in [TextTemplate]s,
 * I'll keep this error in my comments to "avoid confusion".
 *
 * Comments set by `@Setting(comment = "...")` get processed when prefixed with `%`.
 * The format is described at [SimpleTextTemplateSerializer.parseExistingComment].
 *
 * TextActions can be written like so:
 *      - suggestCommand: '[CLICK](&/cmd)'
 *      - runCommand: '[CLICK](/cmd)'
 *      - openUrl: '[CLICK](https://www.google.de)'
 *
 * All these things can be combined freely.
 */
object SimpleTextTemplateTypeSerializer : TypeSerializer<TextTemplate> {
    internal const val COMMENT_NEEDS_PROCESSING_PREFIX = "%"

    override fun serialize(type: TypeToken<*>, textTemplate: TextTemplate, node: ConfigurationNode) = SimpleTextTemplateSerializer.serialize(textTemplate, node)
    override fun deserialize(type: TypeToken<*>, node: ConfigurationNode) = SimpleTextTemplateDeserializer.deserialize(node)
}

fun Text.getLastFormat(): TextFormat = if (children.isEmpty()) format else children.last().getLastFormat()