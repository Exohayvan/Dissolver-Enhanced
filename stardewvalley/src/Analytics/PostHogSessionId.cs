namespace DissolverEnhanced.StardewValley.Common.Analytics;

public static class PostHogSessionId
{
    public static string Create()
    {
        return Guid.NewGuid().ToString("N");
    }
}
