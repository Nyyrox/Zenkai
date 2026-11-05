package com.nyyrox.aniverse.data.discord

object DiscordConfig {
    const val APP_ID: String = "1553084962943672390"
    const val PUBLIC_KEY: String = "d663270df0da27394b09947550c200250daa65c7334762e79462e558ca39194b"

    @Volatile
    var CLIENT_ID: String = "1553084962943672390"

    @Volatile
    var CLIENT_SECRET: String = "NG0kTg9VtNGR1eshydrVRdynBvxO_hv0"

    @Volatile
    var BOT_TOKEN: String = "MTU1MzA4NDk2Mjk0MzY3MjM5MA.GP9OAQ.cmEDQjvJiZtLY-Kxw7mbF7V7rdmozT7tuTpMzw"

    const val DISCORD_COMMUNITY_URL: String = "https://discord.gg/kHxHzZtH7"
    const val INVITE_CODE: String = "kHxHzZtH7"
    const val GUILD_ID: String = "1504914249921724456"
    const val MAIN_CHANNEL_ID: String = "1504915687338934375"

    @Volatile
    var REDIRECT_URI: String = "https://wcvazatblggnbnrguopj.supabase.co/auth/v1/callback"

    const val OAUTH_AUTHORIZE_URL: String = "https://discord.com/oauth2/authorize"
    const val OAUTH_TOKEN_URL: String = "https://discord.com/api/v10/oauth2/token"
    const val USER_ME_URL: String = "https://discord.com/api/v10/users/@me"
    const val GATEWAY_URL: String = "wss://gateway.discord.gg/?v=10&encoding=json"

    fun getAuthorizeUrl(clientId: String = CLIENT_ID, redirectUri: String = REDIRECT_URI): String {
        // Use Supabase as OAuth middleman (configured in Supabase Dashboard with Discord Provider)
        val encodedRedirect = java.net.URLEncoder.encode("aniverse://login-callback", "UTF-8")
        return "https://wcvazatblggnbnrguopj.supabase.co/auth/v1/authorize?provider=discord&redirect_to=$encodedRedirect&scopes=identify%20email"
    }

    fun getDirectDiscordAuthorizeUrl(clientId: String = CLIENT_ID, redirectUri: String = REDIRECT_URI): String {
        val encodedRedirect = java.net.URLEncoder.encode(redirectUri, "UTF-8")
        return "$OAUTH_AUTHORIZE_URL?client_id=$clientId&response_type=code&redirect_uri=$encodedRedirect&scope=identify%20email"
    }

    fun launchDiscordInvite(context: android.content.Context, inviteCode: String = INVITE_CODE) {
        try {
            val appIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("discord://discord.com/invite/$inviteCode")
            ).apply {
                setPackage("com.discord")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(appIntent)
        } catch (_: Exception) {
            try {
                val webIntent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://discord.gg/$inviteCode")
                ).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (_: Exception) {}
        }
    }

    fun launchDiscordChannel(context: android.content.Context, guildId: String = GUILD_ID, channelId: String = MAIN_CHANNEL_ID) {
        try {
            val appIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("discord://discord.com/channels/$guildId/$channelId")
            ).apply {
                setPackage("com.discord")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(appIntent)
        } catch (_: Exception) {
            launchDiscordInvite(context)
        }
    }
}

