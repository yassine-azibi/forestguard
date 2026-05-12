package edu.capteur.interfaces;

import java.util.List;

public interface IService<T> {

    // ✅ Les 4 opérations CRUD
    void ajouter(T t);
    void modifier(T t);
    void supprimer(int id);
    List<T> afficher();
}