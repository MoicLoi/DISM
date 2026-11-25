package me.moiclathy.dism.provider

import me.moiclathy.dism.api.DismPublicApi

/**
 * Capabilities advertised by an external provider.
 */
@DismPublicApi
enum class DismProviderCapability {

    /**
     * Provider can read external information.
     */
    READ,

    /**
     * Provider can write information back to the external system.
     */
    WRITE,

    /**
     * Provider can receive incremental changes.
     */
    STREAM,

    /**
     * Provider can synchronize an external source.
     */
    SYNC,

    /**
     * Provider can interpret natural-language input.
     */
    NATURAL_LANGUAGE,

    /**
     * Provider can produce structured semantic information.
     */
    SEMANTIC_EXTRACTION,
}
