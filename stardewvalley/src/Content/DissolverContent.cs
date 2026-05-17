namespace DissolverEnhanced.StardewValley.Common.Content;

public static class DissolverContent
{
    public const string ModId = "Exohayvan.DissolverEnhanced";
    public const string BigCraftablesAssetName = "Mods/Exohayvan.DissolverEnhanced/BigCraftables";
    public const string DissolverBigCraftableId = "Exohayvan.DissolverEnhanced_Dissolver";
    public const string DissolverQualifiedItemId = "(BC)" + DissolverBigCraftableId;
    public const string DissolverRecipeName = "Dissolver";
    public const string DissolverDisplayName = "Dissolver";
    public const string DissolverDescription = "Converts items into stored EMC.";

    public static string DissolverRecipeForDifficulty(string difficulty)
    {
        return RecipeIngredientsForDifficulty(difficulty)
            + "//"
            + DissolverBigCraftableId
            + "/true/none/"
            + DissolverDisplayName;
    }

    public static string RecipeIngredientsForDifficulty(string difficulty)
    {
        return difficulty.ToLowerInvariant() switch
        {
            "easy" => "390 25 334 2 80 1",
            "normal" => "390 25 334 5 338 1 72 1",
            "hard" => "390 50 335 5 336 5 787 1 72 1",
            _ => "390 50 335 5 336 5 787 1 72 1"
        };
    }
}
