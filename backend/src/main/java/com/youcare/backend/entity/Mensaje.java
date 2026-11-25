package com.youcare.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

//cuerpo del mensaje, Usuario emisor, Usuario receptor, hora_creacion, visto(boolean), 
@Entity 
@Table( name="mensaje")
public class Mensaje {
    //Mirar lenght
    @Column (name = "cuerpo_mensaje", nullable = true)
    private String cuerpo_mensaje; 

    @Column (name = "emisor", nullable)
    private Usuario emisor;
    
}
