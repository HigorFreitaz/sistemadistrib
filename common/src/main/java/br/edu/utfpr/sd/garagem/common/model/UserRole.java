package br.edu.utfpr.sd.garagem.common.model;

import com.google.gson.annotations.SerializedName;

public enum UserRole {
    @SerializedName(value = "admin", alternate = {"ADMIN"}) ADMIN,
    @SerializedName(value = "user", alternate = {"user", "USER", "CLIENTE"}) USER
}
