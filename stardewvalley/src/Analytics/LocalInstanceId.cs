using System.Security.Cryptography;

namespace DissolverEnhanced.StardewValley.Common.Analytics;

public static class LocalInstanceId
{
    public static string ReadOrCreate(string file)
    {
        string? directory = Path.GetDirectoryName(file);
        if (!string.IsNullOrEmpty(directory))
        {
            Directory.CreateDirectory(directory);
        }

        if (File.Exists(file))
        {
            string existing = File.ReadAllText(file).Trim();
            if (!string.IsNullOrWhiteSpace(existing))
            {
                return existing;
            }
        }

        string created = Convert.ToHexString(RandomNumberGenerator.GetBytes(16)).ToLowerInvariant();
        File.WriteAllText(file, created);
        return created;
    }
}
