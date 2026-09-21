package com.knittingai.swatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenAiImageProviderTest {
    @Test void buildsAzureEndpointFromAzureEndpointVariable() {
        URI endpoint = OpenAiImageProvider.endpointFrom(Map.of(
            "AZURE_OPENAI_ENDPOINT", "https://my-resource.openai.azure.com/"));
        assertEquals(URI.create("https://my-resource.openai.azure.com/openai/v1/images/generations"), endpoint);
    }

    @Test void prefersExplicitOpenAiBaseUrlOverAzure() {
        URI endpoint = OpenAiImageProvider.endpointFrom(Map.of(
            "OPENAI_BASE_URL", "https://api.openai.com/v1",
            "AZURE_OPENAI_ENDPOINT", "https://my-resource.openai.azure.com"));
        assertEquals(URI.create("https://api.openai.com/v1/images/generations"), endpoint);
    }

    @Test void fallsBackToPublicOpenAiEndpoint() {
        assertEquals(URI.create("https://api.openai.com/v1/images/generations"),
            OpenAiImageProvider.endpointFrom(Map.of()));
    }

    @Test void addsAzureV1SurfaceWhenBaseUrlIsBareAzureHost() {
        URI endpoint = OpenAiImageProvider.endpointFrom(Map.of(
            "OPENAI_BASE_URL", "https://my-resource.openai.azure.com"));
        assertEquals(URI.create("https://my-resource.openai.azure.com/openai/v1/images/generations"), endpoint);
    }

    @Test void prefersOpenAiKeyThenFallsBackToAzureKey() {
        assertEquals("openai-key", OpenAiImageProvider.apiKeyFrom(Map.of(
            "OPENAI_API_KEY", "openai-key", "AZURE_OPENAI_API_KEY", "azure-key")));
        assertEquals("azure-key", OpenAiImageProvider.apiKeyFrom(Map.of("AZURE_OPENAI_API_KEY", "azure-key")));
        assertEquals("", OpenAiImageProvider.apiKeyFrom(Map.of()));
    }

    @Test void modelDefaultsToFluxAndHonoursOverride() {
        assertEquals("FLUX.2-pro", OpenAiImageProvider.modelFrom(Map.of()));
        assertEquals("stability-sd3", OpenAiImageProvider.modelFrom(Map.of("OPENAI_IMAGE_MODEL", "stability-sd3")));
    }

    @Test void usesExplicitImageUrlVerbatimWhenProvided() {
        String flux = "https://res.services.ai.azure.com/providers/blackforestlabs/v1/flux-2-pro?api-version=preview";
        URI endpoint = OpenAiImageProvider.endpointFrom(Map.of(
            "OPENAI_IMAGE_URL", flux, "OPENAI_BASE_URL", "https://api.openai.com/v1"));
        assertEquals(URI.create(flux), endpoint);
    }

    @Test void fluxPayloadUsesWidthHeightWhileOpenAiUsesSize() {
        var flux = OpenAiImageProvider.requestPayload("FLUX.2-pro",
            URI.create("https://res.services.ai.azure.com/providers/blackforestlabs/v1/flux-2-pro"), "a fox");
        assertEquals(1024, flux.get("width"));
        assertEquals(1024, flux.get("height"));
        assertFalse(flux.containsKey("size"));

        var openai = OpenAiImageProvider.requestPayload("gpt-image-1",
            URI.create("https://api.openai.com/v1/images/generations"), "a fox");
        assertEquals("1024x1024", openai.get("size"));
        assertFalse(openai.containsKey("width"));
    }
}
