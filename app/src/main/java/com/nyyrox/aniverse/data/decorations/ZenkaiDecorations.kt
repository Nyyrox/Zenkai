package com.nyyrox.aniverse.data.decorations

import androidx.compose.ui.graphics.Color

enum class DecorationType {
    AVATAR_FRAME,   // Circular 3D Avatar Frames (20 items)
    AVATAR_OVERLAY, // Animated 60fps Overlays (65 items)
    TEXT_EFFECT     // Username Text Effects (16 items)
}

enum class DecorationRarity(
    val label: String,
    val color: Color,
    val badgeHex: Long
) {
    COMMON("Common", Color(0xFF94A3B8), 0xFF94A3B8),
    RARE("Rare", Color(0xFF3B82F6), 0xFF3B82F6),
    EPIC("Epic", Color(0xFF8B5CF6), 0xFF8B5CF6),
    LEGENDARY("Legendary", Color(0xFFF59E0B), 0xFFF59E0B),
    ZENKAI_MAX("Zenkai Mythic", Color(0xFFE11D48), 0xFFE11D48)
}

data class AvatarFrameTheme(
    val outerGlowColor: Color,
    val rimColors: List<Color>,
    val innerBevelColor: Color,
    val specularHighlight: Color,
    val crestEmoji: String,
    val accentColor: Color,
    val borderThicknessDp: Float = 4.5f
)

enum class TextEffectStyle {
    RGB_GLITCH,
    SHIMMER,
    BOUNCE,
    PULSE,
    HEARTBEAT,
    FROSTBITE,
    MOLTEN,
    NEON_PRISM,
    FLICKER,
    FLOW,
    PHANTOM,
    ELECTRIC,
    THUNDERSTORM,
    STARFALL,
    AURORA,
    SUPERNOVA,
    ECLIPSE
}

data class TextEffectTheme(
    val effectStyle: TextEffectStyle,
    val primaryColors: List<Color>,
    val glowColor: Color,
    val glowRadius: Float = 14f,
    val animationDurationMs: Int = 2200,
    val sampleSubtitle: String = ""
)

// Kept for backward compatibility
data class NameplateTheme(
    val bgStart: Color = Color(0xFF141721),
    val bgEnd: Color = Color(0xFF090A0F),
    val borderColor: Color = Color(0xFF3B82F6),
    val glowColor: Color = Color(0xFF3B82F6),
    val crest: String = "✨",
    val textColor: Color = Color.White
)

data class DecorationItem(
    val id: String,
    val name: String,
    val description: String,
    val type: DecorationType,
    val rarity: DecorationRarity,
    val category: String = "Game Tier",
    val isAnimated: Boolean = true,
    val assetFileName: String? = null,
    val frameTheme: AvatarFrameTheme? = null,
    val textEffectTheme: TextEffectTheme? = null,
    val nameplateTheme: NameplateTheme? = null
)

object ZenkaiDecorationCatalog {

    // 3D Circular Frames removed as requested - backward compatibility alias
    val CIRCULAR_FRAMES: List<DecorationItem> = emptyList()

    // ── 1. Avatar Overlays (65 Animated APNG Overlays loaded directly from GitHub repo) ──
    val AVATAR_OVERLAYS = listOf(
        // Cosmic & Space
        DecorationItem("ao_astronaut_helmet", "Astronaut Helmet", "Space explorer helmet visor", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Cosmic", true, "10_Astronaut-Helmet.png"),
        DecorationItem("ao_black_hole", "Black Hole", "Gravitational accretion singularity", DecorationType.AVATAR_OVERLAY, DecorationRarity.ZENKAI_MAX, "Cosmic", true, "10_Black-Hole.png"),
        DecorationItem("ao_constellations", "Constellations", "Glowing celestial star chart", DecorationType.AVATAR_OVERLAY, DecorationRarity.EPIC, "Cosmic", true, "10_Constellations.png"),
        DecorationItem("ao_solar_orbit", "Solar Orbit", "Orbital solar ring with planetary bodies", DecorationType.AVATAR_OVERLAY, DecorationRarity.LEGENDARY, "Cosmic", true, "10_Solar-Orbit.png"),
        DecorationItem("ao_stardust", "Stardust", "Glittering nebula cosmic stardust", DecorationType.AVATAR_OVERLAY, DecorationRarity.EPIC, "Cosmic", true, "10_Stardust.png"),
        DecorationItem("ao_ufo", "UFO Beam", "Alien beam emitter", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Cosmic", true, "10_UFO.png"),

        // Gyoko Neon
        DecorationItem("ao_gyoko_green", "Gyoko Green", "Electric lime cyberpunk visor", DecorationType.AVATAR_OVERLAY, DecorationRarity.EPIC, "Cyber", true, "11_Gyoko-Green.png"),
        DecorationItem("ao_gyoko_pink", "Gyoko Pink", "Hot magenta neon frame", DecorationType.AVATAR_OVERLAY, DecorationRarity.EPIC, "Cyber", true, "11_Gyoko-Pink.png"),

        // Anime Emotions
        DecorationItem("ao_angry", "Angry Mark", "Burning anger cross mark effect", DecorationType.AVATAR_OVERLAY, DecorationRarity.COMMON, "Emotes", true, "1_Angry.png"),
        DecorationItem("ao_cat_ears", "Cat Ears", "Cute animated neko ears", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Emotes", true, "1_cat-ears.png"),
        DecorationItem("ao_dismay", "Dismay", "Shocked blue lines of dismay", DecorationType.AVATAR_OVERLAY, DecorationRarity.COMMON, "Emotes", true, "1_dismay.png"),
        DecorationItem("ao_heartbloom", "Heartbloom", "Radiant pink blooming love aura", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Emotes", true, "1_heartbloom.png"),
        DecorationItem("ao_in_love", "In Love", "Floating love hearts and sparkles", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Emotes", true, "1_In-Love.png"),
        DecorationItem("ao_in_tears", "In Tears", "Streaming waterfalls of anime tears", DecorationType.AVATAR_OVERLAY, DecorationRarity.COMMON, "Emotes", true, "1_in-tears.png"),
        DecorationItem("ao_ki_energy", "Ki Energy", "Super Saiyan golden ki flame aura", DecorationType.AVATAR_OVERLAY, DecorationRarity.LEGENDARY, "Emotes", true, "1_ki-energy.png"),
        DecorationItem("ao_radiating_energy", "Radiating Energy", "Intense pulsing shockwave rings", DecorationType.AVATAR_OVERLAY, DecorationRarity.EPIC, "Emotes", true, "1_Radiating-Energy.png"),
        DecorationItem("ao_rage", "Full Rage", "Blazing demonic rage flames", DecorationType.AVATAR_OVERLAY, DecorationRarity.EPIC, "Emotes", true, "1_rage.png"),
        DecorationItem("ao_shocked", "Shocked Eyes", "Electric yellow shock sparks", DecorationType.AVATAR_OVERLAY, DecorationRarity.COMMON, "Emotes", true, "1_Shocked.png"),
        DecorationItem("ao_soul_leaving", "Soul Leaving Body", "Ghostly soul floating away", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Emotes", true, "1_Soul-Leaving-Body.png"),
        DecorationItem("ao_starry_eyed", "Starry Eyed", "Glittering golden anime eye stars", DecorationType.AVATAR_OVERLAY, DecorationRarity.COMMON, "Emotes", true, "1_Starry-Eyed.png"),
        DecorationItem("ao_sweat_drops", "Sweat Drops", "Nervous blue anime sweat beads", DecorationType.AVATAR_OVERLAY, DecorationRarity.COMMON, "Emotes", true, "1_Sweat-Drops.png"),

        // Cyberpunk
        DecorationItem("ao_cybernetic", "Cybernetic Core", "High-tech robotic chassis plating", DecorationType.AVATAR_OVERLAY, DecorationRarity.EPIC, "Cyber", true, "2_Cybernetic.png"),
        DecorationItem("ao_digital_sunrise", "Digital Sunrise", "Synthwave 80s neon grid skyline", DecorationType.AVATAR_OVERLAY, DecorationRarity.LEGENDARY, "Cyber", true, "2_Digital Sunrise.png"),
        DecorationItem("ao_glitch", "Cyber Glitch", "RGB chromatic aberration matrix", DecorationType.AVATAR_OVERLAY, DecorationRarity.EPIC, "Cyber", true, "2_Glitch.png"),
        DecorationItem("ao_implant", "Neural Implant", "Futuristic brain interface diodes", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Cyber", true, "2_Implant.png"),
        DecorationItem("ao_disxcore", "DISXCORE Headset", "Cyber sound producer headphones", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Cyber", true, "3_DISXCORE Headset.png"),
        DecorationItem("ao_futuristic_ui", "Futuristic HUD", "Tactical holographic target ring", DecorationType.AVATAR_OVERLAY, DecorationRarity.EPIC, "Cyber", true, "3_Futuristic UI.png"),
        DecorationItem("ao_smoke", "Cyber Smoke", "Violet holographic vapor stream", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Cyber", true, "3_Smoke.png"),

        // Fantasy RPG
        DecorationItem("ao_defensive_shield", "Aegis Shield", "Luminous blue knight barrier", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "RPG", true, "4_Defensive Shield.png"),
        DecorationItem("ao_fairy_sprites", "Fairy Sprites", "Dancing enchanted forest wisps", DecorationType.AVATAR_OVERLAY, DecorationRarity.EPIC, "RPG", true, "4_Fairy Sprites.png"),
        DecorationItem("ao_flaming_sword", "Flaming Sword", "Blazing runic knight broadsword", DecorationType.AVATAR_OVERLAY, DecorationRarity.LEGENDARY, "RPG", true, "4_Flaming Sword.png"),
        DecorationItem("ao_glowing_runes", "Glowing Runes", "Ancient mystic arcane runes", DecorationType.AVATAR_OVERLAY, DecorationRarity.EPIC, "RPG", true, "4_GLowing Runes.png"),
        DecorationItem("ao_magical_potion", "Magical Potion", "Bubbling mana elixir flask", DecorationType.AVATAR_OVERLAY, DecorationRarity.COMMON, "RPG", true, "4_Magical Potion.png"),
        DecorationItem("ao_skull_medallion", "Skull Medallion", "Cursed necromancer skull sigil", DecorationType.AVATAR_OVERLAY, DecorationRarity.EPIC, "RPG", true, "4_Skull Medallion.png"),
        DecorationItem("ao_treasure_key", "Treasure & Key", "Royal chest with skeleton key", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "RPG", true, "4_Treasure and Key.png"),
        DecorationItem("ao_wizards_staff", "Wizard's Staff", "Archmage crystal casting staff", DecorationType.AVATAR_OVERLAY, DecorationRarity.LEGENDARY, "RPG", true, "4_Wizard's Staff.png"),

        // Eastern / Lunar
        DecorationItem("ao_dragons_smile", "Dragon's Smile", "Imperial azure serpentine dragon", DecorationType.AVATAR_OVERLAY, DecorationRarity.ZENKAI_MAX, "Eastern", true, "5_Dragon's Smile.png"),
        DecorationItem("ao_fan_flourish", "Fan Flourish", "Traditional gold embroidered silk fan", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Eastern", true, "5_Fan Fluorish.png"),
        DecorationItem("ao_firecrackers", "Firecrackers", "Festive exploding festival sparks", DecorationType.AVATAR_OVERLAY, DecorationRarity.COMMON, "Eastern", true, "5_Firecrackers.png"),
        DecorationItem("ao_koi_pond", "Koi Pond", "Golden twin koi swimming in harmony", DecorationType.AVATAR_OVERLAY, DecorationRarity.EPIC, "Eastern", true, "5_Koi Pond.png"),
        DecorationItem("ao_lucky_envelopes", "Lucky Envelopes", "Red gold celebration envelopes", DecorationType.AVATAR_OVERLAY, DecorationRarity.COMMON, "Eastern", true, "5_Lucky Envelopes.png"),
        DecorationItem("ao_lunar_lanterns", "Lunar Lanterns", "Glowing red floating paper lanterns", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Eastern", true, "5_Lunar Lanterns.png"),

        // Elements
        DecorationItem("ao_air", "Air Element", "Whirling emerald breeze current", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Elements", true, "6_Air.png"),
        DecorationItem("ao_balance", "Yin Yang Balance", "Harmonious light and dark equilibrium", DecorationType.AVATAR_OVERLAY, DecorationRarity.ZENKAI_MAX, "Elements", true, "6_Balance.png"),
        DecorationItem("ao_earth", "Earth Element", "Shattered runic stone fragments", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Elements", true, "6_Earth.png"),
        DecorationItem("ao_fire", "Fire Element", "Roaring crimson blaze flames", DecorationType.AVATAR_OVERLAY, DecorationRarity.EPIC, "Elements", true, "6_Fire.png"),
        DecorationItem("ao_lightning", "Lightning Element", "Crackling high-voltage plasma bolts", DecorationType.AVATAR_OVERLAY, DecorationRarity.LEGENDARY, "Elements", true, "6_Lightning.png"),
        DecorationItem("ao_water", "Water Element", "Swirling deep ocean whirlpool", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Elements", true, "6_Water.png"),

        // Cozy & Lifestyle
        DecorationItem("ao_chromawave", "Chromawave", "Rainbow gradient audio frequency wave", DecorationType.AVATAR_OVERLAY, DecorationRarity.EPIC, "Cozy", true, "7_Chromawave.png"),
        DecorationItem("ao_cozy_cat", "Cozy Cat", "Sleeping kitten with snuggle hearts", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Cozy", true, "7_Cozy-Cat.png"),
        DecorationItem("ao_cozy_headphones", "Cozy Headphones", "Pastel headphones with soundwaves", DecorationType.AVATAR_OVERLAY, DecorationRarity.COMMON, "Cozy", true, "7_Cozy-Headphones.png"),
        DecorationItem("ao_doodling", "Doodling Stars", "Whimsical sketched notebook doodles", DecorationType.AVATAR_OVERLAY, DecorationRarity.COMMON, "Cozy", true, "7_Doodling.png"),
        DecorationItem("ao_oasis", "Desert Oasis", "Lush tropical palm tree mirage", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Cozy", true, "7_Oasis.png"),
        DecorationItem("ao_rainy_mood", "Rainy Mood", "Soothing rainfall with rainy clouds", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Cozy", true, "7_Rainy-Mood.png"),

        // Tactical & Agents
        DecorationItem("ao_clove", "A Hint of Clove", "Smoky ethereal butterfly mist", DecorationType.AVATAR_OVERLAY, DecorationRarity.EPIC, "Tactical", true, "8_a-hint-of-clove.png"),
        DecorationItem("ao_blade_storm", "Blade Storm", "Orbiting throwing daggers vortex", DecorationType.AVATAR_OVERLAY, DecorationRarity.LEGENDARY, "Tactical", true, "8_blade-storm.png"),
        DecorationItem("ao_frag_out", "Frag Out", "Tactical grenade explosion flash", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Tactical", true, "8_frag-out.png"),
        DecorationItem("ao_omens_cowl", "Omen's Cowl", "Dark shadow shroud wraith hood", DecorationType.AVATAR_OVERLAY, DecorationRarity.LEGENDARY, "Tactical", true, "8_omens-cowl.png"),
        DecorationItem("ao_reynas_leer", "Reyna's Leer", "All-seeing purple void eye", DecorationType.AVATAR_OVERLAY, DecorationRarity.ZENKAI_MAX, "Tactical", true, "8_reynas-leer.png"),

        // Nature & Floral
        DecorationItem("ao_butterflies", "Butterflies", "Fluttering golden monarch butterflies", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Floral", true, "9_butterflies.png"),
        DecorationItem("ao_dandelion_duo", "Dandelion Duo", "Blowing dandelion seeds in the wind", DecorationType.AVATAR_OVERLAY, DecorationRarity.COMMON, "Floral", true, "9_dandelion-duo.png"),
        DecorationItem("ao_honeyblossom", "Honeyblossom", "Sweet blooming meadow honey flowers", DecorationType.AVATAR_OVERLAY, DecorationRarity.COMMON, "Floral", true, "9_honeyblossom.png"),
        DecorationItem("ao_hug_rainbow", "Hug the Rainbow", "Radiant pastel sky rainbow arch", DecorationType.AVATAR_OVERLAY, DecorationRarity.RARE, "Floral", true, "9_hug-the-rainbow.png"),
        DecorationItem("ao_strawberry_vine", "Strawberry Vine", "Sweet strawberry garden vines", DecorationType.AVATAR_OVERLAY, DecorationRarity.COMMON, "Floral", true, "9_strawberry-vine.png"),
        DecorationItem("ao_the_petal_pack", "Petal Pack", "Whirling sakura blossom petal storm", DecorationType.AVATAR_OVERLAY, DecorationRarity.EPIC, "Floral", true, "9_the-petal-pack.png")
    )

    // ── 3. Username Text Effects (16 Masterpiece 60fps Kinetic & Shader Text Effects) ──
    val TEXT_EFFECTS = listOf(
        DecorationItem(
            id = "te_rgb_glitch",
            name = "RGB Glitch (Smooth Flow)",
            description = "Chromatic aberration with dynamic red & cyan micro-jitter with smooth digital wave flow",
            type = DecorationType.TEXT_EFFECT,
            rarity = DecorationRarity.ZENKAI_MAX,
            category = "Cyber",
            isAnimated = true,
            textEffectTheme = TextEffectTheme(
                effectStyle = TextEffectStyle.RGB_GLITCH,
                primaryColors = listOf(Color(0xFFFF0055), Color(0xFF00F0FF), Color.White),
                glowColor = Color(0xFFFF0055),
                glowRadius = 16f,
                animationDurationMs = 1200,
                sampleSubtitle = "Chromatic Signal Distortion"
            )
        ),
        DecorationItem(
            id = "te_shimmer",
            name = "Diamond Shimmer",
            description = "High-contrast diamond luster sweeping continuously across letters with sparkling sheen",
            type = DecorationType.TEXT_EFFECT,
            rarity = DecorationRarity.EPIC,
            category = "Celestial",
            isAnimated = true,
            textEffectTheme = TextEffectTheme(
                effectStyle = TextEffectStyle.SHIMMER,
                primaryColors = listOf(Color(0xFFE2E8F0), Color(0xFFFFFFFF), Color(0xFF38BDF8), Color(0xFFE2E8F0)),
                glowColor = Color(0xFF38BDF8),
                glowRadius = 14f,
                animationDurationMs = 2400,
                sampleSubtitle = "Polished Diamond Gleam"
            )
        ),
        DecorationItem(
            id = "te_bounce",
            name = "Kinetic Bounce",
            description = "Playful rhythmic spring-wave vertical bounce with responsive floating amplitude",
            type = DecorationType.TEXT_EFFECT,
            rarity = DecorationRarity.RARE,
            category = "Kinetic",
            isAnimated = true,
            textEffectTheme = TextEffectTheme(
                effectStyle = TextEffectStyle.BOUNCE,
                primaryColors = listOf(Color(0xFFF43F5E), Color(0xFFFB7185)),
                glowColor = Color(0xFFF43F5E),
                glowRadius = 12f,
                animationDurationMs = 700,
                sampleSubtitle = "Spring Amplitude Wave"
            )
        ),
        DecorationItem(
            id = "te_pulse",
            name = "Astral Pulse",
            description = "Deep rhythmic breathing scale expansion with soft glowing ethereal purple aura",
            type = DecorationType.TEXT_EFFECT,
            rarity = DecorationRarity.RARE,
            category = "Astral",
            isAnimated = true,
            textEffectTheme = TextEffectTheme(
                effectStyle = TextEffectStyle.PULSE,
                primaryColors = listOf(Color(0xFFA855F7), Color(0xFFC084FC)),
                glowColor = Color(0xFFA855F7),
                glowRadius = 15f,
                animationDurationMs = 2200,
                sampleSubtitle = "Harmonic Breathing Scale"
            )
        ),
        DecorationItem(
            id = "te_heartbeat",
            name = "Zenkai Heartbeat",
            description = "Cardiovascular double-thump scale spike surging with crimson bloodline energy",
            type = DecorationType.TEXT_EFFECT,
            rarity = DecorationRarity.LEGENDARY,
            category = "Imperial",
            isAnimated = true,
            textEffectTheme = TextEffectTheme(
                effectStyle = TextEffectStyle.HEARTBEAT,
                primaryColors = listOf(Color(0xFFE11D48), Color(0xFFFF4D6D)),
                glowColor = Color(0xFFE11D48),
                glowRadius = 18f,
                animationDurationMs = 1500,
                sampleSubtitle = "Bloodline Pulse Spike"
            )
        ),
        DecorationItem(
            id = "te_frostbite",
            name = "Glacial Frostbite",
            description = "Sub-zero crystalline arctic frost with shimmering diamond dust and icy blue aura",
            type = DecorationType.TEXT_EFFECT,
            rarity = DecorationRarity.EPIC,
            category = "Elemental",
            isAnimated = true,
            textEffectTheme = TextEffectTheme(
                effectStyle = TextEffectStyle.FROSTBITE,
                primaryColors = listOf(Color(0xFFBAE6FD), Color(0xFFFFFFFF), Color(0xFF38BDF8)),
                glowColor = Color(0xFF0284C7),
                glowRadius = 15f,
                animationDurationMs = 2600,
                sampleSubtitle = "Absolute Zero Crystal"
            )
        ),
        DecorationItem(
            id = "te_molten",
            name = "Molten Magma",
            description = "Searing volcanic lava sweep with molten heatwave embers and incandescent yellow core",
            type = DecorationType.TEXT_EFFECT,
            rarity = DecorationRarity.LEGENDARY,
            category = "Elemental",
            isAnimated = true,
            textEffectTheme = TextEffectTheme(
                effectStyle = TextEffectStyle.MOLTEN,
                primaryColors = listOf(Color(0xFFFF4500), Color(0xFFFFB703), Color(0xFFDC2626), Color(0xFFFFEE00)),
                glowColor = Color(0xFFFF4500),
                glowRadius = 18f,
                animationDurationMs = 2000,
                sampleSubtitle = "Erupting Volcanic Heat"
            )
        ),
        DecorationItem(
            id = "te_neon_prism",
            name = "Neon Prism",
            description = "Full-spectrum rainbow prism cycling continuously through blinding hyper-neon hues",
            type = DecorationType.TEXT_EFFECT,
            rarity = DecorationRarity.ZENKAI_MAX,
            category = "Cyber",
            isAnimated = true,
            textEffectTheme = TextEffectTheme(
                effectStyle = TextEffectStyle.NEON_PRISM,
                primaryColors = listOf(Color(0xFFFF007F), Color(0xFF00F0FF), Color(0xFFFFE600), Color(0xFF7000FF)),
                glowColor = Color(0xFF00F0FF),
                glowRadius = 20f,
                animationDurationMs = 2500,
                sampleSubtitle = "Hyper-Spectrum Bloom"
            )
        ),
        DecorationItem(
            id = "te_flicker",
            name = "Cyber Strobe",
            description = "Faulty retro-futuristic cyberpunk neon sign with organic voltage stutters and flickers",
            type = DecorationType.TEXT_EFFECT,
            rarity = DecorationRarity.RARE,
            category = "Cyber",
            isAnimated = true,
            textEffectTheme = TextEffectTheme(
                effectStyle = TextEffectStyle.FLICKER,
                primaryColors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8)),
                glowColor = Color(0xFF38BDF8),
                glowRadius = 14f,
                animationDurationMs = 1400,
                sampleSubtitle = "Voltage Arc Instability"
            )
        ),
        DecorationItem(
            id = "te_cyber_flow",
            name = "Liquid Cyber Flow",
            description = "Silky fluid stream gliding through neon violet, hot magenta, and electric turquoise",
            type = DecorationType.TEXT_EFFECT,
            rarity = DecorationRarity.EPIC,
            category = "Cyber",
            isAnimated = true,
            textEffectTheme = TextEffectTheme(
                effectStyle = TextEffectStyle.FLOW,
                primaryColors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4), Color(0xFF8B5CF6)),
                glowColor = Color(0xFF8B5CF6),
                glowRadius = 16f,
                animationDurationMs = 3000,
                sampleSubtitle = "Liquid Gradient Current"
            )
        ),
        DecorationItem(
            id = "te_phantom",
            name = "Ethereal Phantom",
            description = "Ghostly spirit mist with smoky shadow drift and translucent lavender vapor glow",
            type = DecorationType.TEXT_EFFECT,
            rarity = DecorationRarity.EPIC,
            category = "Gothic",
            isAnimated = true,
            textEffectTheme = TextEffectTheme(
                effectStyle = TextEffectStyle.PHANTOM,
                primaryColors = listOf(Color(0xFFDDD6FE), Color(0xFFA78BFA), Color(0xFF7C3AED)),
                glowColor = Color(0xFFA78BFA),
                glowRadius = 16f,
                animationDurationMs = 2800,
                sampleSubtitle = "Specter Mist Drift"
            )
        ),
        DecorationItem(
            id = "te_electric",
            name = "Plasma Electric",
            description = "High-voltage erratic micro-jitter with electric blue plasma arcs and sudden lightning discharges",
            type = DecorationType.TEXT_EFFECT,
            rarity = DecorationRarity.LEGENDARY,
            category = "Cyber",
            isAnimated = true,
            textEffectTheme = TextEffectTheme(
                effectStyle = TextEffectStyle.ELECTRIC,
                primaryColors = listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFF67E8F9)),
                glowColor = Color(0xFF38BDF8),
                glowRadius = 18f,
                animationDurationMs = 1400,
                sampleSubtitle = "Arc Discharge Surge"
            )
        ),
        DecorationItem(
            id = "te_thunderstorm",
            name = "Electric Raijin",
            description = "High-voltage lightning arcs crackling across letters with sudden flash discharges",
            type = DecorationType.TEXT_EFFECT,
            rarity = DecorationRarity.LEGENDARY,
            category = "Elemental",
            isAnimated = true,
            textEffectTheme = TextEffectTheme(
                effectStyle = TextEffectStyle.THUNDERSTORM,
                primaryColors = listOf(Color(0xFFFDE047), Color(0xFF60A5FA), Color(0xFFFFFFFF)),
                glowColor = Color(0xFFFBBF24),
                glowRadius = 18f,
                animationDurationMs = 1600,
                sampleSubtitle = "Arc Lightning Discharge"
            )
        ),
        DecorationItem(
            id = "te_starfall",
            name = "Cosmic Starfall",
            description = "Deep stellar galaxy nebula with glistening stars drifting across text geometry",
            type = DecorationType.TEXT_EFFECT,
            rarity = DecorationRarity.LEGENDARY,
            category = "Astral",
            isAnimated = true,
            textEffectTheme = TextEffectTheme(
                effectStyle = TextEffectStyle.STARFALL,
                primaryColors = listOf(Color(0xFF818CF8), Color(0xFFC084FC), Color(0xFFF472B6)),
                glowColor = Color(0xFF818CF8),
                glowRadius = 16f,
                animationDurationMs = 3200,
                sampleSubtitle = "Astral Constellation Sweep"
            )
        ),
        DecorationItem(
            id = "te_aurora",
            name = "Northern Aurora",
            description = "Undulating polar aurora borealis ribbons of emerald green, celestial cyan, and violet",
            type = DecorationType.TEXT_EFFECT,
            rarity = DecorationRarity.ZENKAI_MAX,
            category = "Astral",
            isAnimated = true,
            textEffectTheme = TextEffectTheme(
                effectStyle = TextEffectStyle.AURORA,
                primaryColors = listOf(Color(0xFF34D399), Color(0xFF22D3EE), Color(0xFFA855F7), Color(0xFF34D399)),
                glowColor = Color(0xFF34D399),
                glowRadius = 18f,
                animationDurationMs = 3500,
                sampleSubtitle = "Solar Wind Curtains"
            )
        ),
        DecorationItem(
            id = "te_supernova",
            name = "Stellar Supernova",
            description = "Cataclysmic cosmic starburst core radiating blinding golden-white nuclear brilliance",
            type = DecorationType.TEXT_EFFECT,
            rarity = DecorationRarity.ZENKAI_MAX,
            category = "Celestial",
            isAnimated = true,
            textEffectTheme = TextEffectTheme(
                effectStyle = TextEffectStyle.SUPERNOVA,
                primaryColors = listOf(Color(0xFFFFFFFF), Color(0xFFFDE047), Color(0xFFF97316), Color(0xFFEF4444)),
                glowColor = Color(0xFFFBBF24),
                glowRadius = 22f,
                animationDurationMs = 2000,
                sampleSubtitle = "Cosmic Core Detonation"
            )
        ),
        DecorationItem(
            id = "te_eclipse",
            name = "Corona Eclipse",
            description = "Void obsidian core framed by a searing radiant solar corona amber rim fire",
            type = DecorationType.TEXT_EFFECT,
            rarity = DecorationRarity.LEGENDARY,
            category = "Celestial",
            isAnimated = true,
            textEffectTheme = TextEffectTheme(
                effectStyle = TextEffectStyle.ECLIPSE,
                primaryColors = listOf(Color(0xFF0F172A), Color(0xFFF59E0B), Color(0xFFD97706)),
                glowColor = Color(0xFFF59E0B),
                glowRadius = 16f,
                animationDurationMs = 2800,
                sampleSubtitle = "Solar Corona Halo"
            )
        )
    )

    // Backward compatibility alias for any legacy call
    val NAMEPLATES: List<DecorationItem> get() = TEXT_EFFECTS

    // Total: 65 Overlays + 16 Text Effects = 81 items
    val ALL_ITEMS = AVATAR_OVERLAYS + TEXT_EFFECTS

    fun findById(id: String): DecorationItem? {
        return ALL_ITEMS.find { it.id == id }
    }
}
