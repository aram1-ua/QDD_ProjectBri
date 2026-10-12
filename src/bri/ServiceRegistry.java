package bri;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Vector;

public class ServiceRegistry{
    private static List<Class<?>> servicesClasses = new Vector<>();

    public static void addService(Class<?> c) throws Exception{
        //on doit vérifier si la classe c implémente Service
        //ça c'est la 1ere  verif -> isAssignableFrom
        if (!Service.class.isAssignableFrom(c)){
            throw new IllegalArgumentException("La classe doit implémenter l'interface bri.Service");
        }

        //2eme verif -> classe Modifier
        //pour verifier si la classe est public et non abstrct
        int modifiers = c.getModifiers();
        if (!Modifier.isPublic(modifiers) || Modifier.isAbstract(modifiers)){
            throw new IllegalArgumentException("Le constructeur n'est pas public");
        }

        //3eme verif -> java.net.Socket
        //on verifie le constructeur public
        Constructor<?> constr = c.getDeclaredConstructor(java.net.Socket.class);
        if(!Modifier.isPublic(constr.getModifiers())){
            throw new NoSuchMethodException("Le constructeur n'est pas public");
        }

        //4eme verif -> methode toStringue()
        //verifier la methode public static String
        Method method = c.getDeclaredMethod("toStringue");
        int mMods = method.getModifiers();
        if(!Modifier.isPublic(mMods) || !Modifier.isStatic(mMods) || !method.getReturnType().equals(String.class)){
            throw new NoSuchMethodException("La méthode toStringue doit être public static String");
        }

        //la on ajoute au registre si toutes les verif sont passés
        servicesClasses.add(c);
    }
}