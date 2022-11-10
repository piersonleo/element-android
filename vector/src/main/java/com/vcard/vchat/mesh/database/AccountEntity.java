package com.vcard.vchat.mesh.database;

import java.math.BigInteger;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

public class AccountEntity extends RealmObject {

    @PrimaryKey
    public String address = "";
    public String name = "";
    public String encryptedKey = "";
    public String encryptedJson = "";
    public String privateKey = "";

    public String currency = "";
    public String balance = "0";

    public String type = "";
}
