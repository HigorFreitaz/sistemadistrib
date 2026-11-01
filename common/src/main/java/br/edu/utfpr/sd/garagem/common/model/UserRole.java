package br.edu.utfpr.sd.garagem.common.model;

import com.google.gson.annotations.SerializedName;

public enum UserRole {
    @SerializedName("admin") ADMIN,
    @SerializedName("user") USER
}
