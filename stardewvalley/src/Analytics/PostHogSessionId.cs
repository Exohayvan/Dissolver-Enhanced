namespace DissolverEnhanced.StardewValley.Common.Analytics;

public static class PostHogSessionId
{
    public static string Create()
    {
        Span<byte> bytes = stackalloc byte[16];
        Random.Shared.NextBytes(bytes);

        long timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() & 0x0000FFFFFFFFFFFFL;
        bytes[0] = (byte)((timestamp >> 40) & 0xFF);
        bytes[1] = (byte)((timestamp >> 32) & 0xFF);
        bytes[2] = (byte)((timestamp >> 24) & 0xFF);
        bytes[3] = (byte)((timestamp >> 16) & 0xFF);
        bytes[4] = (byte)((timestamp >> 8) & 0xFF);
        bytes[5] = (byte)(timestamp & 0xFF);
        bytes[6] = (byte)((bytes[6] & 0x0F) | 0x70);
        bytes[8] = (byte)((bytes[8] & 0x3F) | 0x80);

        return string.Create(36, bytes.ToArray(), static (chars, value) =>
        {
            int byteIndex = 0;
            int charIndex = 0;
            for (; byteIndex < value.Length; byteIndex++)
            {
                if (charIndex is 8 or 13 or 18 or 23)
                {
                    chars[charIndex++] = '-';
                }

                byte current = value[byteIndex];
                chars[charIndex++] = ToHex(current >> 4);
                chars[charIndex++] = ToHex(current & 0x0F);
            }
        });
    }

    private static char ToHex(int value)
    {
        return (char)(value < 10 ? '0' + value : 'a' + value - 10);
    }
}
