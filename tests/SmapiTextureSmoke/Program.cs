using System.Buffers.Binary;
using System.IO.Compression;
using System.Reflection;
using StardewModdingAPI;

if (args.Length != 1)
{
    Console.Error.WriteLine("Usage: SmapiTextureSmoke <package.zip>");
    return 2;
}

string archive = Path.GetFullPath(args[0]);
if (!File.Exists(archive))
{
    Console.Error.WriteLine($"Package not found: {archive}");
    return 2;
}

string extractionRoot = Path.Combine(Path.GetTempPath(), "dissolver-smapi-smoke-" + Guid.NewGuid().ToString("N"));
try
{
    ZipFile.ExtractToDirectory(archive, extractionRoot);
    string modRoot = Path.Combine(extractionRoot, "DissolverEnhanced");
    var request = new SmokeAssetRequest(modRoot);
    string modAssemblyPath = Path.Combine(modRoot, "DissolverEnhanced.StardewValley.Smapi.dll");
    Assembly modAssembly = Assembly.LoadFrom(modAssemblyPath);
    Type modEntryType = modAssembly.GetType("DissolverEnhanced.StardewValley.Smapi.ModEntry", throwOnError: true)!;
    object mod = Activator.CreateInstance(modEntryType)
        ?? throw new InvalidOperationException("Could not create the packaged SMAPI ModEntry.");
    MethodInfo? loader = modEntryType.GetMethod("LoadBigCraftablesTexture", BindingFlags.Instance | BindingFlags.NonPublic);
    if (loader == null)
    {
        throw new InvalidOperationException("SMAPI texture-loading method was not found.");
    }

    loader.Invoke(mod, new object[] { request });
    if (request.LoadedPath != "assets/big-craftables.png")
    {
        throw new InvalidOperationException($"SMAPI requested unexpected texture path '{request.LoadedPath}'.");
    }

    Console.WriteLine(
        $"SMAPI packaged-build smoke test loaded {request.LoadedPath} " +
        $"({request.Width}x{request.Height}) through ModEntry.LoadBigCraftablesTexture."
    );
    return 0;
}
finally
{
    if (Directory.Exists(extractionRoot))
    {
        Directory.Delete(extractionRoot, recursive: true);
    }
}

internal sealed class SmokeAssetRequest
{
    private readonly string modRoot;

    public SmokeAssetRequest(string modRoot)
    {
        this.modRoot = modRoot;
    }

    public string? LoadedPath { get; private set; }
    public int Width { get; private set; }
    public int Height { get; private set; }

    public void LoadFromModFile<T>(string path, AssetLoadPriority priority)
    {
        if (priority != AssetLoadPriority.Exclusive)
        {
            throw new InvalidOperationException($"Unexpected asset priority: {priority}");
        }
        if (typeof(T).FullName != "Microsoft.Xna.Framework.Graphics.Texture2D")
        {
            throw new InvalidOperationException($"Unexpected asset type: {typeof(T).FullName}");
        }

        string normalizedRoot = Path.GetFullPath(modRoot) + Path.DirectorySeparatorChar;
        string texturePath = Path.GetFullPath(Path.Combine(modRoot, path));
        if (!texturePath.StartsWith(normalizedRoot, StringComparison.Ordinal))
        {
            throw new InvalidOperationException("Texture path escaped the packaged mod directory.");
        }

        (Width, Height) = PngDecoder.LoadRgba32(texturePath);
        LoadedPath = path;
    }
}

internal static class PngDecoder
{
    private static readonly byte[] Signature = { 137, 80, 78, 71, 13, 10, 26, 10 };
    private static readonly byte[] HeaderChunk = System.Text.Encoding.ASCII.GetBytes("IHDR");
    private static readonly byte[] DataChunk = System.Text.Encoding.ASCII.GetBytes("IDAT");
    private static readonly byte[] EndChunk = System.Text.Encoding.ASCII.GetBytes("IEND");

    public static (int Width, int Height) LoadRgba32(string path)
    {
        byte[] png = File.ReadAllBytes(path);
        if (png.Length < Signature.Length || !png.AsSpan(0, Signature.Length).SequenceEqual(Signature))
        {
            throw new InvalidDataException("Texture is not a PNG.");
        }

        int position = Signature.Length;
        byte[]? header = null;
        using var compressed = new MemoryStream();
        bool reachedEnd = false;
        while (position < png.Length)
        {
            if (position + 12 > png.Length)
            {
                throw new InvalidDataException("PNG ended inside a chunk.");
            }

            uint rawLength = BinaryPrimitives.ReadUInt32BigEndian(png.AsSpan(position, 4));
            int length = checked((int)rawLength);
            ReadOnlySpan<byte> type = png.AsSpan(position + 4, 4);
            int dataStart = position + 8;
            int dataEnd = checked(dataStart + length);
            int chunkEnd = checked(dataEnd + 4);
            if (chunkEnd > png.Length)
            {
                throw new InvalidDataException("PNG chunk extends past the file.");
            }

            if (type.SequenceEqual(HeaderChunk))
            {
                if (header != null || length != 13)
                {
                    throw new InvalidDataException("PNG has an invalid IHDR chunk.");
                }
                header = png.AsSpan(dataStart, length).ToArray();
            }
            else if (type.SequenceEqual(DataChunk))
            {
                compressed.Write(png, dataStart, length);
            }
            else if (type.SequenceEqual(EndChunk))
            {
                if (length != 0 || chunkEnd != png.Length)
                {
                    throw new InvalidDataException("PNG has an invalid IEND chunk.");
                }
                reachedEnd = true;
                position = chunkEnd;
                break;
            }

            position = chunkEnd;
        }

        if (header == null || compressed.Length == 0 || !reachedEnd || position != png.Length)
        {
            throw new InvalidDataException("PNG is incomplete.");
        }

        int width = checked((int)BinaryPrimitives.ReadUInt32BigEndian(header.AsSpan(0, 4)));
        int height = checked((int)BinaryPrimitives.ReadUInt32BigEndian(header.AsSpan(4, 4)));
        if (width < 1 || height < 1 || header[8] != 8 || header[9] != 6 || header[10] != 0 || header[11] != 0 || header[12] != 0)
        {
            throw new InvalidDataException("PNG is not a supported non-interlaced RGBA32 texture.");
        }

        compressed.Position = 0;
        using var decoded = new MemoryStream();
        using (var inflater = new ZLibStream(compressed, CompressionMode.Decompress, leaveOpen: true))
        {
            inflater.CopyTo(decoded);
        }

        byte[] scanlines = decoded.ToArray();
        int stride = checked(width * 4);
        if (scanlines.Length != checked(height * (stride + 1)))
        {
            throw new InvalidDataException("PNG scanline data has an unexpected size.");
        }

        byte[] previous = new byte[stride];
        int offset = 0;
        for (int row = 0; row < height; row++)
        {
            byte filter = scanlines[offset++];
            if (filter > 4)
            {
                throw new InvalidDataException($"PNG uses invalid filter {filter}.");
            }

            byte[] current = scanlines.AsSpan(offset, stride).ToArray();
            offset += stride;
            for (int index = 0; index < current.Length; index++)
            {
                byte left = index >= 4 ? current[index - 4] : (byte)0;
                byte above = previous[index];
                byte upperLeft = index >= 4 ? previous[index - 4] : (byte)0;
                int predictor = filter switch
                {
                    0 => 0,
                    1 => left,
                    2 => above,
                    3 => (left + above) / 2,
                    4 => Paeth(left, above, upperLeft),
                    _ => throw new InvalidDataException()
                };
                current[index] = unchecked((byte)(current[index] + predictor));
            }
            previous = current;
        }

        return (width, height);
    }

    private static int Paeth(int left, int above, int upperLeft)
    {
        int estimate = left + above - upperLeft;
        int leftDistance = Math.Abs(estimate - left);
        int aboveDistance = Math.Abs(estimate - above);
        int upperLeftDistance = Math.Abs(estimate - upperLeft);
        if (leftDistance <= aboveDistance && leftDistance <= upperLeftDistance)
        {
            return left;
        }
        return aboveDistance <= upperLeftDistance ? above : upperLeft;
    }
}
