package bri;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/** Atomic installation/replacement; service numbers remain stable across updates. */
public final class ServiceRegistry {
    private record Entry(Class<? extends Service> type, String owner, String description) { }
    private static final List<Entry> services = new ArrayList<>();

    private ServiceRegistry() { }

    public static synchronized void addService(Class<?> type) throws Exception {
        install(type, null, false);
    }

    public static synchronized void install(Class<?> type, String owner, boolean update) throws Exception {
        if (owner != null && !type.getPackageName().equals(owner)) {
            throw new IllegalArgumentException("Le package doit correspondre au login.");
        }
        int index = -1;
        for (int i = 0; i < services.size(); i++) {
            if (services.get(i).type().getName().equals(type.getName())) {
                index = i;
                break;
            }
        }
        if (update && index < 0) throw new IllegalArgumentException("Service inexistant.");
        if (!update && index >= 0) throw new IllegalArgumentException("Service deja installe; utilisez la mise a jour.");
        if (index >= 0 && !java.util.Objects.equals(services.get(index).owner(), owner)) {
            throw new IllegalArgumentException("Seul le createur peut modifier ce service.");
        }
        // Validate before touching the entry: failed updates preserve the old service.
        if (!Service.class.isAssignableFrom(type) || !Modifier.isPublic(type.getModifiers())
                || Modifier.isAbstract(type.getModifiers())) {
            throw new IllegalArgumentException("La classe doit etre publique, concrete et implementer bri.Service.");
        }
        type.getConstructor(Socket.class);
        Method description = type.getDeclaredMethod("toStringue");
        if (!Modifier.isPublic(description.getModifiers()) || !Modifier.isStatic(description.getModifiers())
                || description.getReturnType() != String.class) {
            throw new IllegalArgumentException("toStringue doit etre public static String.");
        }
        String text = (String) description.invoke(null);
        if (text == null || text.isBlank() || text.contains("\n") || text.contains("\r")) {
            throw new IllegalArgumentException("La description doit tenir sur une ligne non vide.");
        }
        Entry entry = new Entry(type.asSubclass(Service.class), owner, text);
        if (update) services.set(index, entry);
        else services.add(entry);
    }

    public static synchronized Class<?> getServiceClass(int number) {
        return number < 1 || number > services.size() ? null : services.get(number - 1).type();
    }

    public static synchronized String toStringue() {
        StringBuilder menu = new StringBuilder("Services disponibles:\n");
        for (int i = 0; i < services.size(); i++) {
            menu.append(i + 1).append(". ").append(services.get(i).description()).append('\n');
        }
        return menu.toString();
    }
}
