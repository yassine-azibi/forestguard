package com.forestguard.interfaces;

import java.util.List;

public interface IService<T> {
    void addEntity(T entity);
    void deleteEntity(T entity);
    void updateEntity(int id, T entity);
    List<T> getData();
}