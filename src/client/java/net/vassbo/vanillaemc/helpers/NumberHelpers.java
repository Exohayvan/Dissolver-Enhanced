package net.vassbo.vanillaemc.helpers;

import java.util.NavigableMap;
import java.util.TreeMap;

import org.spongepowered.asm.mixin.injection.struct.InjectorGroupInfo.Map;

public class NumberHelpers {
    private static final NavigableMap<Long, String> suffixes = new TreeMap<> ();
    static {
        suffixes.put(1_000L, "k");
        suffixes.put(1_000_000L, "M");
        suffixes.put(1_000_000_000L, "G");
        suffixes.put(1_000_000_000_000L, "T");
        suffixes.put(1_000_000_000_000_000L, "P");
        suffixes.put(1_000_000_000_000_000_000L, "E");
    }

	    public static String format(long value) {
	        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
	        if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1);
	        if (value < 0) return "-" + format(-value);
	        if (value < 1_000) return Long.toString(value); //deal with easy case
	
	        Map.Entry<Long, String> e = suffixes.floorEntry(value);
	        Long divideBy = e.getKey();
	        String suffix = e.getValue();
	
	        return String.format("%.1f%s", value / (double) divideBy, suffix);
	    }
	}
