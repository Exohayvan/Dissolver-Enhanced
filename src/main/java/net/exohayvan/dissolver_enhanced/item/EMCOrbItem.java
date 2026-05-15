package net.exohayvan.dissolver_enhanced.item;

import java.math.BigInteger;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtElement;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;

public class EMCOrbItem extends Item {
    private static final String EMC_KEY = "dissolver_enhanced.emc";
    private static final int NBT_STRING_TYPE = 8;

    public EMCOrbItem(Settings settings) {
        super(settings);
    }

    public static ItemStack create(int emc) {
        return create(BigInteger.valueOf(emc));
    }

    public static ItemStack create(BigInteger emc) {
        ItemStack stack = new ItemStack(ModItems.EMC_ORB);
        setEMC(stack, emc);
        return stack;
    }

    public static int getEMC(ItemStack stack) {
        return EmcNumber.toIntSaturated(getEmcBig(stack));
    }

    public static BigInteger getEmcBig(ItemStack stack) {
        NbtComponent customData = getComponentOrDefault(stack, DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        if (customData == null) {
            return EmcNumber.ZERO;
        }

        var nbt = customData.copyNbt();
        NbtElement emc = nbt.get(EMC_KEY);
        if (emc != null && emc.getType() == NBT_STRING_TYPE) {
            return EmcNumber.parse(readString(emc));
        }

        return EmcNumber.of(readInt(emc));
    }

    public static void setEMC(ItemStack stack, int emc) {
        setEMC(stack, BigInteger.valueOf(emc));
    }

    public static void setEMC(ItemStack stack, BigInteger emc) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putString(EMC_KEY, EmcNumber.nonNegative(emc).toString()));
    }

    public static boolean isEMCOrb(ItemStack stack) {
        return stack.isOf(ModItems.EMC_ORB);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return getEmcBig(stack).signum() > 0;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getComponentOrDefault(ItemStack stack, ComponentType<T> componentType, T defaultValue) {
        Method method = findComponentDefaultGetter(stack.getClass());
        if (method != null) {
            try {
                return (T) method.invoke(stack, componentType, defaultValue);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new IllegalStateException("Unable to read item component " + componentType, exception);
            }
        }

        throw new IllegalStateException("No supported ItemStack component default getter found");
    }

    private static Method findComponentDefaultGetter(Class<?> owner) {
        Method modernGetter = findComponentDefaultGetter(owner, "method_58695");
        if (modernGetter != null) {
            return modernGetter;
        }

        String[] fallbackNames = {"getOrDefault", "method_57825", "method_57379"};
        for (String name : fallbackNames) {
            Method method = findComponentDefaultGetter(owner, name);
            if (method != null) {
                return method;
            }
        }

        return null;
    }

    private static Method findComponentDefaultGetter(Class<?> owner, String name) {
        for (Method method : owner.getMethods()) {
            if (method.getName().equals(name) && isComponentDefaultGetter(method)) {
                return method;
            }
        }

        return null;
    }

    private static boolean isComponentDefaultGetter(Method method) {
        if (method.getParameterCount() != 2) {
            return false;
        }

        return method.getParameterTypes()[0].isAssignableFrom(ComponentType.class) ||
                method.getParameterTypes()[0].isInterface();
    }

    private static String readString(NbtElement element) {
        for (Method method : element.getClass().getMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }

            try {
                if (method.getReturnType() == String.class && isStringValueMethod(method.getName())) {
                    return (String) method.invoke(element);
                }

                if (method.getReturnType() == Optional.class) {
                    Optional<?> value = (Optional<?>) method.invoke(element);
                    if (value.isPresent() && value.get() instanceof String stringValue) {
                        return stringValue;
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new IllegalStateException("Unable to read EMC orb string value", exception);
            }
        }

        for (Field field : element.getClass().getDeclaredFields()) {
            if (field.getType() != String.class) {
                continue;
            }

            try {
                field.setAccessible(true);
                return (String) field.get(element);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Unable to read EMC orb string field", exception);
            }
        }

        return "0";
    }

    private static boolean isStringValueMethod(String name) {
        return name.equals("asString") || name.equals("comp_3831") || name.equals("value");
    }

    private static int readInt(NbtElement element) {
        if (element == null) {
            return 0;
        }

        for (Method method : element.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || method.getReturnType() != int.class) {
                continue;
            }

            String name = method.getName();
            if (!name.equals("intValue") && !name.equals("method_10701") && !name.equals("comp_3820")) {
                continue;
            }

            try {
                return (Integer) method.invoke(element);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new IllegalStateException("Unable to read EMC orb integer value", exception);
            }
        }

        return 0;
    }
}
