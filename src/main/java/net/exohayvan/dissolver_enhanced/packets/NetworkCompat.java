package net.exohayvan.dissolver_enhanced.packets;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.exohayvan.dissolver_enhanced.helpers.ReflectionCompat;

public final class NetworkCompat {
    private static final String PROTOCOL_VERSION = "1";
    private static final int NUMERIC_PROTOCOL_VERSION = 1;

    private NetworkCompat() {
    }

    public static Object createChannel(ResourceLocation name) {
        try {
            Class<?> builderClass = Class.forName("net.minecraftforge.network.ChannelBuilder");
            Object builder = invokeStatic(builderClass, "named", name);
            builder = invoke(builder, "networkProtocolVersion", NUMERIC_PROTOCOL_VERSION);
            return invoke(builder, "simpleChannel");
        } catch (ClassNotFoundException modernApiUnavailable) {
            try {
                Class<?> registryClass = Class.forName("net.minecraftforge.network.NetworkRegistry");
                Supplier<String> version = () -> PROTOCOL_VERSION;
                Predicate<String> accepted = PROTOCOL_VERSION::equals;
                return invokeStatic(registryClass, "newSimpleChannel", name, version, accepted, accepted);
            } catch (ClassNotFoundException impossible) {
                throw new IllegalStateException("Forge networking API is unavailable.", impossible);
            }
        }
    }

    public static <M> void register(
        Object channel,
        int id,
        Class<M> messageType,
        BiConsumer<M, FriendlyByteBuf> encoder,
        Function<FriendlyByteBuf, M> decoder,
        BiConsumer<M, Object> handler,
        String direction
    ) {
        Method legacy = ReflectionCompat.findCompatibleMethod(
            channel.getClass(),
            "registerMessage",
            id,
            messageType,
            encoder,
            decoder,
            handler,
            Optional.empty()
        );
        if (legacy != null) {
            invokeMethod(legacy, channel, id, messageType, encoder, decoder, handler, Optional.empty());
            return;
        }

        try {
            Class<?> directionClass = Class.forName("net.minecraftforge.network.NetworkDirection");
            Object networkDirection;
            if (directionClass.isEnum()) {
                @SuppressWarnings({"rawtypes", "unchecked"})
                Object enumDirection = Enum.valueOf((Class<? extends Enum>) directionClass.asSubclass(Enum.class), direction);
                networkDirection = enumDirection;
            } else {
                networkDirection = directionClass.getField(direction).get(null);
            }
            Object builder = invoke(channel, "messageBuilder", messageType, networkDirection);
            builder = invoke(builder, "encoder", encoder);
            builder = invoke(builder, "decoder", decoder);
            builder = invoke(builder, "consumerNetworkThread", handler);
            invoke(builder, "add");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Forge has no compatible packet registration API.", exception);
        }
    }

    public static void sendToPlayer(Object channel, ServerPlayer player, Object payload) {
        Object target = packetTarget("PLAYER", player, (Supplier<ServerPlayer>) () -> player);
        invokeEitherOrder(channel, "send", payload, target);
    }

    public static void sendToServer(Object channel, Object payload) {
        Method legacy = ReflectionCompat.findCompatibleMethod(channel.getClass(), "sendToServer", payload);
        if (legacy != null) {
            invokeMethod(legacy, channel, payload);
            return;
        }

        Object target = packetTarget("SERVER", null, null);
        invokeEitherOrder(channel, "send", payload, target);
    }

    public static void enqueueWork(Object contextOrSupplier, Runnable task) {
        invoke(context(contextOrSupplier), "enqueueWork", task);
    }

    public static void setPacketHandled(Object contextOrSupplier) {
        invoke(context(contextOrSupplier), "setPacketHandled", true);
    }

    public static ServerPlayer sender(Object contextOrSupplier) {
        Object sender = invoke(context(contextOrSupplier), "getSender");
        return sender instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    private static Object context(Object contextOrSupplier) {
        if (contextOrSupplier instanceof Supplier<?> supplier) {
            return supplier.get();
        }
        return contextOrSupplier;
    }

    private static Object packetTarget(String fieldName, Object directArgument, Object legacyArgument) {
        try {
            Class<?> distributorClass = Class.forName("net.minecraftforge.network.PacketDistributor");
            Field field = distributorClass.getField(fieldName);
            Object distributor = field.get(null);

            if (directArgument != null) {
                Method direct = ReflectionCompat.findCompatibleMethod(distributor.getClass(), "with", directArgument);
                if (direct != null) {
                    return invokeMethod(direct, distributor, directArgument);
                }
            }
            if (legacyArgument != null) {
                Method legacy = ReflectionCompat.findCompatibleMethod(distributor.getClass(), "with", legacyArgument);
                if (legacy != null) {
                    return invokeMethod(legacy, distributor, legacyArgument);
                }
            }

            Method noArg = ReflectionCompat.findCompatibleMethod(distributor.getClass(), "noArg");
            if (noArg != null) {
                return invokeMethod(noArg, distributor);
            }
            throw new IllegalStateException("PacketDistributor." + fieldName + " has no compatible target factory.");
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException("Could not create Forge packet target " + fieldName + '.', exception);
        }
    }

    private static void invokeEitherOrder(Object target, String methodName, Object first, Object second) {
        Method method = ReflectionCompat.findCompatibleMethod(target.getClass(), methodName, first, second);
        if (method != null) {
            invokeMethod(method, target, first, second);
            return;
        }
        method = ReflectionCompat.findCompatibleMethod(target.getClass(), methodName, second, first);
        if (method != null) {
            invokeMethod(method, target, second, first);
            return;
        }
        throw new IllegalStateException(target.getClass().getName() + " has no compatible " + methodName + " method.");
    }

    private static Object invokeStatic(Class<?> type, String methodName, Object... arguments) {
        Method method = ReflectionCompat.findCompatibleMethod(type, methodName, arguments);
        if (method == null || !Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException(type.getName() + " has no compatible static " + methodName + " method.");
        }
        return invokeMethod(method, null, arguments);
    }

    private static Object invoke(Object target, String methodName, Object... arguments) {
        Method method = ReflectionCompat.findCompatibleMethod(target.getClass(), methodName, arguments);
        if (method == null) {
            throw new IllegalStateException(target.getClass().getName() + " has no compatible " + methodName + " method.");
        }
        return invokeMethod(method, target, arguments);
    }

    private static Object invokeMethod(Method method, Object target, Object... arguments) {
        try {
            method.setAccessible(true);
            return method.invoke(target, arguments);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            throw new IllegalStateException("Could not invoke " + method.getDeclaringClass().getName() + '.' + method.getName() + '.', exception);
        }
    }

}
